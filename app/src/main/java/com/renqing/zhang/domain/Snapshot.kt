package com.renqing.zhang.domain

import com.renqing.zhang.data.BookEntity
import com.renqing.zhang.data.BookTypeEntity
import com.renqing.zhang.data.ContactEntity
import com.renqing.zhang.data.GroupEntity
import com.renqing.zhang.data.RecordEntity
import com.renqing.zhang.data.Settings
import java.time.LocalDate

data class MoneyBalance(val received: Long, val given: Long) {
    val net: Long get() = received - given
}

enum class BalanceTone { Return, Out, Even, None }

data class Snapshot(
    val contacts: List<ContactEntity>,
    val books: List<BookEntity>,
    val records: List<RecordEntity>,
    val groups: List<GroupEntity>,
    val types: List<BookTypeEntity>,
) {
    private val contactMap = contacts.associateBy { it.id }
    private val bookMap = books.associateBy { it.id }
    private val groupMap = groups.associateBy { it.id }
    private val typeMap = types.associateBy { it.id }
    private val byContact = records.groupBy { it.contactId }
    private val byBook = records.groupBy { it.bookId }

    fun contact(id: Long) = contactMap[id]
    fun book(id: Long) = bookMap[id]
    fun group(id: Long) = groupMap[id]
    fun type(id: Long) = typeMap[id]
    fun recordsOfContact(id: Long) = byContact[id].orEmpty()
    fun recordsOfBook(id: Long) = byBook[id].orEmpty()

    fun balance(contactId: Long): MoneyBalance {
        var received = 0L
        var given = 0L
        recordsOfContact(contactId).forEach { row ->
            if (row.direction == DIR_IN) received += row.amount else given += row.amount
        }
        return MoneyBalance(received, given)
    }

    fun bookStatus(book: BookEntity, today: LocalDate): String {
        val date = parseDate(book.date)
        return if (book.archived || !date.isAfter(today)) "closed" else "planning"
    }

    fun isPlanning(book: BookEntity, today: LocalDate) = bookStatus(book, today) == "planning"

    fun latestReceived(contactId: Long): Long? {
        return recordsOfContact(contactId)
            .filter { it.direction == DIR_IN && it.amount > 0 }
            .maxWithOrNull(compareBy<RecordEntity> { it.date }.thenBy { it.id })
            ?.amount
    }

    fun linkedContactId(book: BookEntity): Long? {
        book.contactId?.let { return it }
        return contacts
            .filter { it.name.isNotBlank() && book.title.contains(it.name) }
            .maxByOrNull { it.name.length }
            ?.id
    }

    fun suggestion(book: BookEntity, today: LocalDate): Long? {
        if (book.role != ROLE_GUEST || !isPlanning(book, today)) return null
        if (recordsOfBook(book.id).any { it.direction == DIR_OUT }) return null
        val contactId = linkedContactId(book) ?: return 0L
        return latestReceived(contactId) ?: 0L
    }

    fun qualifiesForReminder(contact: ContactEntity, settings: Settings): Boolean {
        if (!settings.remindOn) return false
        val net = balance(contact.id).net
        if (net < settings.thresholdFen) return false
        val family = group(contact.groupId)?.key == GROUP_FAMILY
        if (settings.familyMute && family) return false
        return true
    }

    fun home(settings: Settings, today: LocalDate): HomeUi {
        val year = today.year.toString()
        val yearRows = records.filter { it.date.startsWith(year) }
        val received = yearRows.filter { it.direction == DIR_IN }.sumOf { it.amount }
        val sent = yearRows.filter { it.direction == DIR_OUT }.sumOf { it.amount }
        val reminders = contacts
            .filter { qualifiesForReminder(it, settings) }
            .sortedByDescending { balance(it.id).net }
            .map { contact ->
                ReminderUi(
                    contactId = contact.id,
                    name = contact.name,
                    relation = contact.relation,
                    groupName = group(contact.groupId)?.name.orEmpty(),
                    net = balance(contact.id).net,
                    copy = reminderCopy(contact, today),
                )
            }
        val recent = records
            .sortedWith(compareByDescending<RecordEntity> { it.date }.thenByDescending { it.id })
            .take(5)
            .map { recordRow(it, today, homeStyle = true) }
        return HomeUi(
            received = received,
            sent = sent,
            net = received - sent,
            month = today.monthValue,
            reminders = reminders,
            recent = recent,
            threshold = settings.thresholdFen,
            remindOn = settings.remindOn,
        )
    }

    fun books(typeId: Long?, today: LocalDate): List<BookCardUi> {
        return books
            .filter { typeId == null || it.typeId == typeId }
            .sortedWith(bookComparator(today))
            .map { book ->
                val rows = recordsOfBook(book.id)
                val planning = isPlanning(book, today)
                val focus = if (book.role == ROLE_HOST) {
                    rows.filter { it.direction == DIR_IN }
                } else {
                    rows.filter { it.direction == DIR_OUT }
                }
                BookCardUi(
                    id = book.id,
                    title = book.title,
                    date = parseDate(book.date),
                    count = focus.size,
                    role = book.role,
                    typeName = type(book.typeId)?.name.orEmpty(),
                    planning = planning,
                    total = focus.sumOf { it.amount },
                    suggestion = suggestion(book, today),
                    archived = book.archived,
                )
            }
    }

    fun bookDetail(id: Long, today: LocalDate, kind: String?): BookDetailUi? {
        val book = book(id) ?: return null
        val rows = recordsOfBook(book.id)
            .filter { kind == null || it.kind == kind }
            .let { list ->
                if (book.role == ROLE_HOST) list.sortedByDescending { it.amount }
                else list.sortedWith(compareByDescending<RecordEntity> { it.date }.thenByDescending { it.id })
            }
        val all = recordsOfBook(book.id)
        val focus = if (book.role == ROLE_HOST) {
            all.filter { it.direction == DIR_IN }
        } else {
            all.filter { it.direction == DIR_OUT }
        }
        val biggest = focus.maxByOrNull { it.amount }
        return BookDetailUi(
            id = book.id,
            title = book.title,
            role = book.role,
            typeName = type(book.typeId)?.name.orEmpty(),
            planning = isPlanning(book, today),
            date = parseDate(book.date),
            place = book.place,
            note = book.note,
            total = focus.sumOf { it.amount },
            count = focus.size,
            biggestName = biggest?.let { contact(it.contactId)?.name },
            biggestAmount = biggest?.amount,
            rows = rows.map { recordRow(it, today, homeStyle = false) },
            defaultDirection = if (book.role == ROLE_HOST) DIR_IN else DIR_OUT,
            archived = book.archived,
        )
    }

    fun contactSections(settings: Settings): List<ContactSectionUi> {
        val grouped = contacts.groupBy { it.groupId }
        return groups.mapNotNull { group ->
            val people = grouped[group.id].orEmpty()
                .sortedByDescending { kotlin.math.abs(balance(it.id).net) }
            if (people.isEmpty() && group.system.not()) return@mapNotNull null
            ContactSectionUi(
                key = group.key,
                name = group.name,
                hint = if (settings.familyMute && group.key == GROUP_FAMILY) "默认不提醒" else null,
                people = people.map { person ->
                    val bal = balance(person.id)
                    val hide = settings.familyMute && group.key == GROUP_FAMILY
                    ContactRowUi(
                        id = person.id,
                        name = person.name,
                        relation = person.relation,
                        letter = person.name.take(1),
                        net = bal.net,
                        hasRecords = recordsOfContact(person.id).isNotEmpty(),
                        hideBalance = hide,
                    )
                },
            )
        }.filter { it.people.isNotEmpty() }
    }

    fun contactDetail(id: Long, today: LocalDate): ContactDetailUi? {
        val person = contact(id) ?: return null
        val bal = balance(id)
        val rows = recordsOfContact(id)
            .sortedWith(compareByDescending<RecordEntity> { it.date }.thenByDescending { it.id })
            .map { recordRow(it, today, homeStyle = false) }
        return ContactDetailUi(
            id = person.id,
            name = person.name,
            relation = person.relation,
            groupName = group(person.groupId)?.name.orEmpty(),
            phone = person.phone,
            note = person.note,
            net = bal.net,
            received = bal.received,
            given = bal.given,
            rows = rows,
        )
    }

    fun stats(year: Int): StatsUi {
        val prefix = year.toString()
        val rows = records.filter { it.date.startsWith(prefix) }
        val received = rows.filter { it.direction == DIR_IN }.sumOf { it.amount }
        val sent = rows.filter { it.direction == DIR_OUT }.sumOf { it.amount }
        val months = (1..12).map { month ->
            val key = "%04d-%02d".format(year, month)
            val monthRows = rows.filter { it.date.startsWith(key) }
            val amount = monthRows.sumOf { it.amount }
            val label = monthLabel(month, monthRows)
            MonthBarUi(month, amount, label)
        }
        val flow = received + sent
        val byType = types.mapNotNull { type ->
            val amount = rows.filter { book(it.bookId ?: -1)?.typeId == type.id }.sumOf { it.amount }
            if (amount <= 0) null else TypeShareUi(type.id, type.name, amount, share(amount, flow))
        }.sortedByDescending { it.amount }
        val unbooked = rows.filter { it.bookId == null }.sumOf { it.amount }
        val typeShares = if (unbooked > 0) {
            byType + TypeShareUi(null, "未归入", unbooked, share(unbooked, flow))
        } else {
            byType
        }
        val byGroup = groups.mapNotNull { group ->
            val amount = rows.filter { contact(it.contactId)?.groupId == group.id }.sumOf { it.amount }
            if (amount <= 0) null else GroupShareUi(group.key, group.name, amount, share(amount, flow))
        }.sortedByDescending { it.amount }
        return StatsUi(year, sent, received, received - sent, months, typeShares, byGroup)
    }

    fun search(query: String): SearchUi {
        val q = query.trim()
        if (q.isEmpty()) return SearchUi(q, emptyList(), emptyList(), emptyList(), blank = true)
        val contactsHit = contacts.filter { person ->
            val groupName = group(person.groupId)?.name.orEmpty()
            listOf(person.name, person.relation, groupName, person.note, person.phone).any { it.contains(q, true) }
        }
        val booksHit = books.filter { book ->
            val typeName = type(book.typeId)?.name.orEmpty()
            listOf(book.title, book.place, book.note, typeName).any { it.contains(q, true) }
        }
        val bookIds = booksHit.map { it.id }.toSet()
        val contactIds = contactsHit.map { it.id }.toSet()
        val recordsHit = records.filter { row ->
            val person = contact(row.contactId)
            val book = row.bookId?.let { book(it) }
            row.contactId in contactIds ||
                row.bookId in bookIds ||
                row.giftName.contains(q, true) ||
                row.note.contains(q, true) ||
                person?.name.orEmpty().contains(q, true) ||
                book?.title.orEmpty().contains(q, true)
        }.sortedWith(compareByDescending<RecordEntity> { it.date }.thenByDescending { it.id })
        return SearchUi(
            query = q,
            contacts = contactsHit.map { person ->
                val bal = balance(person.id)
                SearchContactUi(
                    id = person.id,
                    name = person.name,
                    relation = person.relation,
                    net = bal.net,
                    hasRecords = recordsOfContact(person.id).isNotEmpty(),
                )
            },
            records = recordsHit.map { recordRow(it, LocalDate.now(), homeStyle = false) },
            books = booksHit.map { book ->
                SearchBookUi(book.id, book.title, type(book.typeId)?.name.orEmpty(), parseDate(book.date))
            },
            blank = false,
        )
    }

    fun allRecords(today: LocalDate): List<RecordRowUi> =
        records.sortedWith(compareByDescending<RecordEntity> { it.date }.thenByDescending { it.id })
            .map { recordRow(it, today, homeStyle = true) }

    fun recordDetail(id: Long): RecordDetailUi? {
        val row = records.find { it.id == id } ?: return null
        val person = contact(row.contactId) ?: return null
        val book = row.bookId?.let { book(it) }
        return RecordDetailUi(
            id = row.id,
            contactId = person.id,
            contactName = person.name,
            relation = person.relation,
            groupName = group(person.groupId)?.name.orEmpty(),
            bookId = book?.id,
            bookTitle = book?.title,
            bookRole = book?.role,
            bookDate = book?.date?.let { parseDate(it) },
            direction = row.direction,
            kind = row.kind,
            amount = row.amount,
            giftName = row.giftName,
            date = parseDate(row.date),
            note = row.note,
        )
    }

    private fun recordRow(row: RecordEntity, today: LocalDate, homeStyle: Boolean): RecordRowUi {
        val person = contact(row.contactId)
        val book = row.bookId?.let { book(it) }
        val typeName = book?.let { type(it.typeId)?.name }.orEmpty()
        val date = parseDate(row.date)
        val kindText = kindLabel(row.kind, row.giftName)
        val where = book?.title ?: "未归入礼簿"
        val listType = typeName.ifBlank { row.note.trim() }.ifBlank { if (book == null) "未归入" else "礼簿" }
        val homeSubtitle = "${directionLabel(row.direction)} · $listType · ${formatListDate(date, today)}"
        val detailLine = "${formatDot(date)} · $kindText · $where"
        val directionText = directionLabel(row.direction)
        val lead = if (book != null) {
            typeName.takeIf { it.isNotBlank() }
        } else {
            holidayName(date) ?: row.note.trim().takeIf { it.isNotBlank() }
        }
        return RecordRowUi(
            id = row.id,
            contactId = row.contactId,
            letter = person?.name.orEmpty().take(1),
            title = person?.name.orEmpty(),
            subtitle = if (homeStyle) homeSubtitle else "${person?.relation.orEmpty()} · $kindText",
            detailTitle = if (lead.isNullOrBlank()) directionText else "$lead · $directionText",
            detailSubtitle = detailLine,
            amount = if (row.direction == DIR_IN) row.amount else -row.amount,
            direction = row.direction,
            gift = row.kind == KIND_GIFT,
            date = date,
            bookArchived = book?.archived == true,
        )
    }

    private fun reminderCopy(contact: ContactEntity, today: LocalDate): String {
        val rows = recordsOfContact(contact.id)
            .sortedWith(compareByDescending<RecordEntity> { it.date }.thenByDescending { it.id })
        val upcoming = books
            .filter { book ->
                book.role == ROLE_GUEST && isPlanning(book, today) && (
                    book.contactId == contact.id ||
                        (book.contactId == null && contact.name.isNotBlank() && book.title.contains(contact.name))
                    )
            }
            .minByOrNull { it.date }
        if (upcoming != null) {
            val latestIn = rows.firstOrNull { it.direction == DIR_IN }
            val date = parseDate(upcoming.date)
            val typeName = type(upcoming.typeId)?.name.orEmpty()
            val head = if (latestIn != null) "最近一次他随礼 ${formatYuan(latestIn.amount)}" else "他还没有随礼"
            return "${head}，他家${typeName}是 ${date.monthValue} 月 ${date.dayOfMonth} 日。"
        }
        val recent = rows.take(3)
        if (recent.isEmpty()) return "还没有往来。"
        val parts = recent.joinToString("，") { row ->
            val book = row.bookId?.let { book(it) }
            val typeName = book?.let { type(it.typeId)?.name }.orEmpty()
                .ifBlank { row.note.trim() }
                .ifBlank { "未归入" }
            val verb = if (row.direction == DIR_IN) "收到" else "送过"
            "$typeName$verb ${formatYuan(row.amount)}"
        }
        return if (rows.any { it.direction == DIR_OUT }) "$parts。" else "$parts，此后还没有回过。"
    }

    private fun monthLabel(month: Int, rows: List<RecordEntity>): String {
        if (rows.isEmpty()) return ""
        val titles = rows.mapNotNull { it.bookId?.let { id -> book(id)?.title } }
        val tag = when {
            titles.any { it.contains("百日") } -> "百日"
            titles.any { it.contains("婚礼") } -> "婚礼"
            titles.any { it.contains("满月") } -> "满月"
            titles.any { it.contains("乔迁") } -> "乔迁"
            else -> ""
        }
        return if (tag.isBlank()) "${month}月" else "${month}月$tag"
    }

    private fun bookComparator(today: LocalDate): Comparator<BookEntity> {
        return compareBy<BookEntity> { if (isPlanning(it, today)) 0 else 1 }
            .thenBy { book ->
                val date = parseDate(book.date).toEpochDay()
                if (isPlanning(book, today)) date else -date
            }
            .thenByDescending { it.id }
    }

    private fun share(amount: Long, flow: Long): Int {
        if (flow <= 0) return 0
        return ((amount * 100.0) / flow).toInt()
    }
}

data class HomeUi(
    val received: Long,
    val sent: Long,
    val net: Long,
    val month: Int,
    val reminders: List<ReminderUi>,
    val recent: List<RecordRowUi>,
    val threshold: Long,
    val remindOn: Boolean,
)

data class ReminderUi(
    val contactId: Long,
    val name: String,
    val relation: String,
    val groupName: String,
    val net: Long,
    val copy: String,
)

data class RecordRowUi(
    val id: Long,
    val contactId: Long,
    val letter: String,
    val title: String,
    val subtitle: String,
    val detailTitle: String,
    val detailSubtitle: String,
    val amount: Long,
    val direction: String,
    val gift: Boolean,
    val date: LocalDate,
    val bookArchived: Boolean = false,
)

data class BookCardUi(
    val id: Long,
    val title: String,
    val date: LocalDate,
    val count: Int,
    val role: String,
    val typeName: String,
    val planning: Boolean,
    val total: Long,
    val suggestion: Long?,
    val archived: Boolean = false,
)

data class BookDetailUi(
    val id: Long,
    val title: String,
    val role: String,
    val typeName: String,
    val planning: Boolean,
    val date: LocalDate,
    val place: String,
    val note: String,
    val total: Long,
    val count: Int,
    val biggestName: String?,
    val biggestAmount: Long?,
    val rows: List<RecordRowUi>,
    val defaultDirection: String,
    val archived: Boolean = false,
)

data class ContactSectionUi(
    val key: String,
    val name: String,
    val people: List<ContactRowUi>,
    val hint: String? = null,
)

data class ContactRowUi(
    val id: Long,
    val name: String,
    val relation: String,
    val letter: String,
    val net: Long,
    val hasRecords: Boolean,
    val hideBalance: Boolean,
)

data class ContactDetailUi(
    val id: Long,
    val name: String,
    val relation: String,
    val groupName: String,
    val phone: String,
    val note: String,
    val net: Long,
    val received: Long,
    val given: Long,
    val rows: List<RecordRowUi>,
)

data class MonthBarUi(val month: Int, val amount: Long, val label: String)
data class TypeShareUi(val typeId: Long?, val name: String, val amount: Long, val percent: Int)
data class GroupShareUi(val key: String, val name: String, val amount: Long, val percent: Int)

data class StatsUi(
    val year: Int,
    val sent: Long,
    val received: Long,
    val net: Long,
    val months: List<MonthBarUi>,
    val types: List<TypeShareUi>,
    val groups: List<GroupShareUi>,
)

data class SearchContactUi(
    val id: Long,
    val name: String,
    val relation: String,
    val net: Long,
    val hasRecords: Boolean,
)

data class SearchBookUi(
    val id: Long,
    val title: String,
    val typeName: String,
    val date: LocalDate,
)

data class SearchUi(
    val query: String,
    val contacts: List<SearchContactUi>,
    val records: List<RecordRowUi>,
    val books: List<SearchBookUi>,
    val blank: Boolean,
)

data class RecordDetailUi(
    val id: Long,
    val contactId: Long,
    val contactName: String,
    val relation: String,
    val groupName: String,
    val bookId: Long?,
    val bookTitle: String?,
    val bookRole: String?,
    val bookDate: LocalDate?,
    val direction: String,
    val kind: String,
    val amount: Long,
    val giftName: String,
    val date: LocalDate,
    val note: String,
)

fun toneOf(net: Long, hasRecords: Boolean): BalanceTone = when {
    !hasRecords -> BalanceTone.None
    net > 0 -> BalanceTone.Return
    net < 0 -> BalanceTone.Out
    else -> BalanceTone.Even
}

fun toneLabel(net: Long, hasRecords: Boolean): String = when (toneOf(net, hasRecords)) {
    BalanceTone.None -> "还没有往来"
    BalanceTone.Return -> "待回 ${formatYuan(net)}"
    BalanceTone.Out -> "在外 ${formatYuan(-net)}"
    BalanceTone.Even -> "持平"
}
