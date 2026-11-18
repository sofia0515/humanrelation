package com.renqing.zhang.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renqing.zhang.data.BackupPreview
import com.renqing.zhang.ui.LocalRepo
import com.renqing.zhang.ui.LocalSession
import com.renqing.zhang.ui.components.ConfirmDialog
import com.renqing.zhang.ui.components.PrimaryButton
import com.renqing.zhang.ui.theme.CardWhite
import com.renqing.zhang.ui.theme.Cinnabar
import com.renqing.zhang.ui.theme.Honey
import com.renqing.zhang.ui.theme.Ink
import com.renqing.zhang.ui.theme.InkSoft
import com.renqing.zhang.ui.theme.Line
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val repo = LocalRepo.current
    val session = LocalSession.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState { 3 }
    var preview by remember { mutableStateOf<BackupPreview?>(null) }
    var payload by remember { mutableStateOf<String?>(null) }
    val opener = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("empty")
            preview = repo.previewBackup(text)
            payload = text
        }.onFailure { session.toast("备份文件无法读取") }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Honey)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                "跳过",
                color = InkSoft,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            scope.launch {
                                repo.finishOnboarding()
                                onFinished()
                            }
                        },
                    )
                    .padding(vertical = 8.dp),
            )
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            val model = pages[page]
            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.height(28.dp))
                Box(Modifier.size(84.dp).clip(CircleShape).background(CardWhite), contentAlignment = Alignment.Center) {
                    Icon(model.icon, contentDescription = null, tint = Cinnabar, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(28.dp))
                Text(model.title, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Spacer(Modifier.height(12.dp))
                Text(model.body, color = InkSoft, fontSize = 15.sp, lineHeight = 22.sp)
                Spacer(Modifier.height(28.dp))
                val others = pages.mapIndexed { index, item -> index to item }.filter { it.first != page }
                others.forEachIndexed { position, (index, other) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { scope.launch { pager.animateScrollToPage(index) } }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(other.icon, contentDescription = null, tint = Ink, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.size(12.dp))
                        Column {
                            Text(other.title, color = Ink, fontWeight = FontWeight.Medium)
                            Text("第 ${index + 1} 页 · ${other.blurb}", color = InkSoft, fontSize = 13.sp)
                        }
                    }
                    if (position < others.lastIndex) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(3) { index ->
                Box(
                    Modifier
                        .padding(4.dp)
                        .size(if (pager.currentPage == index) 18.dp else 6.dp, 6.dp)
                        .clip(CircleShape)
                        .background(if (pager.currentPage == index) Cinnabar else Color(0xFFE0D5CC)),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            text = if (pager.currentPage == 2) "进入首页" else "继续",
            onClick = {
                if (pager.currentPage < 2) {
                    scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                } else {
                    scope.launch {
                        repo.finishOnboarding()
                        onFinished()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "已有备份，恢复",
            color = InkSoft,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { opener.launch(arrayOf("application/json", "text/*", "*/*")) },
                )
                .padding(vertical = 8.dp),
        )
    }
    preview?.let { info ->
        ConfirmDialog(
            title = "恢复这份备份？",
            body = "将替换本机 ${info.contacts} 位亲友、${info.books} 本礼簿、${info.records} 条记录。确认后才会覆盖。",
            confirm = "恢复",
            onDismiss = { preview = null; payload = null },
            onConfirm = {
                val text = payload
                preview = null
                payload = null
                if (text == null) return@ConfirmDialog
                scope.launch {
                    runCatching {
                        repo.restore(text)
                        repo.finishOnboarding()
                    }.onSuccess {
                        session.toast("已恢复")
                        onFinished()
                    }.onFailure { session.toast("恢复失败") }
                }
            },
        )
    }
}

private data class Page(val icon: ImageVector, val title: String, val body: String, val blurb: String)

private val pages = listOf(
    Page(
        Icons.Outlined.MenuBook,
        "一场酒，一本礼簿",
        "主家默认收礼，去随礼默认送礼。日期晚于今天的，就是筹备中。",
        "婚礼、满月、乔迁分开记",
    ),
    Page(
        Icons.Outlined.People,
        "按亲友，算清该回谁",
        "同一个人的收礼、送礼会合成一条长期的账。待回的事，首页会提醒你。",
        "待回的事，首页会提醒你",
    ),
    Page(
        Icons.Outlined.Shield,
        "账先留在这台手机",
        "不上传，不注册。备份和导出都由你决定。",
        "备份和导出由你决定",
    ),
)
