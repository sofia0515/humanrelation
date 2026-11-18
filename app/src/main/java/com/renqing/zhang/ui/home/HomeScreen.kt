package com.renqing.zhang.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renqing.zhang.data.Settings
import com.renqing.zhang.domain.DIR_IN
import com.renqing.zhang.domain.RecordRowUi
import com.renqing.zhang.domain.Snapshot
import com.renqing.zhang.domain.formatYuan
import com.renqing.zhang.ui.LedgerBody
import com.renqing.zhang.ui.LocalNav
import com.renqing.zhang.ui.switchTab
import com.renqing.zhang.ui.components.Avatar
import com.renqing.zhang.ui.components.SectionLabel
import com.renqing.zhang.ui.components.SwipeDelete
import com.renqing.zhang.ui.record.rememberDeleteRequest
import com.renqing.zhang.ui.theme.AmountFamily
import com.renqing.zhang.ui.theme.CardWhite
import com.renqing.zhang.ui.theme.Cinnabar
import com.renqing.zhang.ui.theme.Honey
import com.renqing.zhang.ui.theme.Line
import com.renqing.zhang.ui.theme.Ink
import com.renqing.zhang.ui.theme.InkSoft
import com.renqing.zhang.ui.theme.Pine
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun HomeScreen() {
    val nav = LocalNav.current
    LedgerBody { snapshot, settings ->
        HomeContent(snapshot, settings, today = LocalDate.now(), nav = nav)
    }
}

@Composable
private fun HomeContent(
    snapshot: Snapshot,
    settings: Settings,
    today: LocalDate,
    nav: androidx.navigation.NavHostController,
) {
    val home = snapshot.home(settings, today)
    val scroll = rememberScrollState()
    var reminderTop by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val delete = rememberDeleteRequest()
    Column(
        Modifier
            .fillMaxSize()
            .background(Honey),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFE15A4C), Cinnabar, Color(0xFFB13327)))),
        ) {
            Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("人情往来", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                        Text(settings.userName.ifBlank { "未命名" }, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                    }
                    HeaderIcon(Icons.Filled.Search, "搜索") { nav.navigate("search") }
                    HeaderIcon(Icons.Filled.Notifications, "待回") {
                        scope.launch { scroll.animateScrollTo(reminderTop) }
                    }
                    Box(
                        Modifier
                            .padding(start = 6.dp)
                            .size(40.dp)
                            .clickable { nav.switchTab("books") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.matchParentSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val whiteW = 1.6.dp.toPx()
                            val innerW = 2.2.dp.toPx()
                            val outerR = size.minDimension / 2f - whiteW / 2f
                            drawCircle(
                                color = Color.White,
                                radius = outerR,
                                center = center,
                                style = Stroke(width = whiteW),
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.34f),
                                radius = outerR - whiteW / 2f - innerW / 2f,
                                center = center,
                                style = Stroke(width = innerW),
                            )
                        }
                        Text("礼", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
                Text(
                    if (!home.remindOn) "回礼提醒已关闭"
                    else if (home.reminders.isEmpty()) "${home.month}月没有要回的礼"
                    else "${home.month}月还有 ${home.reminders.size} 位亲友待回礼",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Column(Modifier.weight(1f).verticalScroll(scroll)) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatPill("今年送出", formatYuan(home.sent), Cinnabar, Modifier.weight(1f))
            StatPill("今年收到", formatYuan(home.received), Pine, Modifier.weight(1f))
            StatPill("净收", formatYuan(home.net, withSign = true), if (home.net >= 0) Pine else Cinnabar, Modifier.weight(1f))
        }
        Row(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ActionButton("记一笔", filled = true, modifier = Modifier.weight(1.77f)) {
                nav.navigate("recordEdit?contactId=-1&bookId=-1&direction=&recordId=-1")
            }
            ActionButton("新建礼簿", filled = false, modifier = Modifier.weight(1f)) {
                nav.navigate("bookEdit?id=-1")
            }
        }
        SectionLabel("最近", "全部") { nav.navigate("records") }
        if (home.recent.isEmpty()) {
            Text("还没有记录。先记一笔，或先建一本礼簿。", color = InkSoft, modifier = Modifier.padding(horizontal = 16.dp), fontSize = 14.sp)
        } else {
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardWhite),
            ) {
                home.recent.forEachIndexed { index, row ->
                    if (row.bookArchived) {
                        RecentRow(row) { nav.navigate("record/${row.id}") }
                    } else {
                        SwipeDelete(onDelete = { delete.ask(snapshot, row.id) }) {
                            RecentRow(row) { nav.navigate("record/${row.id}") }
                        }
                    }
                    if (index < home.recent.lastIndex) {
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
        Row(
            Modifier
                .onGloballyPositioned { reminderTop = it.positionInParent().y.toInt() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("待回礼", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Ink, modifier = Modifier.weight(1f))
            Text("阈值 ${formatYuan(home.threshold, withSymbol = true)}", color = InkSoft, fontSize = 13.sp)
        }
        if (!home.remindOn) {
            Text("打开「我的」里的回礼提醒后，这里会列出该回的人。", color = InkSoft, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))
        } else if (home.reminders.isEmpty()) {
            Text("没有超过阈值的待回礼。家人默认不出现在这里。", color = InkSoft, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))
        } else {
            home.reminders.chunked(2).forEach { pair ->
                EqualHeightRow(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    pair.forEach { item ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardWhite)
                                .clickable { nav.navigate("contact/${item.contactId}") }
                                .padding(14.dp),
                        ) {
                            Text(
                                "待回 ${formatYuan(item.net, withSymbol = true)}",
                                color = com.renqing.zhang.ui.theme.Amber,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(com.renqing.zhang.ui.theme.AmberSoft)
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("${item.name} · ${item.relation}", fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 14.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(item.copy, color = InkSoft, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        }
    }
    delete.Dialog()
}

@Composable
private fun EqualHeightRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier.fillMaxWidth()) { measurables, constraints ->
        val gap = 10.dp.roundToPx()
        val count = measurables.size.coerceAtLeast(1)
        val width = if (count == 1) constraints.maxWidth else (constraints.maxWidth - gap * (count - 1)) / count
        val height = measurables.maxOf { it.maxIntrinsicHeight(width) }
        val fixed = constraints.copy(minWidth = width, maxWidth = width, minHeight = height, maxHeight = height)
        val placed = measurables.map { it.measure(fixed) }
        layout(constraints.maxWidth, height) {
            var x = 0
            placed.forEach { item ->
                item.placeRelative(x, 0)
                x += width + gap
            }
        }
    }
}

@Composable
private fun HeaderIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(4.dp)
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Color.White)
    }
}

@Composable
private fun StatPill(label: String, value: String, valueColor: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = InkSoft, fontSize = 12.sp, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(value, color = valueColor, fontFamily = AmountFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1)
    }
}

@Composable
private fun ActionButton(text: String, filled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (filled) Cinnabar else CardWhite)
            .then(if (filled) Modifier else Modifier.border(1.dp, com.renqing.zhang.ui.theme.Line, RoundedCornerShape(22.dp)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (filled) Color.White else Ink, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RecentRow(row: RecordRowUi, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(row.letter, row.title)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(row.title, fontWeight = FontWeight.Medium, color = Ink)
            Text(row.subtitle, color = InkSoft, fontSize = 12.sp)
        }
        Text(
            formatYuan(row.amount, withSign = true),
            color = if (row.direction == DIR_IN) Pine else Cinnabar,
            fontFamily = AmountFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
    }
}
