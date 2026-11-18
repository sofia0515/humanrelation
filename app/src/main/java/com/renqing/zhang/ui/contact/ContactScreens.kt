package com.renqing.zhang.ui.contact

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renqing.zhang.data.ContactDraft
import com.renqing.zhang.domain.DIR_IN
import com.renqing.zhang.domain.formatYuan
import com.renqing.zhang.domain.maskPhone
import com.renqing.zhang.ui.LedgerBody
import com.renqing.zhang.ui.LocalNav
import com.renqing.zhang.ui.LocalRepo
import com.renqing.zhang.ui.LocalSession
import com.renqing.zhang.ui.components.Avatar
import com.renqing.zhang.ui.components.ChoiceChip
import com.renqing.zhang.ui.components.EmptyState
import com.renqing.zhang.ui.components.PageHeader
import com.renqing.zhang.ui.components.InlineField
import com.renqing.zhang.ui.components.PrimaryButton
import com.renqing.zhang.ui.components.SwipeDelete
import com.renqing.zhang.ui.components.ToneChip
import com.renqing.zhang.ui.record.rememberDeleteRequest
import com.renqing.zhang.ui.theme.Amber
import com.renqing.zhang.ui.theme.AmountFamily
import com.renqing.zhang.ui.theme.CardWhite
import com.renqing.zhang.ui.theme.Line
import com.renqing.zhang.ui.theme.Cinnabar
import com.renqing.zhang.ui.theme.Honey
import com.renqing.zhang.ui.theme.Ink
import com.renqing.zhang.ui.theme.InkSoft
import com.renqing.zhang.ui.theme.Pine
import com.renqing.zhang.ui.theme.Slate
import kotlinx.coroutines.launch

@Composable
fun ContactListScreen() {
    val nav = LocalNav.current
    val session = LocalSession.current
    val listState = rememberLazyListState()
    Column(Modifier.fillMaxSize().background(Honey).statusBarsPadding()) {
        PageHeader("亲友") {
            IconButton(onClick = { nav.navigate("contactEdit?id=-1") }) {
                Icon(Icons.Filled.PersonAdd, contentDescription = "新建亲友", tint = Ink)
            }
            IconButton(onClick = { nav.navigate("search") }) {
                Icon(Icons.Filled.Search, contentDescription = "搜索", tint = Ink)
            }
        }
        LedgerBody(Modifier.weight(1f)) { snapshot, settings ->
            val sections = snapshot.contactSections(settings)
            val target = session.scrollGroupKey
            LaunchedEffect(target, sections) {
                if (target == null) return@LaunchedEffect
                var index = 0
                var found = -1
                sections.forEach { section ->
                    if (section.key == target && found < 0) found = index
                    index += 1 + section.people.size
                }
                if (found >= 0) listState.animateScrollToItem(found)
                session.scrollGroupKey = null
            }
            if (snapshot.contacts.isEmpty()) {
                EmptyState(
                    title = "先记下一位亲友",
                    body = "同名的人不会自动并成一个。按关系分开记。",
                    primary = "新建亲友",
                    onPrimary = { nav.navigate("contactEdit?id=-1") },
                )
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    sections.forEach { section ->
                        item(key = "g-${section.key}") {
                            Text(
                                if (section.hint.isNullOrBlank()) section.name else "${section.name} · ${section.hint}",
                                color = InkSoft,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        itemsIndexed(section.people, key = { _, person -> person.id }) { index, person ->
                            Column {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { nav.navigate("contact/${person.id}") }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Avatar(person.letter, person.name)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(person.name, fontWeight = FontWeight.Medium, color = Ink)
                                        Text(person.relation, color = InkSoft, fontSize = 12.sp)
                                    }
                                    if (person.hideBalance) {
                                        Text(
                                            "家里",
                                            color = Amber,
                                            fontSize = 13.sp,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(99.dp))
                                                .background(Color(0xFFF6EFE6))
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                        )
                                    } else {
                                        ToneChip(person.net, person.hasRecords)
                                    }
                                }
                                if (index < section.people.lastIndex) {
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
            }
        }
    }
}

@Composable
fun ContactDetailScreen(contactId: Long) {
    val nav = LocalNav.current
    val context = LocalContext.current
    val delete = rememberDeleteRequest()
    Column(Modifier.fillMaxSize().background(Honey).statusBarsPadding().navigationBarsPadding()) {
        LedgerBody(Modifier.weight(1f)) { snapshot, _ ->
            val detail = snapshot.contactDetail(contactId, java.time.LocalDate.now())
            Column(Modifier.fillMaxSize()) {
            PageHeader("亲友", onBack = { nav.popBackStack() }) {
                IconButton(onClick = { nav.navigate("contactEdit?id=$contactId") }) {
                    Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = Ink)
                }
            }
            if (detail == null) {
                Text("这位亲友已经不在了", modifier = Modifier.padding(16.dp), color = InkSoft)
            } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(detail.name.take(1), detail.name, size = 48)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(detail.name, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Ink)
                            Text(
                                "${detail.relation} · ${detail.groupName}" +
                                    if (detail.phone.isNotBlank()) " · ${maskPhone(detail.phone)}" else "",
                                color = InkSoft,
                                fontSize = 13.sp,
                                modifier = if (detail.phone.isBlank()) Modifier else Modifier.clickable {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${detail.phone}")))
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    val color = when {
                        detail.net > 0 -> Amber
                        detail.net < 0 -> Slate
                        else -> Pine
                    }
                    val balanceLabel = when {
                        detail.net > 0 -> "累计待回"
                        detail.net < 0 -> "累计在外"
                        detail.rows.isEmpty() -> "还没有往来"
                        else -> "已经持平"
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardWhite)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(balanceLabel, color = InkSoft, fontSize = 12.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                formatYuan(kotlin.math.abs(detail.net), withSymbol = true),
                                color = color,
                                fontFamily = AmountFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 32.sp,
                            )
                        }
                        if (detail.net > 0) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Cinnabar)
                                    .clickable {
                                        nav.navigate("recordEdit?contactId=$contactId&bookId=-1&direction=out&recordId=-1")
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text("去回礼", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }
                }
                Text("往来", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = Ink)
                if (detail.rows.isEmpty()) {
                    Text("还没有往来。记一笔之后，差额从这里长出来。", color = InkSoft, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))
                }
                detail.rows.forEachIndexed { index, row ->
                    val line: @Composable () -> Unit = {
                        Column {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { nav.navigate("record/${row.id}") }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val received = row.direction == DIR_IN
                                Box(
                                    Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(if (received) Color(0xFFE7F5EE) else Color(0xFFF8E8E4)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Box(
                                        Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (received) Pine else Color(0xFFC0392B)),
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(row.detailTitle, color = Ink, fontWeight = FontWeight.Medium)
                                    Text(row.detailSubtitle, color = InkSoft, fontSize = 12.sp)
                                }
                                Text(
                                    formatYuan(row.amount, withSign = true),
                                    color = if (row.direction == DIR_IN) Pine else Cinnabar,
                                    fontFamily = AmountFamily,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            if (index < detail.rows.lastIndex) {
                                Box(
                                    Modifier
                                        .padding(start = 36.dp, end = 16.dp)
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Line),
                                )
                            }
                        }
                    }
                    if (row.bookArchived) line() else SwipeDelete(onDelete = { delete.ask(snapshot, row.id) }, surface = Honey) { line() }
                }
                Spacer(Modifier.height(24.dp))
            }
            }
            }
            delete.Dialog()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContactEditScreen(contactId: Long) {
    val nav = LocalNav.current
    val repo = LocalRepo.current
    val session = LocalSession.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }
    var groupId by remember { mutableStateOf(0L) }
    var phone by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Honey).statusBarsPadding().navigationBarsPadding()) {
        PageHeader(if (contactId > 0) "编辑亲友" else "新建亲友", onBack = { nav.popBackStack() })
        LedgerBody(Modifier.weight(1f)) { snapshot, _ ->
            LaunchedEffect(contactId) {
                snapshot.contact(contactId)?.let { person ->
                    name = person.name
                    relation = person.relation
                    groupId = person.groupId
                    phone = person.phone
                    note = person.note
                }
                if (groupId == 0L) {
                    groupId = snapshot.groups.firstOrNull { it.key == "relative" }?.id
                        ?: snapshot.groups.firstOrNull()?.id ?: 0L
                }
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                InlineField("姓名", name, { name = it }, modifier = Modifier.padding(horizontal = 16.dp))
                InlineField("关系", relation, { relation = it }, placeholder = "如舅舅、同事", modifier = Modifier.padding(horizontal = 16.dp))
                Text("分组", color = InkSoft, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
                FlowRow(
                    Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    snapshot.groups.forEach { group ->
                        ChoiceChip(group.name, groupId == group.id) { groupId = group.id }
                    }
                }
                InlineField("手机", phone, { phone = it }, placeholder = "选填", modifier = Modifier.padding(horizontal = 16.dp))
                InlineField("备注", note, { note = it }, placeholder = "选填", modifier = Modifier.padding(horizontal = 16.dp))
                Text(
                    "改资料不会改已经记下的金额。同名的人各自算各自的账。",
                    color = InkSoft,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                )
                Spacer(Modifier.height(12.dp))
                PrimaryButton(
                    text = "保存",
                    enabled = name.isNotBlank() && relation.isNotBlank(),
                    onClick = {
                        scope.launch {
                            runCatching {
                                repo.saveContact(ContactDraft(contactId.coerceAtLeast(0), name, relation, groupId, phone, note))
                            }.onSuccess { nav.popBackStack() }
                                .onFailure { session.toast(it.message ?: "保存失败") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }
        }
    }
}
