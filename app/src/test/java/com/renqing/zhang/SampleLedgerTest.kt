package com.renqing.zhang

import com.renqing.zhang.data.BookEntity
import com.renqing.zhang.data.BookTypeEntity
import com.renqing.zhang.data.ContactEntity
import com.renqing.zhang.data.GroupEntity
import com.renqing.zhang.data.RecordEntity
import com.renqing.zhang.data.SampleData
import com.renqing.zhang.data.Settings
import com.renqing.zhang.domain.Snapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SampleLedgerTest {
    @Test
    fun prototypeNumbers() {
        SampleData.verify()
    }

    @Test
    fun homeRemindersAndStatsMatchTheSpec() {
        val snapshot = sampleSnapshot()
        val today = LocalDate.of(2026, 11, 17)
        val home = snapshot.home(Settings(), today)
        assertEquals(18_600_00L, home.received)
        assertEquals(12_800_00L, home.sent)
        assertEquals(5_800_00L, home.net)
        assertEquals(listOf("王建国", "周婷", "李淑芬"), home.reminders.map { it.name })
        assertTrue(home.reminders.first { it.name == "王建国" }.copy.contains("满月收到 1,000"))
        assertTrue(home.reminders.first { it.name == "王建国" }.copy.contains("其他送过 800"))
        assertTrue(home.reminders.first { it.name == "周婷" }.copy.contains("最近一次他随礼 1,000"))
        assertTrue(home.reminders.first { it.name == "周婷" }.copy.contains("他家满月是 11 月 28 日"))
        assertTrue(home.reminders.first { it.name == "李淑芬" }.copy.contains("此后还没有回过"))
        val books = snapshot.books(null, today)
        assertEquals("周婷家满月", books.first().title)
        assertEquals(100_000L, books.first().suggestion)
        val stats = snapshot.stats(2026)
        assertEquals(46, stats.types.first { it.name == "满月" }.percent)
        assertEquals(28, stats.types.first { it.name == "婚礼" }.percent)
        assertEquals(15, stats.types.first { it.name == "乔迁" }.percent)
        assertEquals(11, stats.types.first { it.name == "其他" }.percent)
        assertEquals(38, stats.groups.first { it.name == "亲戚" }.percent)
        assertEquals(0, snapshot.balance(snapshot.contacts.first { it.name == "陈明" }.id).net)
        assertEquals(-80_000L, snapshot.balance(snapshot.contacts.first { it.name == "张伟" }.id).net)
    }
}

private fun sampleSnapshot(): Snapshot {
    val groups = listOf(
        GroupEntity(1, "家人", "family", true, 0),
        GroupEntity(2, "亲戚", "relative", true, 1),
        GroupEntity(3, "朋友", "friend", true, 2),
        GroupEntity(4, "同事", "colleague", true, 3),
    )
    val groupId = groups.associate { it.key to it.id }
    val types = listOf(
        BookTypeEntity(1, "婚礼", "wedding", true, 0),
        BookTypeEntity(2, "满月", "fullmoon", true, 1),
        BookTypeEntity(3, "寿宴", "birthday", true, 2),
        BookTypeEntity(4, "白事", "funeral", true, 3),
        BookTypeEntity(5, "乔迁", "house", true, 4),
        BookTypeEntity(6, "其他", "other", true, 5),
    )
    val typeId = types.associate { it.key to it.id }
    val contacts = SampleData.people.mapIndexed { index, person ->
        ContactEntity(
            id = index + 1L,
            name = person.name,
            relation = person.relation,
            groupId = groupId.getValue(person.groupKey),
            phone = person.phone,
            note = "",
        )
    }
    val contactId = contacts.associate { it.name to it.id }
    val books = SampleData.books.mapIndexed { index, book ->
        BookEntity(
            id = index + 1L,
            title = book.title,
            typeId = typeId.getValue(book.typeKey),
            role = book.role,
            date = book.date,
            place = book.place,
            note = book.note,
            archived = false,
        )
    }
    val bookId = books.associate { it.title to it.id }
    val records = SampleData.rows.mapIndexed { index, row ->
        RecordEntity(
            id = index + 1L,
            contactId = contactId.getValue(row.contact),
            bookId = row.book?.let { bookId.getValue(it) },
            direction = row.direction,
            kind = row.kind,
            amount = row.yuan.toLong() * 100L,
            giftName = row.gift,
            date = row.date,
            note = row.note,
        )
    }
    return Snapshot(contacts, books, records, groups, types)
}
