package com.renqing.zhang.domain

import android.icu.util.ChineseCalendar
import android.icu.util.GregorianCalendar
import android.icu.util.TimeZone
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

const val DIR_IN = "in"
const val DIR_OUT = "out"
const val KIND_CASH = "cash"
const val KIND_GIFT = "gift"
const val ROLE_HOST = "host"
const val ROLE_GUEST = "guest"
const val GROUP_FAMILY = "family"

fun formatYuan(fen: Long, withSign: Boolean = false, withSymbol: Boolean = false): String {
    val negative = fen < 0
    val abs = kotlin.math.abs(fen)
    val yuan = abs / 100
    val cent = (abs % 100).toInt()
    val body = if (cent == 0) {
        String.format(Locale.US, "%,d", yuan)
    } else {
        String.format(Locale.US, "%,d.%02d", yuan, cent)
    }
    val sign = when {
        negative -> "-"
        withSign && fen > 0 -> "+"
        else -> ""
    }
    return if (withSymbol) "$sign¥$body" else "$sign$body"
}

fun maskPhone(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    if (digits.length < 7) return phone.ifBlank { "未留电话" }
    return digits.take(3) + "****" + digits.takeLast(4)
}

fun parseYuanToFen(text: String): Long {
    val clean = text.trim().removePrefix("+").removePrefix("¥").replace(",", "")
    if (clean.isBlank() || clean == ".") return 0
    val parts = clean.split('.')
    val yuan = parts[0].toLongOrNull() ?: return 0
    val fracRaw = if (parts.size > 1) parts[1].take(2) else ""
    val frac = fracRaw.padEnd(2, '0').toLongOrNull() ?: 0
    return yuan * 100 + frac
}

fun fenToInput(fen: Long): String {
    val yuan = fen / 100
    val cent = (fen % 100).toInt()
    val raw = if (cent == 0) yuan.toString() else "$yuan.${cent.toString().padStart(2, '0').trimEnd('0')}"
    return formatInput(raw)
}

fun formatInput(text: String): String {
    if (text.isBlank()) return ""
    val raw = text
    val dot = raw.indexOf('.')
    val intPart = (if (dot >= 0) raw.substring(0, dot) else raw).ifBlank { "0" }
    val yuan = intPart.toLongOrNull() ?: return raw
    val grouped = String.format(Locale.US, "%,d", yuan)
    return if (dot >= 0) grouped + raw.substring(dot) else grouped
}

private val iso: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun parseDate(value: String): LocalDate = LocalDate.parse(value, iso)

fun formatIso(date: LocalDate): String = date.format(iso)

fun formatCn(date: LocalDate): String = "${date.year}年${date.monthValue}月${date.dayOfMonth}日"

fun formatDot(date: LocalDate): String =
    "%d.%02d.%02d".format(date.year, date.monthValue, date.dayOfMonth)

fun formatListDate(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "今天"
    today.minusDays(1) -> "昨天"
    else -> "%02d-%02d".format(date.monthValue, date.dayOfMonth)
}

fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun Long.utcToLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

fun shortOccasion(title: String, note: String, typeName: String): String {
    val src = title.ifBlank { note }
    return when {
        src.contains("百日") -> "百日"
        src.contains("升学") -> "升学宴"
        src.contains("婚礼") -> "婚礼"
        src.contains("满月") -> "满月"
        src.contains("乔迁") -> "乔迁"
        src.contains("寿") -> "寿宴"
        src.contains("白事") || src.contains("丧") -> "白事"
        src.contains("春节") -> "春节"
        src.contains("聚会") -> "聚会"
        title.isBlank() && note.isNotBlank() -> note
        typeName.isNotBlank() -> typeName
        else -> "随礼"
    }
}

fun directionLabel(direction: String): String = if (direction == DIR_IN) "收礼" else "送礼"

fun holidayName(date: LocalDate): String? {
    val (month, day, leap) = lunarMonthDay(date)
    if (!leap) {
        when (month to day) {
            1 to 1 -> return "春节"
            1 to 15 -> return "元宵节"
            5 to 5 -> return "端午节"
            7 to 7 -> return "七夕"
            7 to 15 -> return "中元节"
            8 to 15 -> return "中秋节"
            9 to 9 -> return "重阳节"
            12 to 8 -> return "腊八节"
        }
    }
    val next = lunarMonthDay(date.plusDays(1))
    if (!next.third && next.first == 1 && next.second == 1) return "除夕"
    if (date.monthValue == 1 && date.dayOfMonth == 1) return "元旦"
    if (date.monthValue == 5 && date.dayOfMonth == 1) return "劳动节"
    if (date.monthValue == 10 && date.dayOfMonth == 1) return "国庆节"
    if (date == qingming(date.year)) return "清明节"
    return null
}

private fun lunarMonthDay(date: LocalDate): Triple<Int, Int, Boolean> {
    val zone = TimeZone.getTimeZone("UTC")
    val gregorian = GregorianCalendar(zone)
    gregorian.set(date.year, date.monthValue - 1, date.dayOfMonth, 12, 0, 0)
    gregorian.set(GregorianCalendar.MILLISECOND, 0)
    val lunar = ChineseCalendar(zone)
    lunar.timeInMillis = gregorian.timeInMillis
    return Triple(
        lunar.get(ChineseCalendar.MONTH) + 1,
        lunar.get(ChineseCalendar.DAY_OF_MONTH),
        lunar.get(ChineseCalendar.IS_LEAP_MONTH) == 1,
    )
}

private fun qingming(year: Int): LocalDate {
    val y = year % 100
    val day = (y * 0.2422 + 4.81).toInt() - y / 4
    return LocalDate.of(year, 4, day.coerceIn(4, 6))
}

fun roleLabel(role: String): String = if (role == ROLE_HOST) "主家" else "随礼"

fun kindLabel(kind: String, giftName: String): String =
    if (kind == KIND_GIFT) giftName.ifBlank { "礼品" } else "现金"
