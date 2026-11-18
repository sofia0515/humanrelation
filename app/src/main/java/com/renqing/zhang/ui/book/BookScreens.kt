package com.renqing.zhang.ui.book

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.renqing.zhang.data.BookDraft
import com.renqing.zhang.domain.DIR_IN
import com.renqing.zhang.domain.ROLE_GUEST
import com.renqing.zhang.domain.ROLE_HOST
import com.renqing.zhang.domain.RecordRowUi
import com.renqing.zhang.domain.formatCn
import com.renqing.zhang.domain.formatDot
import com.renqing.zhang.domain.formatIso
import com.renqing.zhang.domain.formatYuan
import com.renqing.zhang.domain.roleLabel
import com.renqing.zhang.domain.toUtcMillis
import com.renqing.zhang.domain.utcToLocalDate
import com.renqing.zhang.ui.LedgerBody
import com.renqing.zhang.ui.LocalNav
import com.renqing.zhang.ui.LocalRepo
import com.renqing.zhang.ui.LocalSession
import com.renqing.zhang.ui.components.AmountText
import com.renqing.zhang.ui.components.Avatar
import com.renqing.zhang.ui.components.ChoiceChip
import com.renqing.zhang.ui.components.ConfirmDialog
import com.renqing.zhang.ui.components.EmptyState
import com.renqing.zhang.ui.components.PageHeader
import com.renqing.zhang.ui.components.InlineField
import com.renqing.zhang.ui.components.PrimaryButton
import com.renqing.zhang.ui.components.SecondaryButton
import com.renqing.zhang.ui.components.SwipeDelete
import com.renqing.zhang.ui.record.ContactPicker
import com.renqing.zhang.ui.record.rememberDeleteRequest
import com.renqing.zhang.ui.theme.Amber
import com.renqing.zhang.ui.theme.AmountFamily
import com.renqing.zhang.ui.theme.CardWhite
import com.renqing.zhang.ui.theme.Cinnabar
import com.renqing.zhang.ui.theme.Hint
import com.renqing.zhang.ui.theme.Honey
import com.renqing.zhang.ui.theme.Ink
import com.renqing.zhang.ui.theme.InkSoft
import com.renqing.zhang.ui.theme.Line
import com.renqing.zhang.ui.theme.Pine
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun BookListScreen() {
    val nav = LocalNav.current
    val session = LocalSession.current
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<com.renqing.zhang.domain.BookCardUi?>(null) }
    Column(Modifier.fillMaxSize().background(Honey).statusBarsPadding()) {
        PageHeader(title = "礼簿") {
            IconButton(onClick = { nav.navigate("search") }) {
                Icon(Icons.Filled.Search, contentDescription = "搜索", tint = Ink)
            }
        }
        LedgerBody(Modifier.weight(1f)) { snapshot, _ ->
            val types = snapshot.types
            Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChoiceChip("全部", session.bookTypeId == null, Cinnabar) { session.bookTypeId = null }
                types.forEach { type ->
                    ChoiceChip(type.name, session.bookTypeId == type.id, Cinnabar) { session.bookTypeId = type.id }
                }
            }
            val cards = snapshot.books(session.bookTypeId, LocalDate.now())
            Box(Modifier.weight(1f)) {
                if (cards.isEmpty()) {
                    EmptyState(
                        title = if (session.bookTypeId == null) "还没有礼簿" else "这个类型还没有礼簿",
                        body = "一场酒记一本。先建礼簿，或先记一笔，稍后再归类。",
                        primary = "新建第一本礼簿",
                        onPrimary = { nav.navigate("bookEdit?id=-1") },
                        secondary = "先记一笔",
                        onSecondary = { nav.navigate("recordEdit?contactId=-1&bookId=-1&direction=&recordId=-1") },
                    )
                } else {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 88.dp)) {
                        cards.forEach { card ->
                            if (card.archived) {
                                BookCard(card) { nav.navigate("book/${card.id}") }
                            } else {
                                SwipeDelete(onDelete = { pendingDelete = card }, surface = Honey) {
                                    BookCard(card) { nav.navigate("book/${card.id}") }
                                }
                            }
                        }
                    }
                    FloatingActionButton(
                        onClick = { nav.navigate("bookEdit?id=-1") },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                        containerColor = Cinnabar,
                        contentColor = Color.White,
                        shape = CircleShape,
                    ) { Icon(Icons.Filled.Add, contentDescription = "新建礼簿") }
                }
            }
            }
        }
    }
    pendingDelete?.let { card ->
        ConfirmDialog(
            title = "删除这本礼簿？",
            body = "「${card.title}」和里面的记录会一起删除，不能恢复。",
            confirm = "删除",
            onDismiss = { pendingDelete = null },
            onConfirm = {
                pendingDelete = null
                scope.launch {
                    runCatching { repo.deleteBook(card.id) }
                        .onSuccess { session.toast("已删除") }
                        .onFailure { session.toast(it.message ?: "删除失败") }
                }
            },
        )
    }
}

@Composable
private fun BookCard(card: com.renqing.zhang.domain.BookCardUi, onClick: () -> Unit) {
    val showSuggestion = card.suggestion != null
    val amount = if (showSuggestion) card.suggestion!! else card.total
    val amountColor = when {
        showSuggestion -> Amber
        card.role == ROLE_HOST -> Pine
        else -> Cinnabar
    }
    val caption = when {
        showSuggestion -> "建议"
        card.role == ROLE_HOST -> "收礼"
        else -> "送出"
    }
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (card.planning) {
            Canvas(Modifier.padding(start = 2.dp).width(8.dp).height(52.dp)) {
                drawLine(
                    color = Color(0xFFD4A017),
                    start = Offset(size.width / 2f, 0f),
                    end = Offset(size.width / 2f, size.height),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()), 0f),
                )
            }
        } else {
            Box(
                Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                    .background(Cinnabar),
            )
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, top = 14.dp, bottom = 14.dp, end = 8.dp)) {
            Text(card.title, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                if (card.count == 0) "${formatDot(card.date)} · 还没有记录" else "${formatDot(card.date)} · ${card.count} 笔",
                color = InkSoft,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniTag(if (card.role == ROLE_HOST) "主家" else "随礼")
                MiniTag(when {
                    card.archived -> "已封存"
                    card.planning -> "筹备中"
                    else -> "已结束"
                })
            }
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 16.dp)) {
            Text(formatYuan(amount), color = amountColor, fontFamily = AmountFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            Text(caption, color = amountColor, fontSize = 12.sp)
        }
    }
}

private val IdentityTrack = Color(0xFFEFE7DE)
private val IdentityIdle = Color(0xFF7D7268)

@Composable
private fun IdentitySwitch(guest: Boolean, onHost: () -> Unit, onGuest: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(IdentityTrack)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IdentityPiece("我是主家", selected = !guest, Modifier.weight(1f), onHost)
        IdentityPiece("我去随礼", selected = guest, Modifier.weight(1f), onGuest)
    }
}

@Composable
private fun IdentityPiece(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (selected) Pine else IdentityIdle,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun MiniTag(text: String) {
    val (background, foreground) = when (text) {
        "主家" -> com.renqing.zhang.ui.theme.PineSoft to Pine
        "随礼" -> com.renqing.zhang.ui.theme.CinnabarSoft to Cinnabar
        "筹备中" -> com.renqing.zhang.ui.theme.AmberSoft to Amber
        "已结束" -> com.renqing.zhang.ui.theme.SlateSoft to com.renqing.zhang.ui.theme.Slate
        else -> Honey to InkSoft
    }
    Text(
        text,
        fontSize = 11.sp,
        color = foreground,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
fun BookDetailScreen(bookId: Long) {
    val nav = LocalNav.current
    val repo = LocalRepo.current
    val session = LocalSession.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var kind by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val delete = rememberDeleteRequest()
    var exportBytes by remember { mutableStateOf<ByteArray?>(null) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        val bytes = exportBytes ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        }.onSuccess { session.toast("已导出") }.onFailure { session.toast("导出失败，请重试") }
    }
    Column(Modifier.fillMaxSize().background(Honey).statusBarsPadding().navigationBarsPadding()) {
        LedgerBody(Modifier.weight(1f)) { snapshot, _ ->
            val detail = snapshot.bookDetail(bookId, LocalDate.now(), kind)
            Column(Modifier.fillMaxSize()) {
            if (detail == null) {
                PageHeader("礼簿", onBack = { nav.popBackStack() })
                Text("这本礼簿已经不在了", modifier = Modifier.padding(16.dp), color = InkSoft)
            } else {
            PageHeader("礼簿", onBack = { nav.popBackStack() }) {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = Ink)
                }
                DropdownMenu(
                    expanded = menu,
                    onDismissRequest = { menu = false },
                    containerColor = CardWhite,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    DropdownMenuItem(text = { Text("编辑礼簿", color = if (detail.archived) InkSoft else Ink) }, onClick = {
                        menu = false
                        if (detail.archived) session.toast("这本礼簿已封存，不能修改")
                        else nav.navigate("bookEdit?id=$bookId")
                    })
                    HorizontalDivider(color = Line, modifier = Modifier.padding(horizontal = 12.dp))
                    DropdownMenuItem(text = { Text("导出本簿", color = Ink) }, onClick = {
                        menu = false
                        scope.launch {
                            runCatching { repo.exportWorkbook(snapshot, bookId) }
                                .onSuccess {
                                    exportBytes = it
                                    exporter.launch("${detail.title}.xlsx")
                                }
                                .onFailure { session.toast("导出失败，请重试") }
                        }
                    })
                    HorizontalDivider(color = Line, modifier = Modifier.padding(horizontal = 12.dp))
                    DropdownMenuItem(
                        text = { Text(if (detail.archived) "取消封存" else "封存", color = Ink) },
                        onClick = {
                            menu = false
                            scope.launch {
                                val locking = !detail.archived
                                runCatching {
                                    if (locking) repo.archiveBook(bookId) else repo.unarchiveBook(bookId)
                                }.onSuccess { session.toast(if (locking) "已封存" else "已取消封存") }
                                    .onFailure { session.toast(if (locking) "封存失败" else "取消封存失败") }
                            }
                        },
                    )
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                Column(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF8E241C), Cinnabar)))
                        .padding(16.dp),
                ) {
                    Text(
                        "${roleLabel(detail.role)} · ${when {
                            detail.archived -> "已封存"
                            detail.planning -> "筹备中"
                            else -> "已结束"
                        }} · ${detail.typeName}",
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 12.sp,
                    )
                    Text(detail.title, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, color = Color.White, lineHeight = 42.sp)
                    val place = detail.place.ifBlank { "地点未填" }
                    Text("${formatCn(detail.date)} · $place", color = Color.White.copy(alpha = 0.82f), fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    AmountText(detail.total, Color.White, size = 36, withSymbol = true)
                    val biggest = if (detail.biggestName != null && detail.biggestAmount != null) {
                        " · 最大一笔 ${formatYuan(detail.biggestAmount, withSymbol = true)} ${detail.biggestName}"
                    } else ""
                    Text(
                        "${detail.count} 笔${if (detail.role == ROLE_HOST) "收礼" else "送礼"}$biggest",
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 13.sp,
                    )
                    if (detail.note.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(detail.note, color = Color.White.copy(alpha = 0.82f), fontSize = 13.sp)
                    }
                }
                Row(
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ChoiceChip("全部", kind == null) { kind = null }
                    ChoiceChip("现金", kind == "cash") { kind = "cash" }
                    ChoiceChip("礼品", kind == "gift") { kind = "gift" }
                }
                if (detail.archived) {
                    Text(
                        "已封存，不能修改、删除或记一笔。",
                        color = InkSoft,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                if (detail.rows.isEmpty()) {
                    EmptyState(
                        title = "还没有记录",
                        body = if (detail.archived) "这本礼簿已封存。" else "记一笔后，合计和亲友差额会一起更新。",
                        primary = if (detail.archived) null else "记一笔",
                        onPrimary = if (detail.archived) null else {
                            {
                                nav.navigate("recordEdit?contactId=-1&bookId=$bookId&direction=${detail.defaultDirection}&recordId=-1")
                            }
                        },
                    )
                } else {
                    val visible = if (expanded) detail.rows else detail.rows.take(5)
                    visible.forEach { row ->
                        if (detail.archived) {
                            BookRecordRow(row) { nav.navigate("record/${row.id}") }
                        } else {
                            SwipeDelete(onDelete = { delete.ask(snapshot, row.id) }, surface = Honey) {
                                BookRecordRow(row) { nav.navigate("record/${row.id}") }
                            }
                        }
                    }
                    val rest = detail.rows.size - visible.size
                    if (rest > 0) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { expanded = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("查看其余 $rest 笔", color = Ink, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                Text("当前按金额从高到低", color = InkSoft, fontSize = 12.sp)
                            }
                            Icon(Icons.Filled.ExpandMore, contentDescription = "展开", tint = InkSoft)
                        }
                    }
                    if (!detail.archived && (detail.rows.size <= 5 || expanded)) {
                        Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                            FloatingActionButton(
                                onClick = {
                                    nav.navigate("recordEdit?contactId=-1&bookId=$bookId&direction=${detail.defaultDirection}&recordId=-1")
                                },
                                containerColor = Cinnabar,
                                contentColor = Color.White,
                                shape = CircleShape,
                            ) { Icon(Icons.Filled.Add, contentDescription = "记一笔") }
                        }
                    }
                }
            }
            }
            }
            delete.Dialog()
        }
    }
}

@Composable
private fun BookRecordRow(row: RecordRowUi, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(row.letter, row.title)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(row.title, fontWeight = FontWeight.Medium, color = Ink)
                Text(row.subtitle, color = InkSoft, fontSize = 12.sp)
                if (row.gift) Text("估价", color = Amber, fontSize = 11.sp)
            }
            Text(
                formatYuan(kotlin.math.abs(row.amount)),
                color = if (row.direction == DIR_IN) Pine else Cinnabar,
                fontFamily = AmountFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(Line),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookEditScreen(bookId: Long) {
    val nav = LocalNav.current
    val repo = LocalRepo.current
    val session = LocalSession.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var autoTitle by remember { mutableStateOf("") }
    var typeId by remember { mutableStateOf(0L) }
    var role by remember { mutableStateOf(ROLE_HOST) }
    var contactId by remember { mutableStateOf<Long?>(null) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var place by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var archived by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingContact by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    Column(Modifier.fillMaxSize().background(Honey).statusBarsPadding().navigationBarsPadding()) {
        PageHeader(if (bookId > 0) "编辑礼簿" else "新建礼簿", onBack = { nav.popBackStack() })
        LedgerBody(Modifier.weight(1f)) { snapshot, _ ->
            LaunchedEffect(bookId) {
                val book = if (bookId > 0) snapshot.book(bookId) else null
                if (book != null) {
                    title = book.title
                    typeId = book.typeId
                    role = book.role
                    contactId = book.contactId ?: snapshot.linkedContactId(book)
                    date = com.renqing.zhang.domain.parseDate(book.date)
                    place = book.place
                    note = book.note
                    archived = book.archived
                }
            }
            val guest = role == ROLE_GUEST
            val person = contactId?.let { snapshot.contact(it) }
            LaunchedEffect(contactId, typeId, role, snapshot) {
                if (role != ROLE_GUEST) return@LaunchedEffect
                val name = contactId?.let { snapshot.contact(it)?.name }.orEmpty()
                val typeName = snapshot.type(typeId)?.name.orEmpty()
                val next = if (name.isBlank() || typeName.isBlank()) "" else "${name}家$typeName"
                if (next.isNotBlank() && (title.isBlank() || title == autoTitle)) title = next
                autoTitle = next
            }
            val guestReady = typeId > 0L && (!guest || person != null)
            Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                Text("我的身份", color = Ink, fontSize = 15.sp, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 10.dp))
                Box(Modifier.padding(horizontal = 16.dp)) {
                    IdentitySwitch(
                        guest = guest,
                        onHost = { role = ROLE_HOST },
                        onGuest = { role = ROLE_GUEST },
                    )
                }
                if (guest) {
                    Row(
                        Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable { pickingContact = true },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("亲友", color = InkSoft, fontSize = 16.sp, modifier = Modifier.widthIn(min = 72.dp).padding(end = 12.dp))
                        Text(
                            person?.let { "${it.name} · ${it.relation}" } ?: "请选择",
                            color = if (person == null) Hint else Ink,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Text("›", color = InkSoft)
                    }
                    Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(Line))
                    if (showError && person == null) {
                        Text("请选择亲友", color = Cinnabar, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    }
                }
                Text("类型", color = InkSoft, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, top = 12.dp))
                FlowRow(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    snapshot.types.forEach { type ->
                        ChoiceChip(type.name, typeId == type.id) {
                            typeId = type.id
                            showError = false
                        }
                    }
                }
                if (showError && typeId == 0L) {
                    Text("请选择类型", color = Cinnabar, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
                InlineField(
                    label = "名称",
                    value = title,
                    onValueChange = { title = it; showError = false },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                if (showError && title.isBlank()) {
                    Text("请填写这本账的名称", color = Cinnabar, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
                Row(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { pickingDate = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("日期", color = InkSoft, fontSize = 16.sp, modifier = Modifier.widthIn(min = 72.dp).padding(end = 12.dp))
                    Text(formatIso(date), color = Ink, fontSize = 16.sp)
                }
                Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(Line))
                if (date.isAfter(LocalDate.now())) {
                    Text(
                        "晚于今天，保存后状态为筹备中",
                        color = Amber,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
                val noteHint = if (guest) {
                    val advised = person?.let { snapshot.latestReceived(it.id) }
                    if (advised != null) "可参考她最近一次随礼 ${formatYuan(advised, withSymbol = true)}" else "选填"
                } else {
                    "选填"
                }
                InlineField("地点", place, { place = it }, placeholder = "选填", modifier = Modifier.padding(horizontal = 16.dp))
                InlineField("备注", note, { note = it }, placeholder = noteHint, modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp))
                Text(
                    "身份会决定这本账里记一笔时的默认方向：主家默认收礼，随礼默认送礼。",
                    color = InkSoft,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
                if (archived) {
                    Text("这本礼簿已封存，不能修改。", color = InkSoft, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
                PrimaryButton(
                    text = "保存礼簿",
                    enabled = title.isNotBlank() && guestReady && !archived,
                    onClick = {
                        if (archived) {
                            session.toast("这本礼簿已封存，不能修改")
                            return@PrimaryButton
                        }
                        if (title.isBlank() || typeId == 0L || (guest && person == null)) {
                            showError = true
                            return@PrimaryButton
                        }
                        scope.launch {
                            runCatching {
                                repo.saveBook(
                                    BookDraft(
                                        bookId.coerceAtLeast(0),
                                        title,
                                        typeId,
                                        role,
                                        formatIso(date),
                                        place,
                                        note,
                                        contactId = if (guest) person?.id else null,
                                    ),
                                    archived = archived && bookId > 0,
                                )
                            }.onSuccess { id ->
                                session.toast("已保存")
                                if (bookId < 0) {
                                    nav.popBackStack()
                                    nav.navigate("book/$id")
                                } else {
                                    nav.popBackStack()
                                }
                            }.onFailure {
                                session.toast(it.message ?: "保存失败")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }
            if (pickingContact) {
                ModalBottomSheet(
                    onDismissRequest = { pickingContact = false },
                    sheetState = sheetState,
                    containerColor = Honey,
                ) {
                    ContactPicker(
                        snapshot,
                        person?.id,
                        onPick = {
                            contactId = it
                            showError = false
                            pickingContact = false
                        },
                        onCreate = {
                            contactId = it
                            showError = false
                            pickingContact = false
                        },
                    )
                }
            }
            if (pickingDate) {
                val state = rememberDatePickerState(initialSelectedDateMillis = date.toUtcMillis())
                DatePickerDialog(
                    onDismissRequest = { pickingDate = false },
                    confirmButton = {
                        PrimaryButton("确定", onClick = {
                            state.selectedDateMillis?.let { date = it.utcToLocalDate() }
                            pickingDate = false
                        })
                    },
                    dismissButton = { SecondaryButton("取消", onClick = { pickingDate = false }) },
                    colors = DatePickerDefaults.colors(containerColor = CardWhite),
                ) { DatePicker(state = state) }
            }
        }
    }
}
