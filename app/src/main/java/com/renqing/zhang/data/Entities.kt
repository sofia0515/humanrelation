package com.renqing.zhang.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val key: String,
    val system: Boolean,
    val sort: Int,
)

@Entity(tableName = "book_types")
data class BookTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val key: String,
    val system: Boolean,
    val sort: Int,
)

@Entity(
    tableName = "contacts",
    indices = [Index("groupId")],
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val relation: String,
    val groupId: Long,
    val phone: String,
    val note: String,
)

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val typeId: Long,
    val role: String,
    val date: String,
    val place: String,
    val note: String,
    val archived: Boolean,
    val contactId: Long? = null,
)

@Entity(
    tableName = "records",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("contactId"), Index("bookId")],
)
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long,
    val bookId: Long?,
    val direction: String,
    val kind: String,
    val amount: Long,
    val giftName: String,
    val date: String,
    val note: String,
)

@Entity(tableName = "searches")
data class SearchEntity(
    @PrimaryKey val query: String,
    val at: Long,
)

@Dao
interface LedgerDao {
    @Query("SELECT * FROM contacts ORDER BY id")
    fun contacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM books")
    fun books(): Flow<List<BookEntity>>

    @Query("SELECT * FROM records")
    fun records(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM groups ORDER BY sort, id")
    fun groups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM book_types ORDER BY sort, id")
    fun types(): Flow<List<BookTypeEntity>>

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun contactCount(): Int

    @Query("SELECT COUNT(*) FROM groups")
    suspend fun groupCount(): Int

    @Insert
    suspend fun insertContacts(items: List<ContactEntity>)

    @Insert
    suspend fun insertBooks(items: List<BookEntity>)

    @Insert
    suspend fun insertRecords(items: List<RecordEntity>)

    @Insert
    suspend fun insertGroups(items: List<GroupEntity>)

    @Insert
    suspend fun insertTypes(items: List<BookTypeEntity>)

    @Insert
    suspend fun insertContact(item: ContactEntity): Long

    @Update
    suspend fun updateContact(item: ContactEntity)

    @Insert
    suspend fun insertBook(item: BookEntity): Long

    @Update
    suspend fun updateBook(item: BookEntity)

    @Insert
    suspend fun insertRecord(item: RecordEntity): Long

    @Update
    suspend fun updateRecord(item: RecordEntity)

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun deleteRecord(id: Long)

    @Query("UPDATE books SET archived = :archived WHERE id = :id")
    suspend fun setBookArchived(id: Long, archived: Boolean)

    @Query("SELECT bookId FROM records WHERE id = :id")
    suspend fun recordBookId(id: Long): Long?

    @Query("DELETE FROM records WHERE bookId = :id")
    suspend fun deleteRecordsOfBook(id: Long)

    @Query("SELECT archived FROM books WHERE id = :id")
    suspend fun isBookArchived(id: Long): Boolean?

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBook(id: Long)

    @Insert
    suspend fun insertType(item: BookTypeEntity): Long

    @Insert
    suspend fun insertGroup(item: GroupEntity): Long

    @Query("DELETE FROM book_types WHERE id = :id AND system = 0")
    suspend fun deleteType(id: Long): Int

    @Query("DELETE FROM groups WHERE id = :id AND system = 0")
    suspend fun deleteGroup(id: Long): Int

    @Query("SELECT COUNT(*) FROM books WHERE typeId = :id")
    suspend fun booksUsingType(id: Long): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE groupId = :id")
    suspend fun contactsUsingGroup(id: Long): Int

    @Query("DELETE FROM records")
    suspend fun clearRecords()

    @Query("DELETE FROM books")
    suspend fun clearBooks()

    @Query("DELETE FROM contacts")
    suspend fun clearContacts()

    @Query("DELETE FROM groups")
    suspend fun clearGroups()

    @Query("DELETE FROM book_types")
    suspend fun clearTypes()

    @Query("SELECT * FROM searches ORDER BY at DESC LIMIT 8")
    fun recentSearches(): Flow<List<SearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearch(item: SearchEntity)

    @Query("DELETE FROM searches WHERE query NOT IN (SELECT query FROM searches ORDER BY at DESC LIMIT 8)")
    suspend fun trimSearches()
}
