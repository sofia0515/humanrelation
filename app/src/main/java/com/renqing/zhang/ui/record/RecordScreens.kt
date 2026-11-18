package com.renqing.zhang.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renqing.zhang.data.ContactDraft
import com.renqing.zhang.data.RecordDraft
import com.renqing.zhang.domain.DIR_IN
import com.renqing.zhang.domain.DIR_OUT
import com.renqing.zhang.domain.KIND_CASH
import com.renqing.zhang.domain.KIND_GIFT
import com.renqing.zhang.domain.directionLabel
import com.renqing.zhang.domain.fenToInput
import com.renqing.zhang.domain.formatDot
import com.renqing.zhang.domain.formatIso
import com.renqing.zhang.domain.formatYuan
import com.renqing.zhang.domain.kindLabel
import com.renqing.zhang.domain.parseDate
import com.renqing.zhang.domain.parseYuanToFen
import com.renqing.zhang.domain.roleLabel
import com.renqing.zhang.domain.toUtcMillis
import com.renqing.zhang.domain.utcToLocalDate
import com.renqing.zhang.ui.LedgerBody
import com.renqing.zhang.ui.LocalNav
import com.renqing.zhang.ui.LocalRepo
import com.renqing.zhang.ui.LocalSession
import com.renqing.zhang.ui.components.Avatar
import com.renqing.zhang.ui.components.ChoiceChip
import com.renqing.zhang.ui.components.Keypad
import com.renqing.zhang.ui.components.PageHeader
import com.renqing.zhang.ui.components.InlineField
import com.renqing.zhang.ui.components.PrimaryButton
import com.renqing.zhang.ui.components.SearchField
import com.renqing.zhang.ui.components.SecondaryButton
import com.renqing.zhang.ui.components.SwipeDelete
import com.renqing.zhang.ui.theme.AmountFamily
import com.renqing.zhang.ui.theme.CardWhite
import com.renqing.zhang.ui.theme.Cinnabar
import com.renqing.zhang.ui.theme.Honey
import com.renqing.zhang.ui.theme.Ink
import com.renqing.zhang.ui.theme.Hint
import com.renqing.zhang.ui.theme.InkSoft
import com.renqing.zhang.ui.theme.Line
import com.renqing.zhang.ui.theme.Pine
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun AllRecordsScreen() {
    val nav = LocalNav.current
    val delete = rememberDeleteRequest()
    Column(Modifier.fillMaxSize().background(Honey).statusBarsPadding().navigationBarsPadding()) {
        PageHeader("全部记录", onBack = { nav.popBackStack() })
        LedgerBody(Modifier.weight(1f)) { snapshot, _ ->
            val rows = snapshot.allRecords(LocalDate.now())
            if (rows.isEmpty()) {
                Text("还没有记录。", color = InkSoft, modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn {
                    itemsIndexed(rows, key = { _, row -> row.id }) { index, row ->
                        Column {
                            val recordLine = @Composable {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { nav.navigate("record/${row.id}") }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(row.title, color = Ink, fontWeight = FontWeight.Medium)
                                        Text(row.subtitle, color = InkSoft, fontSize = 12.sp)
                                    }
                                    Text(
                                        formatYuan(row.amount, withSign = true),
                                        color = if (row.direction == DIR_IN) Pine else Cinnabar,
                                        fontFamily = AmountFamily,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                            if (row.bookArchived) recordLine() else SwipeDelete(onDelete = { delete.ask(snapshot, row.id) }, surface = Honey) { recordLine() }
                            if (index < rows.lastIndex) {
                                Box(
                                    Modifier
                                        .padding(horizontal = 16.dp)
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Line),
                                )
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
fun RecordDetailScreen(recordId: Long) {
    val nav = LocalNav.current
    val session = LocalSession.current
    val delete = rememberDeleteRequest()
    Column(Modifier.fillMaxSize().background(Honey).statusBarsPadding().navigationBarsPadding()) {
        LedgerBody(Modifier.weight(1f)) { snapshot, _ ->
            val detail = snapshot.recordDetail(recordId)
            val locked = detail?.bookId?.let { snapshot.book(it)?.archived } == true
            Column(Modifier.fillMaxSize()) {
            PageHeader("记录", onBack = { nav.popBackStack() }) {
                if (detail != null) {
                    IconButton(onClick = {
                        if (locked) session.toast("这本礼簿已封存，不能修改")
                        else nav.navigate("recordEdit?contactId=-1&bookId=-1&direction=&recordId=$recordId")
                    }) { Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = if (locked) InkSoft else Ink) }
                }
            }
            if (detail == null) {
                Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("已删除", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("这条记录已经不在了", color = InkSoft, fontSize = 14.sp)
                }
            } else Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Column(Modifier.padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardWhite).padding(20.dp)) {
                Text(
                    "${directionLabel(detail.direction)} · ${kindLabel(detail.kind, detail.giftName)}",
                    color = if (detail.direction == DIR_IN) Pine else Cinnabar,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    formatYuan(detail.amount, withSymbol = true),
                    color = if (detail.direction == DIR_IN) Pine else Cinnabar,
                    fontFamily = AmountFamily,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (detail.kind == KIND_GIFT) {
                    Spacer(Modifier.height(4.dp))
                    Text("估价计入差额", color = InkSoft, fontSize = 12.sp)
                }
                Spacer(Modifier.height(18.dp))
                InfoLine("亲友", "${detail.contactName}  ${detail.relation} · ${detail.groupName}") { nav.navigate("contact/${detail.contactId}") }
                InfoLine(
                    "礼簿",
                    if (detail.bookTitle == null) "未归入礼簿" else "${detail.bookTitle}\n${roleLabel(detail.bookRole ?: "")} · ${detail.bookDate?.let { formatDot(it) }.orEmpty()}",
                ) { detail.bookId?.let { nav.navigate("book/$it") } }
                InfoLine("日期", formatDot(detail.date), null)
                if (detail.note.isNotBlank()) InfoLine("备注", detail.note, null)
            }
            Spacer(Modifier.height(20.dp))
            if (locked) {
                Text(
                    "这本礼簿已封存，不能修改或删除。",
                    color = InkSoft,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            } else {
                Box(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Cinnabar)
                        .clickable { delete.ask(snapshot, recordId) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("删除这条记录", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
            }
            delete.Dialog()
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String, onClick: (() -> Unit)?) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
    ) {
        Text(label, color = InkSoft, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = Ink, fontSize = 16.sp, lineHeight = 22.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordEditScreen(contactId: Long, bookId: Long, direction: String, recordId: Long) {
    val nav = LocalNav.current
    val repo = LocalRepo.current
    val session = LocalSession.current
    val scope = rememberCoroutineScope()
    var dir by remember { mutableStateOf(if (direction == DIR_OUT) DIR_OUT else DIR_IN) }
    var amountText by remember { mutableStateOf("") }
    var pickedContact by remember { mutableStateOf(contactId.takeIf { it > 0 }) }
    var pickedBook by remember { mutableStateOf(bookId.takeIf { it > 0 }) }
    var skipBook by remember { mutableStateOf(false) }
    var kind by remember { mutableStateOf(KIND_CASH) }
    var giftName by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var note by remember { mutableStateOf("") }
    var showContacts by remember { mutableStateOf(false) }
    var showBooks by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var keypad by remember { mutableStateOf(true) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(Modifier.fillMaxSize().background(Honey).statusBarsPadding().navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
            Text(
                if (recordId > 0) "编辑" else "记一笔",
                color = Ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
                Text(
                    "取消",
                    color = Color(0xFF7D7268),
                    fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { nav.popBackStack() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }
        LedgerBody(Modifier.weight(1f)) { snapshot, _ ->
            LaunchedEffect(recordId, contactId, bookId, direction) {
                if (recordId > 0) {
                    snapshot.recordDetail(recordId)?.let { detail ->
                        dir = detail.direction
                        amountText = fenToInput(detail.amount)
                        pickedContact = detail.contactId
                        pickedBook = detail.bookId
                        skipBook = detail.bookId == null
                        kind = detail.kind
                        giftName = detail.giftName
                        date = detail.date
                        note = detail.note
                    }
                } else {
                    if (direction == DIR_IN || direction == DIR_OUT) dir = direction
                    if (contactId > 0) pickedContact = contactId
                    if (bookId > 0) {
                        pickedBook = bookId
                        val linked = snapshot.book(bookId)
                        if (direction.isBlank() && linked != null) {
                            dir = if (linked.role == com.renqing.zhang.domain.ROLE_HOST) DIR_IN else DIR_OUT
                        }
                    }
                }
            }
            val person = pickedContact?.let { snapshot.contact(it) }
            val book = if (skipBook) null else pickedBook?.let { snapshot.book(it) }
            val fen = parseYuanToFen(amountText.replace(",", ""))
            val missingContact = person == null
            val missingGift = kind == KIND_GIFT && giftName.isBlank()
            val button = when {
                missingContact -> "请先选择亲友"
                recordId > 0 -> "保存"
                dir == DIR_IN -> "记下这笔收礼"
                else -> "记下这笔送礼"
            }
            Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Row(Modifier.padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(LineSoft()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Seg("收礼", dir == DIR_IN, Modifier.weight(1f)) { dir = DIR_IN }
                    Seg("送礼", dir == DIR_OUT, Modifier.weight(1f)) { dir = DIR_OUT }
                }
                Text(
                    if (dir == DIR_IN) "收款金额" else "随礼金额",
                    color = if (dir == DIR_IN) Color(0xFF7D7268) else InkSoft,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Text(
                    if (amountText.isBlank()) "¥0" else "¥$amountText",
                    color = if (dir == DIR_IN) Pine else Cinnabar,
                    fontFamily = AmountFamily,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clickable { keypad = true },
                )
                PickerRow("亲友", person?.let { "${it.name} · ${it.relation}" } ?: "请选择") { showContacts = true; keypad = false }
                PickerRow("礼簿", if (skipBook || book == null) "不归入礼簿" else book.title) { showBooks = true; keypad = false }
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("现金", kind == KIND_CASH) { kind = KIND_CASH }
                    ChoiceChip("礼品", kind == KIND_GIFT) { kind = KIND_GIFT; keypad = false }
                }
                if (kind == KIND_GIFT) {
                    InlineField(
                        "礼品名称",
                        giftName,
                        { giftName = it },
                        placeholder = "估价计入差额",
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                PickerRow("日期", formatIso(date)) { showDate = true; keypad = false }
                InlineField(
                    if (skipBook || book == null) "事由" else "备注",
                    note,
                    { note = it; keypad = false },
                    placeholder = "选填，例如春节",
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            val recordLocked = (recordId > 0 && snapshot.recordDetail(recordId)?.bookId?.let { snapshot.book(it)?.archived } == true) || book?.archived == true
            if (recordLocked) {
                Text("这本礼簿已封存，不能记一笔或修改。", color = InkSoft, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
            PrimaryButton(
                text = button,
                enabled = !missingContact && fen > 0 && !missingGift && !recordLocked,
                onClick = {
                    if (recordLocked) {
                        session.toast("这本礼簿已封存，不能修改")
                        return@PrimaryButton
                    }
                    scope.launch {
                        val who = person ?: return@launch
                        val linkedBook = if (skipBook) null else book
                        runCatching {
                            repo.saveRecord(
                                RecordDraft(
                                    id = recordId.coerceAtLeast(0),
                                    contactId = who.id,
                                    bookId = linkedBook?.id,
                                    direction = dir,
                                    kind = kind,
                                    amount = fen,
                                    giftName = if (kind == KIND_GIFT) giftName else "",
                                    date = formatIso(date),
                                    note = note,
                                ),
                            )
                        }.onSuccess {
                            val where = linkedBook?.let { "${who.name}与${it.title}" } ?: who.name
                            session.toast(if (recordId > 0) "已保存" else "已保存，已归入$where")
                            nav.popBackStack()
                        }.onFailure { session.toast(it.message ?: "保存失败，还停在这一页") }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (keypad && kind != KIND_GIFT) {
                Keypad { key -> amountText = applyKey(amountText, key) }
            }
            }
            if (showContacts) {
                ModalBottomSheet(onDismissRequest = { showContacts = false }, sheetState = sheetState, containerColor = Honey) {
                    ContactPicker(snapshot, person?.id, onPick = { pickedContact = it; showContacts = false }, onCreate = { id ->
                        pickedContact = id
                        showContacts = false
                    })
                }
            }
            if (showBooks) {
                ModalBottomSheet(onDismissRequest = { showBooks = false }, sheetState = sheetState, containerColor = Honey) {
                    BookPicker(snapshot, if (skipBook) null else book?.id, onNone = {
                        skipBook = true
                        pickedBook = null
                        showBooks = false
                    }, onPick = {
                        skipBook = false
                        pickedBook = it
                        showBooks = false
                    })
                }
            }
            if (showDate) {
                val state = rememberDatePickerState(initialSelectedDateMillis = date.toUtcMillis())
                DatePickerDialog(
                    onDismissRequest = { showDate = false },
                    confirmButton = {
                        PrimaryButton("确定", onClick = {
                            state.selectedDateMillis?.let { date = it.utcToLocalDate() }
                            showDate = false
                        })
                    },
                    dismissButton = { SecondaryButton("取消", onClick = { showDate = false }) },
                    colors = DatePickerDefaults.colors(containerColor = CardWhite),
                ) { DatePicker(state = state) }
            }
        }
    }
}

@Composable
private fun Seg(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) Ink else InkSoft,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) CardWhite else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    )
}

@Composable
private fun PickerRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = InkSoft, fontSize = 16.sp, modifier = Modifier.width(56.dp))
        Text(value, color = if (value == "请选择") Hint else Ink, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text("›", color = InkSoft)
    }
}

@Composable
private fun LineSoft() = Color(0xFFE7DFD6)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ContactPicker(
    snapshot: com.renqing.zhang.domain.Snapshot,
    selectedId: Long?,
    onPick: (Long) -> Unit,
    onCreate: (Long) -> Unit,
) {
    val repo = LocalRepo.current
    val session = LocalSession.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var draftName by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var groupId by remember { mutableStateOf(snapshot.groups.firstOrNull { it.key == "relative" }?.id ?: 0L) }
    val filtered = snapshot.contacts.filter {
        query.isBlank() || it.name.contains(query, true) || it.relation.contains(query, true)
    }
    val sameName = filtered.filter { it.name == query.trim() }
    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
        Text("选择亲友", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp), fontSize = 18.sp, color = Ink)
        SearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "搜索姓名或关系",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        if (sameName.size >= 2) {
            Text(
                "有 ${sameName.size} 位叫${query.trim()}。请按关系点选，不会自动并成一个人。",
                color = com.renqing.zhang.ui.theme.Amber,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(com.renqing.zhang.ui.theme.AmberSoft)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
        if (!creating) LazyColumn(Modifier.height(320.dp).padding(top = 8.dp)) {
            itemsIndexed(filtered, key = { _, person -> person.id }) { index, person ->
                val selected = person.id == selectedId
                Column {
                Row(
                    Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) com.renqing.zhang.ui.theme.CinnabarSoft else Color.Transparent)
                        .clickable { onPick(person.id) }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(person.name.take(1), person.name)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(person.name, color = Ink, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${person.relation} · ${snapshot.group(person.groupId)?.name.orEmpty()}",
                            color = InkSoft,
                            fontSize = 12.sp,
                        )
                    }
                    if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Cinnabar)
                }
                if (index < filtered.lastIndex) {
                    Box(
                        Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Line),
                    )
                }
                }
            }
        }
        if (!creating) {
            Box(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, com.renqing.zhang.ui.theme.Line, RoundedCornerShape(14.dp))
                    .background(CardWhite)
                    .clickable {
                    draftName = query.trim()
                    creating = true
                },
                contentAlignment = Alignment.Center,
            ) {
                Text(if (query.isBlank()) "新建亲友" else "新建「${query.trim()}」", color = Ink, fontWeight = FontWeight.Medium)
            }
        } else {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
                InlineField("姓名", draftName, { draftName = it })
                InlineField("关系", relation, { relation = it }, placeholder = "如舅舅、同事")
                Text("分组", color = com.renqing.zhang.ui.theme.InkSoft, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                FlowRow(
                    Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    snapshot.groups.forEach { group ->
                        ChoiceChip(group.name, groupId == group.id) { groupId = group.id }
                    }
                }
                InlineField("手机", phone, { phone = it }, placeholder = "选填")
                InlineField("备注", note, { note = it }, placeholder = "选填")
                Text(
                    "改资料不会改已经记下的金额。同名的人各自算各自的账。",
                    color = InkSoft,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton("取消", onClick = { creating = false }, modifier = Modifier.weight(0.72f))
                    PrimaryButton(
                        "保存并选中",
                        enabled = draftName.isNotBlank() && relation.isNotBlank(),
                        onClick = {
                            val same = snapshot.contacts.any {
                                it.name.trim() == draftName.trim() &&
                                    it.relation.trim() == relation.trim() &&
                                    it.groupId == groupId
                            }
                            if (same) {
                                session.toast("已有该亲友，不能新建")
                                return@PrimaryButton
                            }
                            scope.launch {
                                runCatching {
                                    repo.saveContact(
                                        ContactDraft(
                                            name = draftName.trim(),
                                            relation = relation.trim(),
                                            groupId = groupId,
                                            phone = phone.trim(),
                                            note = note.trim(),
                                        ),
                                    )
                                }.onSuccess { onCreate(it) }
                                    .onFailure { session.toast(it.message ?: "保存失败") }
                            }
                        },
                        modifier = Modifier.weight(1.85f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BookPicker(
    snapshot: com.renqing.zhang.domain.Snapshot,
    selectedId: Long?,
    onNone: () -> Unit,
    onPick: (Long) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val books = snapshot.books.filter { !it.archived && (query.isBlank() || it.title.contains(query, true)) }
        .sortedByDescending { it.date }
    Column(Modifier.navigationBarsPadding()) {
        Text("选择礼簿", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp), fontSize = 18.sp, color = Ink)
        SearchField(query, { query = it }, "搜索礼簿", Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        SecondaryButton("不归入礼簿", onNone, Modifier.padding(horizontal = 16.dp).fillMaxWidth())
        LazyColumn(Modifier.height(320.dp)) {
            items(books, key = { it.id }) { book ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(book.id) }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(book.title, color = Ink)
                        Text("${snapshot.type(book.typeId)?.name.orEmpty()} · ${roleLabel(book.role)}", color = InkSoft, fontSize = 12.sp)
                    }
                    if (book.id == selectedId) Icon(Icons.Filled.Check, contentDescription = null, tint = Cinnabar)
                }
            }
        }
    }
}

private fun applyKey(current: String, key: String): String {
    val raw = current.replace(",", "")
    if (key == "⌫") return com.renqing.zhang.domain.formatInput(raw.dropLast(1).removeSuffix(".").let { if (it == "0") "" else it })
    if (key == ".") {
        if (raw.contains('.')) return current
        return com.renqing.zhang.domain.formatInput(if (raw.isBlank()) "0." else "$raw.")
    }
    val frac = raw.substringAfter('.', "")
    if (raw.contains('.') && frac.length >= 2) return current
    val next = if (raw == "0") key else raw + key
    if (parseYuanToFen(next) > 99_999_999_99L) return current
    return com.renqing.zhang.domain.formatInput(next)
}
