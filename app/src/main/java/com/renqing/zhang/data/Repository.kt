package com.renqing.zhang.data

import com.renqing.zhang.domain.Snapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ready<T>(val value: T) : Load<T>
    data class Failed(val message: String) : Load<Nothing>
}

data class RecordDraft(
    val id: Long = 0,
    val contactId: Long,
    val bookId: Long?,
    val direction: String,
    val kind: String,
    val amount: Long,
    val giftName: String,
    val date: String,
    val note: String,
)

data class BookDraft(
    val id: Long = 0,
    val title: String,
    val typeId: Long,
    val role: String,
    val date: String,
    val place: String,
    val note: String,
    val contactId: Long? = null,
)

data class ContactDraft(
    val id: Long = 0,
    val name: String,
    val relation: String,
    val groupId: Long,
    val phone: String,
    val note: String,
)

data class BackupPreview(val contacts: Int, val books: Int, val records: Int)

class Repository(
    private val db: AppDatabase,
    private val settingsStore: SettingsStore,
) {
    private val dao = db.ledger()
    val settings = settingsStore.settings

    fun ledger(): Flow<Load<Snapshot>> = flow {
        emit(Load.Loading)
        combine(dao.contacts(), dao.books(), dao.records(), dao.groups(), dao.types()) { contacts, books, records, groups, types ->
            Snapshot(contacts, books, records, groups, types)
        }.collect { emit(Load.Ready(it)) }
    }.catch { emit(Load.Failed("账本暂时读不出来")) }

    fun searches(): Flow<List<String>> = dao.recentSearches().mapNames()

    suspend fun ensureSeeded() {
        if (dao.groupCount() > 0) return
        listOf(
            GroupEntity(name = "家人", key = "family", system = true, sort = 0),
            GroupEntity(name = "亲戚", key = "relative", system = true, sort = 1),
            GroupEntity(name = "朋友", key = "friend", system = true, sort = 2),
            GroupEntity(name = "同事", key = "colleague", system = true, sort = 3),
        ).forEach { dao.insertGroup(it) }
        listOf(
            BookTypeEntity(name = "婚礼", key = "wedding", system = true, sort = 0),
            BookTypeEntity(name = "满月", key = "fullmoon", system = true, sort = 1),
            BookTypeEntity(name = "寿宴", key = "birthday", system = true, sort = 2),
            BookTypeEntity(name = "白事", key = "funeral", system = true, sort = 3),
            BookTypeEntity(name = "乔迁", key = "house", system = true, sort = 4),
            BookTypeEntity(name = "其他", key = "other", system = true, sort = 5),
        ).forEach { dao.insertType(it) }
    }

    suspend fun saveRecord(draft: RecordDraft): Long {
        require(draft.contactId > 0) { "请先选择亲友" }
        require(draft.amount > 0) { "金额要大于 0" }
        if (draft.kind == "gift") require(draft.giftName.isNotBlank()) { "请填写礼品名称" }
        if (draft.id > 0) ensureBookOpen(dao.recordBookId(draft.id))
        ensureBookOpen(draft.bookId)
        val entity = RecordEntity(
            id = draft.id,
            contactId = draft.contactId,
            bookId = draft.bookId,
            direction = draft.direction,
            kind = draft.kind,
            amount = draft.amount,
            giftName = draft.giftName.trim(),
            date = draft.date,
            note = draft.note.trim(),
        )
        return if (draft.id > 0) {
            dao.updateRecord(entity)
            draft.id
        } else {
            dao.insertRecord(entity)
        }
    }

    suspend fun deleteRecord(id: Long) {
        ensureBookOpen(dao.recordBookId(id))
        dao.deleteRecord(id)
    }

    suspend fun saveBook(draft: BookDraft, archived: Boolean = false): Long {
        if (draft.id > 0 && dao.isBookArchived(draft.id) == true) error("这本礼簿已封存，不能修改")
        require(draft.title.isNotBlank()) { "请填写这本账的名称" }
        require(draft.typeId > 0) { "请选择类型" }
        val guest = draft.role == com.renqing.zhang.domain.ROLE_GUEST
        if (guest) require(draft.contactId != null && draft.contactId > 0) { "请选择亲友" }
        val entity = BookEntity(
            id = draft.id,
            title = draft.title.trim(),
            typeId = draft.typeId,
            role = draft.role,
            date = draft.date,
            place = draft.place.trim(),
            note = draft.note.trim(),
            archived = archived,
            contactId = if (guest) draft.contactId else null,
        )
        return if (draft.id > 0) {
            dao.updateBook(entity)
            draft.id
        } else {
            dao.insertBook(entity)
        }
    }

    suspend fun archiveBook(id: Long) = dao.setBookArchived(id, true)

    suspend fun unarchiveBook(id: Long) = dao.setBookArchived(id, false)

    private suspend fun ensureBookOpen(bookId: Long?) {
        if (bookId != null && dao.isBookArchived(bookId) == true) error("这本礼簿已封存，不能修改")
    }

    suspend fun deleteBook(id: Long) {
        if (dao.isBookArchived(id) != false) error("封存的礼簿不能删除")
        dao.deleteRecordsOfBook(id)
        dao.deleteBook(id)
    }

    suspend fun saveContact(draft: ContactDraft): Long {
        require(draft.name.isNotBlank()) { "请填写姓名" }
        require(draft.relation.isNotBlank()) { "请填写关系" }
        val entity = ContactEntity(
            id = draft.id,
            name = draft.name.trim(),
            relation = draft.relation.trim(),
            groupId = draft.groupId,
            phone = draft.phone.trim(),
            note = draft.note.trim(),
        )
        return if (draft.id > 0) {
            dao.updateContact(entity)
            draft.id
        } else {
            dao.insertContact(entity)
        }
    }

    suspend fun addType(name: String) {
        val trimmed = name.trim()
        require(trimmed.isNotBlank())
        dao.insertType(
            BookTypeEntity(
                name = trimmed,
                key = "c_${System.currentTimeMillis()}",
                system = false,
                sort = 100,
            ),
        )
    }

    suspend fun deleteType(id: Long): Boolean {
        if (dao.booksUsingType(id) > 0) return false
        return dao.deleteType(id) > 0
    }

    suspend fun addGroup(name: String) {
        val trimmed = name.trim()
        require(trimmed.isNotBlank())
        dao.insertGroup(
            GroupEntity(
                name = trimmed,
                key = "c_${System.currentTimeMillis()}",
                system = false,
                sort = 100,
            ),
        )
    }

    suspend fun deleteGroup(id: Long): Boolean {
        if (dao.contactsUsingGroup(id) > 0) return false
        return dao.deleteGroup(id) > 0
    }

    suspend fun rememberSearch(query: String) {
        val text = query.trim()
        if (text.isEmpty()) return
        dao.upsertSearch(SearchEntity(text, System.currentTimeMillis()))
        dao.trimSearches()
    }

    suspend fun exportWorkbook(snapshot: Snapshot, bookId: Long?): ByteArray =
        Transfer.workbook(snapshot, bookId)

    suspend fun exportBackup(snapshot: Snapshot, settings: Settings): String =
        Transfer.backupJson(snapshot, settings)

    fun previewBackup(json: String): BackupPreview = Transfer.preview(json)

    suspend fun restore(json: String) {
        restoreParsed(Transfer.parse(json))
    }

    private suspend fun restoreParsed(payload: ParsedBackup) {
        dao.clearRecords()
        dao.clearBooks()
        dao.clearContacts()
        dao.clearGroups()
        dao.clearTypes()
        if (payload.groups.isNotEmpty()) dao.insertGroups(payload.groups)
        if (payload.types.isNotEmpty()) dao.insertTypes(payload.types)
        if (payload.contacts.isNotEmpty()) dao.insertContacts(payload.contacts)
        if (payload.books.isNotEmpty()) dao.insertBooks(payload.books)
        if (payload.records.isNotEmpty()) dao.insertRecords(payload.records)
        bumpSequences()
        settingsStore.applyBackup(
            remind = payload.remindOn,
            thresholdYuan = payload.threshold,
            familyMuteOn = payload.familyMute,
            name = payload.userName,
            phone = payload.userPhone,
        )
    }

    private fun bumpSequences() {
        val sql = db.openHelper.writableDatabase
        listOf("groups", "book_types", "contacts", "books", "records").forEach { table ->
            sql.execSQL(
                "INSERT OR REPLACE INTO sqlite_sequence(name, seq) " +
                    "SELECT '$table', IFNULL((SELECT MAX(id) FROM $table), 0)",
            )
        }
    }

    suspend fun setRemind(on: Boolean) = settingsStore.setRemind(on)
    suspend fun setThreshold(yuan: Int) = settingsStore.setThreshold(yuan)
    suspend fun setFamilyMute(on: Boolean) = settingsStore.setFamilyMute(on)
    suspend fun setLock(on: Boolean) = settingsStore.setLock(on)
    suspend fun finishOnboarding() = settingsStore.setOnboardingDone()
    suspend fun setProfile(name: String, phone: String) = settingsStore.setProfile(name, phone)
}

private fun Flow<List<SearchEntity>>.mapNames(): Flow<List<String>> =
    map { list -> list.map { item -> item.query } }

data class ParsedBackup(
    val groups: List<GroupEntity>,
    val types: List<BookTypeEntity>,
    val contacts: List<ContactEntity>,
    val books: List<BookEntity>,
    val records: List<RecordEntity>,
    val remindOn: Boolean,
    val threshold: Int,
    val familyMute: Boolean,
    val userName: String,
    val userPhone: String,
)
