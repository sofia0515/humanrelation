package com.renqing.zhang.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renqing.zhang.domain.DIR_IN
import com.renqing.zhang.domain.formatDot
import com.renqing.zhang.domain.formatYuan
import com.renqing.zhang.domain.toneLabel
import com.renqing.zhang.ui.LedgerBody
import com.renqing.zhang.ui.LocalNav
import com.renqing.zhang.ui.LocalRepo
import com.renqing.zhang.ui.components.EmptyState
import com.renqing.zhang.ui.theme.CardWhite
import com.renqing.zhang.ui.theme.Cinnabar
import com.renqing.zhang.ui.theme.Honey
import com.renqing.zhang.ui.theme.Ink
import com.renqing.zhang.ui.theme.InkSoft
import com.renqing.zhang.ui.theme.Line
import com.renqing.zhang.ui.theme.Pine
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen() {
    val nav = LocalNav.current
    val repo = LocalRepo.current
    var raw by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    val history by repo.searches().collectAsState(initial = emptyList())
    LaunchedEffect(raw) {
        if (raw.isBlank()) {
            query = ""
        } else {
            delay(300)
            query = raw.trim()
            repo.rememberSearch(query)
        }
    }
    Column(Modifier.fillMaxSize().background(Honey).statusBarsPadding().navigationBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Ink)
            }
            androidx.compose.foundation.text.BasicTextField(
                value = raw,
                onValueChange = { raw = it },
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(color = Ink, fontSize = 16.sp),
                singleLine = true,
                decorationBox = { inner ->
                    if (raw.isEmpty()) Text("搜索亲友、礼簿、记录", color = com.renqing.zhang.ui.theme.Hint)
                    inner()
                },
            )
            if (raw.isNotEmpty()) {
                IconButton(onClick = { raw = "" }) { Icon(Icons.Filled.Close, contentDescription = "清空", tint = InkSoft) }
            }
        }
        if (query.isBlank()) {
            Text("最近搜索", color = InkSoft, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            if (history.isEmpty()) {
                Text("输入后会在本机账本里找。", color = InkSoft, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))
            } else {
                FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    history.take(8).forEach { item ->
                        Text(
                            item,
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(CardWhite)
                                .clickable { raw = item }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Ink,
                        )
                    }
                }
            }
            return@Column
        }
        LedgerBody(Modifier.weight(1f)) { snapshot, _ ->
            val result = snapshot.search(query)
            val nothing = result.contacts.isEmpty() && result.records.isEmpty() && result.books.isEmpty()
            if (nothing) {
                EmptyState(title = "没有找到", body = "没有找到相关亲友、礼簿或记录。")
            } else {
                LazyColumn {
                    item { Section("亲友 · ${result.contacts.size}") }
                    itemsIndexed(result.contacts, key = { _, person -> "c${person.id}" }) { index, person ->
                        Column(Modifier.fillMaxWidth().clickable { nav.navigate("contact/${person.id}") }) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                Text(person.name, color = Ink, fontWeight = FontWeight.Medium)
                                Text("${person.relation} · ${toneLabel(person.net, person.hasRecords)}", color = InkSoft, fontSize = 12.sp)
                            }
                            if (index < result.contacts.lastIndex) {
                                Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(Line))
                            }
                        }
                    }
                    item { Section("记录 · ${result.records.size}") }
                    itemsIndexed(result.records, key = { _, row -> "r${row.id}" }) { index, row ->
                        Column(Modifier.fillMaxWidth().clickable { nav.navigate("record/${row.id}") }) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                Text(
                                    "${if (row.direction == DIR_IN) "收礼" else "送礼"} ${formatYuan(kotlin.math.abs(row.amount), withSymbol = true)}",
                                    color = if (row.direction == DIR_IN) Pine else Cinnabar,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text("${row.title} · ${row.detailSubtitle}", color = InkSoft, fontSize = 12.sp)
                            }
                            if (index < result.records.lastIndex) {
                                Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(Line))
                            }
                        }
                    }
                    item { Section(if (result.books.isEmpty()) "礼簿 · 无匹配" else "礼簿 · ${result.books.size}") }
                    if (result.books.isEmpty()) {
                        item {
                            Text(
                                "没有标题里带「$query」的礼簿。记录仍可通过亲友找到。",
                                color = InkSoft,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    } else {
                        itemsIndexed(result.books, key = { _, book -> "b${book.id}" }) { index, book ->
                            Column(Modifier.fillMaxWidth().clickable { nav.navigate("book/${book.id}") }) {
                                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                    Text(book.title, color = Ink, fontWeight = FontWeight.Medium)
                                    Text("${book.typeName} · ${formatDot(book.date)}", color = InkSoft, fontSize = 12.sp)
                                }
                                if (index < result.books.lastIndex) {
                                    Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(Line))
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
private fun Section(title: String) {
    Text(title, color = InkSoft, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
}
