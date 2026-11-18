package com.renqing.zhang.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.renqing.zhang.domain.formatYuan
import com.renqing.zhang.ui.LedgerBody
import com.renqing.zhang.ui.LocalNav
import com.renqing.zhang.ui.switchTab
import com.renqing.zhang.ui.LocalSession
import com.renqing.zhang.ui.theme.Amber
import com.renqing.zhang.ui.theme.AmountFamily
import com.renqing.zhang.ui.theme.CardWhite
import com.renqing.zhang.ui.theme.Cinnabar
import com.renqing.zhang.ui.theme.Honey
import com.renqing.zhang.ui.theme.Ink
import com.renqing.zhang.ui.theme.InkSoft
import com.renqing.zhang.ui.theme.Pine
import com.renqing.zhang.ui.theme.Slate
import java.time.LocalDate

@Composable
fun StatsScreen() {
    val nav = LocalNav.current
    val session = LocalSession.current
    val thisYear = LocalDate.now().year
    var year by rememberSaveable { mutableIntStateOf(thisYear) }
    Column(Modifier.fillMaxSize().background(Honey).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            IconButton(onClick = { year -= 1 }) { Icon(Icons.Filled.ChevronLeft, contentDescription = "上一年", tint = Ink) }
            Text("${year}年", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Ink)
            IconButton(onClick = { if (year < thisYear) year += 1 }, enabled = year < thisYear) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "下一年", tint = if (year < thisYear) Ink else InkSoft.copy(alpha = 0.4f))
            }
        }
        LedgerBody(Modifier.weight(1f)) { snapshot, _ ->
            val stats = snapshot.stats(year)
            Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                Row(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardWhite)
                        .padding(vertical = 16.dp),
                ) {
                    Stat("送出", formatYuan(stats.sent), Cinnabar, Modifier.weight(1f))
                    Stat("收到", formatYuan(stats.received), Pine, Modifier.weight(1f))
                    Stat("净收", formatYuan(stats.net, withSign = true), if (stats.net >= 0) Cinnabar else Slate, Modifier.weight(1f))
                }
                Text("月份", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp), color = Ink)
                MonthChart(stats.months.map { it.amount }, stats.months.map { it.label })
                Text("事由", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Ink)
                Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                    if (stats.types.isEmpty()) {
                        Text("这一年还没有记录。", color = InkSoft, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                    }
                    stats.types.forEach { item ->
                        ShareRow(item.name, item.percent, typeColor(item.name)) {
                            item.typeId?.let {
                                session.bookTypeId = it
                                nav.switchTab("books")
                            }
                        }
                    }
                }
                Text("关系", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Ink)
                Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                    stats.groups.forEach { item ->
                        ShareRow(item.name, item.percent, groupColor(item.key)) {
                            session.scrollGroupKey = item.key
                            nav.switchTab("contacts")
                        }
                    }
                }
                Text("礼品按估价计入。口径和首页的年度数字相同。", color = InkSoft, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontFamily = AmountFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp)
        Spacer(Modifier.height(4.dp))
        Text(label, color = InkSoft, fontSize = 14.sp)
    }
}

@Composable
private fun MonthChart(amounts: List<Long>, labels: List<String>) {
    val max = amounts.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val gap = 6.dp.toPx()
            val barWidth = (size.width - gap * 11) / 12f
            amounts.forEachIndexed { index, amount ->
                val h = (amount.toFloat() / max) * (size.height - 8.dp.toPx())
                val left = index * (barWidth + gap)
                drawRoundRect(
                    color = if (amount == max && amount > 0) Cinnabar else Cinnabar.copy(alpha = 0.35f),
                    topLeft = Offset(left, size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(6f, 6f),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEach { label ->
                Text(
                    if (label.isBlank()) "" else label,
                    modifier = Modifier.weight(1f),
                    fontSize = 9.sp,
                    color = InkSoft,
                    textAlign = TextAlign.Center,
                    lineHeight = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun ShareRow(name: String, percent: Int, color: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name,
            color = Ink,
            fontSize = 14.sp,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.width(48.dp),
        )
        Box(
            Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(color.copy(alpha = 0.18f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth((percent.coerceIn(0, 100) / 100f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(color),
            )
        }
        Text(
            "$percent%",
            color = InkSoft,
            fontSize = 13.sp,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(start = 8.dp).widthIn(min = 48.dp),
            textAlign = TextAlign.End,
        )
    }
}

private fun typeColor(name: String): Color = when (name) {
    "满月" -> Cinnabar
    "婚礼" -> Slate
    "乔迁" -> Pine
    "寿宴" -> Amber
    "白事" -> Color(0xFF5C534C)
    "其他" -> Color(0xFFB7A89C)
    else -> InkSoft
}

private fun groupColor(key: String): Color = when (key) {
    "relative" -> Cinnabar
    "friend" -> Pine
    "family" -> Amber
    "colleague" -> Slate
    else -> Color(0xFF6B4C7A)
}
