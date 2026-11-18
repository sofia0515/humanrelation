package com.renqing.zhang.data

import com.renqing.zhang.domain.DIR_IN
import com.renqing.zhang.domain.DIR_OUT
import com.renqing.zhang.domain.GROUP_FAMILY
import com.renqing.zhang.domain.KIND_CASH
import com.renqing.zhang.domain.KIND_GIFT
import com.renqing.zhang.domain.ROLE_GUEST
import com.renqing.zhang.domain.ROLE_HOST

object SampleData {
    data class Person(
        val name: String,
        val relation: String,
        val groupKey: String,
        val phone: String = "",
    )

    data class BookSeed(
        val title: String,
        val typeKey: String,
        val role: String,
        val date: String,
        val place: String = "",
        val note: String = "",
    )

    data class Row(
        val book: String?,
        val contact: String,
        val direction: String,
        val yuan: Int,
        val date: String,
        val kind: String = KIND_CASH,
        val gift: String = "",
        val note: String = "",
    )

    val people = listOf(
        Person("林国栋", "父亲", "family"),
        Person("林秀兰", "母亲", "family"),
        Person("林建国", "爷爷", "family"),
        Person("林芳", "姑姑", "family"),
        Person("王建国", "舅舅", "relative", "13800002219"),
        Person("李淑芬", "阿姨", "relative"),
        Person("王小丽", "表姐", "relative"),
        Person("李强", "表哥", "relative"),
        Person("何芳", "堂妹", "relative"),
        Person("唐磊", "表弟", "relative"),
        Person("林远", "表叔", "relative"),
        Person("周婷", "朋友", "friend"),
        Person("张伟", "朋友", "friend"),
        Person("周敏", "朋友", "friend"),
        Person("孙悦", "朋友", "friend"),
        Person("黄丽", "朋友", "friend"),
        Person("马超", "朋友", "friend"),
        Person("周衡", "朋友", "friend"),
        Person("陈明", "同事", "colleague"),
        Person("赵敏", "同事", "colleague"),
        Person("吴刚", "同事", "colleague"),
        Person("郑凯", "同事", "colleague"),
        Person("吴迪", "同事", "colleague"),
    )

    val books = listOf(
        BookSeed("周婷家满月", "fullmoon", ROLE_GUEST, "2026-11-28", note = "可参考她去年随礼 ¥1,000"),
        BookSeed("陈明乔迁", "house", ROLE_GUEST, "2026-11-16"),
        BookSeed("张伟婚礼", "wedding", ROLE_GUEST, "2026-05-18"),
        BookSeed("林小满百日宴", "fullmoon", ROLE_HOST, "2026-03-02", "城南酒楼"),
        BookSeed("林晚婚礼", "wedding", ROLE_HOST, "2026-03-01", "城南酒楼"),
        BookSeed("家族满月", "fullmoon", ROLE_GUEST, "2026-03-01"),
        BookSeed("孙悦婚礼", "wedding", ROLE_GUEST, "2026-03-01"),
        BookSeed("林家乔迁", "house", ROLE_HOST, "2026-03-01"),
        BookSeed("远房乔迁", "house", ROLE_GUEST, "2026-03-01"),
        BookSeed("春节家宴", "other", ROLE_HOST, "2026-01-28", "家里"),
        BookSeed("同学聚会", "other", ROLE_GUEST, "2026-01-05"),
        BookSeed("去年乔迁", "house", ROLE_HOST, "2025-10-18"),
        BookSeed("王建国升学", "other", ROLE_GUEST, "2025-09-01"),
    )

    val rows = listOf(
        Row("林小满百日宴", "林国栋", DIR_IN, 2000, "2026-03-02"),
        Row("林小满百日宴", "王建国", DIR_IN, 1000, "2026-03-02"),
        Row("林小满百日宴", "李淑芬", DIR_IN, 600, "2026-03-02"),
        Row("林小满百日宴", "陈明", DIR_IN, 500, "2026-03-02"),
        Row("林小满百日宴", "赵敏", DIR_IN, 300, "2026-03-02", KIND_GIFT, "婴儿衣服"),
        Row("林小满百日宴", "林秀兰", DIR_IN, 1500, "2026-03-02"),
        Row("林小满百日宴", "林建国", DIR_IN, 1200, "2026-03-02"),
        Row("林小满百日宴", "林芳", DIR_IN, 800, "2026-03-02"),
        Row("林小满百日宴", "王小丽", DIR_IN, 200, "2026-03-02"),
        Row("林小满百日宴", "李强", DIR_IN, 150, "2026-03-02"),
        Row("林小满百日宴", "周敏", DIR_IN, 100, "2026-03-02"),
        Row("林小满百日宴", "孙悦", DIR_IN, 80, "2026-03-02"),
        Row("林小满百日宴", "吴刚", DIR_IN, 70, "2026-03-02"),
        Row("林小满百日宴", "郑凯", DIR_IN, 50, "2026-03-02"),
        Row("林小满百日宴", "黄丽", DIR_IN, 20, "2026-03-02"),
        Row("林小满百日宴", "马超", DIR_IN, 10, "2026-03-02"),
        Row("林小满百日宴", "何芳", DIR_IN, 10, "2026-03-02"),
        Row("林小满百日宴", "唐磊", DIR_IN, 10, "2026-03-02"),
        Row("张伟婚礼", "张伟", DIR_OUT, 800, "2026-05-18"),
        Row("陈明乔迁", "陈明", DIR_OUT, 500, "2026-11-16"),
        Row("林晚婚礼", "林远", DIR_IN, 2500, "2026-03-01"),
        Row("林晚婚礼", "周衡", DIR_IN, 2200, "2026-03-01"),
        Row("林晚婚礼", "吴迪", DIR_IN, 900, "2026-03-01"),
        Row("孙悦婚礼", "林远", DIR_OUT, 1500, "2026-03-01"),
        Row("孙悦婚礼", "周衡", DIR_OUT, 892, "2026-03-01"),
        Row("家族满月", "林远", DIR_OUT, 2200, "2026-03-01"),
        Row("家族满月", "周衡", DIR_OUT, 1700, "2026-03-01"),
        Row("家族满月", "吴迪", DIR_OUT, 536, "2026-03-01"),
        Row("家族满月", "林芳", DIR_OUT, 1408, "2026-03-01"),
        Row("林家乔迁", "林远", DIR_IN, 1600, "2026-03-01"),
        Row("林家乔迁", "周衡", DIR_IN, 1000, "2026-03-01"),
        Row("林家乔迁", "吴迪", DIR_IN, 500, "2026-03-01"),
        Row("远房乔迁", "林远", DIR_OUT, 800, "2026-03-01"),
        Row("远房乔迁", "周衡", DIR_OUT, 310, "2026-03-01"),
        Row("春节家宴", "林远", DIR_IN, 800, "2026-01-28"),
        Row("春节家宴", "周衡", DIR_IN, 500, "2026-01-28"),
        Row("同学聚会", "林远", DIR_OUT, 562, "2026-01-05"),
        Row("同学聚会", "周衡", DIR_OUT, 866, "2026-01-05"),
        Row("同学聚会", "吴迪", DIR_OUT, 726, "2026-01-05"),
        Row(null, "王建国", DIR_IN, 1000, "2025-01-28", note = "春节"),
        Row("王建国升学", "王建国", DIR_OUT, 800, "2025-09-01"),
        Row("去年乔迁", "周婷", DIR_IN, 1000, "2025-10-18"),
    )

    fun verify() {
        val y2026 = rows.filter { it.date.startsWith("2026") }
        val incoming = y2026.filter { it.direction == DIR_IN }.sumOf { it.yuan }
        val outgoing = y2026.filter { it.direction == DIR_OUT }.sumOf { it.yuan }
        check(incoming == 18600) { "收到 $incoming" }
        check(outgoing == 12800) { "送出 $outgoing" }
        val banquet = y2026.filter { it.book == "林小满百日宴" }
        check(banquet.size == 18 && banquet.sumOf { it.yuan } == 8600)
        val typeOf = books.associate { it.title to it.typeKey }
        val typeYuan = mutableMapOf<String, Int>()
        y2026.forEach { row ->
            val key = typeOf.getValue(row.book!!)
            typeYuan[key] = typeYuan.getOrDefault(key, 0) + row.yuan
        }
        val flow = incoming + outgoing
        check(typeYuan.getValue("fullmoon") * 100 / flow == 46)
        check(typeYuan.getValue("wedding") * 100 / flow == 28)
        check(typeYuan.getValue("house") * 100 / flow == 15)
        check(typeYuan.getValue("other") * 100 / flow == 11)
        val groupOf = people.associate { it.name to it.groupKey }
        val groupYuan = mutableMapOf<String, Int>()
        y2026.forEach { row ->
            val key = groupOf.getValue(row.contact)
            groupYuan[key] = groupYuan.getOrDefault(key, 0) + row.yuan
        }
        check(groupYuan.getValue("relative") * 100 / flow == 38)
        check(groupYuan.getValue("friend") * 100 / flow == 27)
        check(groupYuan.getValue(GROUP_FAMILY) * 100 / flow == 22)
        check(groupYuan.getValue("colleague") * 100 / flow == 13)
        fun net(name: String) = rows.filter { it.contact == name }.sumOf {
            if (it.direction == DIR_IN) it.yuan else -it.yuan
        }
        check(net("王建国") == 1200)
        check(net("周婷") == 1000)
        check(net("李淑芬") == 600)
        check(net("张伟") == -800)
        check(net("陈明") == 0)
        check(net("赵敏") == 300)
        val reminded = people.filter { person ->
            person.groupKey != GROUP_FAMILY && net(person.name) >= 500
        }.map { it.name }.toSet()
        check(reminded == setOf("王建国", "周婷", "李淑芬")) { reminded.toString() }
    }
}
