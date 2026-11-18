package com.renqing.zhang.ui.me

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.renqing.zhang.ui.components.InlineField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.renqing.zhang.data.BackupPreview
import com.renqing.zhang.data.Settings
import com.renqing.zhang.domain.formatYuan
import com.renqing.zhang.domain.maskPhone
import com.renqing.zhang.ui.LedgerBody
import com.renqing.zhang.ui.LocalNav
import com.renqing.zhang.ui.LocalRepo
import com.renqing.zhang.ui.LocalSession
import com.renqing.zhang.ui.components.Avatar
import com.renqing.zhang.ui.components.ConfirmDialog
import com.renqing.zhang.ui.components.PageHeader
import com.renqing.zhang.ui.components.PaperDialog
import com.renqing.zhang.ui.components.PrimaryButton
import com.renqing.zhang.ui.theme.CardWhite
import com.renqing.zhang.ui.theme.Cinnabar
import com.renqing.zhang.ui.theme.Honey
import com.renqing.zhang.ui.theme.Ink
import com.renqing.zhang.ui.theme.InkSoft
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun MeScreen() {
    val nav = LocalNav.current
    val repo = LocalRepo.current
    val session = LocalSession.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as FragmentActivity
    var editingProfile by remember { mutableStateOf(false) }
    var backupMenu by remember { mutableStateOf(false) }
    var editingThreshold by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf("") }
    var phoneDraft by remember { mutableStateOf("") }
    var thresholdDraft by remember { mutableStateOf("500") }
    var pendingExport by remember { mutableStateOf<ByteArray?>(null) }
    var pendingBackup by remember { mutableStateOf<String?>(null) }
    var restoreText by remember { mutableStateOf<String?>(null) }
    var restorePreview by remember { mutableStateOf<BackupPreview?>(null) }
    val excelLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        val bytes = pendingExport ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } }
            .onSuccess { session.toast("已导出") }
            .onFailure { session.toast("导出失败，请重试") }
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val text = pendingBackup ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } }
            .onSuccess { session.toast("备份已保存") }
            .onFailure { session.toast("备份失败，请重试") }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("读不到文件")
            restorePreview = repo.previewBackup(text)
            restoreText = text
        }.onFailure { session.toast("备份文件无法读取") }
    }
    Column(Modifier.fillMaxSize().background(Honey).statusBarsPadding()) {
        PageHeader("我的")
        LedgerBody(Modifier.weight(1f)) { snapshot, settings ->
            Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                Row(
                    Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .clickable {
                            nameDraft = settings.userName
                            phoneDraft = settings.userPhone
                            editingProfile = true
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(settings.userName.take(1).ifBlank { "账" }, settings.userName.ifBlank { "账" }, 52)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(settings.userName.ifBlank { "未命名" }, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Ink)
                        Text("账本只在本机 · ${maskPhone(settings.userPhone)}", color = InkSoft, fontSize = 13.sp)
                    }
                }
                Column(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardWhite),
                ) {
                    SettingSwitch(
                        icon = Icons.Filled.Notifications,
                        title = "回礼提醒",
                        subtitle = "满 ${formatYuan(settings.thresholdFen, withSymbol = true)} 才出现在首页",
                        checked = settings.remindOn,
                        onChecked = { scope.launch { repo.setRemind(it) } },
                        onClick = {
                            thresholdDraft = settings.thresholdYuan.toString()
                            editingThreshold = true
                        },
                    )
                    SettingSwitch(
                        icon = Icons.Filled.People,
                        title = "家人不提醒",
                        subtitle = "家人的差额仍然可查看",
                        checked = settings.familyMute,
                        onChecked = { scope.launch { repo.setFamilyMute(it) } },
                    )
                    SettingSwitch(
                        icon = Icons.Filled.Lock,
                        title = "应用锁",
                        subtitle = if (settings.lockOn) "已开启 · 可用生物识别" else "关闭 · 可用生物识别",
                        checked = settings.lockOn,
                        onChecked = { want ->
                            if (!want) {
                                scope.launch { repo.setLock(false) }
                            } else {
                                activity.authenticate(
                                    title = "开启应用锁",
                                    onSuccess = { scope.launch { repo.setLock(true); session.unlocked = true } },
                                    onError = { session.toast(it) },
                                )
                            }
                        },
                    )
                    }
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardWhite),
                ) {
                    ActionRow(Icons.Filled.FileDownload, "导出 Excel", "亲友、礼簿、记录三张表") {
                        scope.launch {
                            runCatching { repo.exportWorkbook(snapshot, null) }
                                .onSuccess {
                                    pendingExport = it
                                    excelLauncher.launch("人情往来-${LocalDate.now()}.xlsx")
                                }
                                .onFailure { session.toast("导出失败，请重试") }
                        }
                    }
                    ActionRow(Icons.Filled.SaveAlt, "本地备份 / 恢复", "JSON，恢复前先预览笔数") {
                        backupMenu = true
                    }
                    ActionRow(Icons.Filled.Category, "礼簿类型与亲友分组", "可增删，系统项不可删") {
                        nav.navigate("categories")
                    }
                }
                if (backupMenu) {
                    PaperDialog(
                        title = "本地备份 / 恢复",
                        onDismiss = { backupMenu = false },
                        dismissText = "从文件恢复",
                        onDismissAction = {
                            backupMenu = false
                            restoreLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        },
                        confirmText = "保存备份",
                        onConfirm = {
                            backupMenu = false
                            scope.launch {
                                runCatching { repo.exportBackup(snapshot, settings) }
                                    .onSuccess {
                                        pendingBackup = it
                                        backupLauncher.launch("人情往来备份-${LocalDate.now()}.json")
                                    }
                                    .onFailure { session.toast("备份失败，请重试") }
                            }
                        },
                    ) {
                        Text("备份只留在这台手机。恢复前会先告诉你要替换多少笔。", color = InkSoft, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
                Text("导出和备份都不包含应用锁。", color = InkSoft, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
            }
            if (editingProfile) {
                var name by remember(settings.userName) { mutableStateOf(nameDraft) }
                var phone by remember(settings.userPhone) { mutableStateOf(phoneDraft) }
                PaperDialog(
                    title = "我的资料",
                    onDismiss = { editingProfile = false },
                    confirmText = "保存",
                    onConfirm = {
                        scope.launch {
                            repo.setProfile(name, phone)
                            editingProfile = false
                        }
                    },
                ) {
                    InlineField("姓名", name, { name = it })
                    InlineField("手机", phone, { phone = it })
                }
            }
            if (editingThreshold) {
                var text by remember { mutableStateOf(thresholdDraft) }
                PaperDialog(
                    title = "提醒阈值",
                    onDismiss = { editingThreshold = false },
                    confirmText = "保存",
                    confirmEnabled = text.toIntOrNull() != null,
                    onConfirm = {
                        val yuan = text.toIntOrNull() ?: return@PaperDialog
                        scope.launch {
                            repo.setThreshold(yuan)
                            editingThreshold = false
                        }
                    },
                ) {
                    InlineField("满多少元", text, { text = it.filter { ch -> ch.isDigit() } })
                }
            }
            restorePreview?.let { preview ->
                ConfirmDialog(
                    title = "恢复这份备份？",
                    body = "将替换本机 ${preview.contacts} 位亲友、${preview.books} 本礼簿、${preview.records} 条记录。确认后才会覆盖。",
                    confirm = "恢复",
                    danger = true,
                    onDismiss = { restorePreview = null; restoreText = null },
                    onConfirm = {
                        val text = restoreText
                        restorePreview = null
                        restoreText = null
                        if (text == null) return@ConfirmDialog
                        scope.launch {
                            runCatching { repo.restore(text) }
                                .onSuccess { session.toast("已恢复") }
                                .onFailure { session.toast("恢复失败，原来的账还在") }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Cinnabar, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, color = Ink, fontWeight = FontWeight.Medium)
            Text(subtitle, color = InkSoft, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedTrackColor = Cinnabar, checkedThumbColor = Color.White),
        )
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Cinnabar, modifier = Modifier.size(22.dp))
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, color = Ink, fontWeight = FontWeight.Medium)
            Text(subtitle, color = InkSoft, fontSize = 12.sp)
        }
    }
}

fun FragmentActivity.authenticate(title: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val prompt = BiometricPrompt(
        this,
        ContextCompat.getMainExecutor(this),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_CANCELED
                ) {
                    onError(errString.toString())
                }
            }
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder().setTitle(title)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (BiometricManager.from(this).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            onError("这台设备暂时不能验证身份")
            return
        }
        info.setAllowedAuthenticators(authenticators)
    } else {
        @Suppress("DEPRECATION")
        info.setDeviceCredentialAllowed(true)
    }
    prompt.authenticate(info.build())
}

@Composable
fun CategoriesScreen() {
    val nav = LocalNav.current
    val repo = LocalRepo.current
    val session = LocalSession.current
    val scope = rememberCoroutineScope()
    var typeName by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Honey).statusBarsPadding().navigationBarsPadding()) {
        PageHeader("类型与分组", onBack = { nav.popBackStack() })
        LedgerBody(Modifier.weight(1f)) { snapshot, _ ->
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardWhite)
                        .padding(16.dp),
                ) {
                    Text("礼簿类型", fontWeight = FontWeight.SemiBold, color = Ink)
                    snapshot.types.forEach { type ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(type.name, modifier = Modifier.weight(1f), color = Ink)
                            if (type.system) Text("系统", color = InkSoft, fontSize = 12.sp)
                            else IconButton(onClick = {
                                scope.launch {
                                    val ok = repo.deleteType(type.id)
                                    session.toast(if (ok) "已删除" else "已有礼簿在使用，不能删除")
                                }
                            }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = "删除", tint = Cinnabar, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Column(
                        Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Honey)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        InlineField("新的礼簿类型", typeName, { typeName = it })
                        Spacer(Modifier.height(8.dp))
                        PrimaryButton(
                            "添加类型",
                            enabled = typeName.isNotBlank(),
                            onClick = {
                                scope.launch {
                                    runCatching { repo.addType(typeName) }
                                        .onSuccess { typeName = "" }
                                        .onFailure { session.toast("请填写名称") }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
                Spacer(Modifier.height(20.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardWhite)
                        .padding(16.dp),
                ) {
                    Text("亲友分组", fontWeight = FontWeight.SemiBold, color = Ink)
                    snapshot.groups.forEach { group ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(group.name, modifier = Modifier.weight(1f), color = Ink)
                            if (group.system) Text("系统", color = InkSoft, fontSize = 12.sp)
                            else IconButton(onClick = {
                                scope.launch {
                                    val ok = repo.deleteGroup(group.id)
                                    session.toast(if (ok) "已删除" else "已有亲友在这个分组里，不能删除")
                                }
                            }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = "删除", tint = Cinnabar, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Column(
                        Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Honey)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        InlineField("新的分组", groupName, { groupName = it })
                        Spacer(Modifier.height(8.dp))
                        PrimaryButton(
                            "添加分组",
                            enabled = groupName.isNotBlank(),
                            onClick = {
                                scope.launch {
                                    runCatching { repo.addGroup(groupName) }
                                        .onSuccess { groupName = "" }
                                        .onFailure { session.toast("请填写名称") }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val activity = LocalContext.current as FragmentActivity
    val session = LocalSession.current
    androidx.compose.runtime.LaunchedEffect(Unit) {
        activity.authenticate("解锁人情往来", onSuccess = onUnlocked, onError = { session.toast(it) })
    }
    Column(
        Modifier.fillMaxSize().background(Honey).statusBarsPadding().navigationBarsPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(80.dp))
        Text("人情往来", fontSize = 32.sp, fontWeight = FontWeight.SemiBold, color = Cinnabar)
        Spacer(Modifier.height(8.dp))
        Text("账本已锁定", color = InkSoft)
        Spacer(Modifier.height(24.dp))
        com.renqing.zhang.ui.components.PrimaryButton("解锁", onClick = {
            activity.authenticate("解锁人情往来", onSuccess = onUnlocked, onError = { session.toast(it) })
        }, modifier = Modifier.fillMaxWidth())
    }
}
