package com.example.libraryrental.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class BookingStatus { PENDING, ACTIVE, RETURNED }

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val bookId: Long = 0,
    val title: String,
    val author: String,
    val category: String,
    val isAvailable: Boolean = true
)

@Entity(
    tableName = "bookings",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["bookId"],
        childColumns = ["bookOwnerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("bookOwnerId")]
)
data class BookingEntity(
    @PrimaryKey(autoGenerate = true) val bookingId: Long = 0,
    val bookOwnerId: Long,
    val userName: String,
    val bookingDate: Long,
    val returnDeadline: Long,
    val status: BookingStatus = BookingStatus.PENDING
)

class Converters {
    @TypeConverter fun fromStatus(value: BookingStatus): String = value.name
    @TypeConverter fun toStatus(value: String): BookingStatus = BookingStatus.valueOf(value)
}

data class BookingWithBook(
    @Embedded val booking: BookingEntity,
    @Relation(parentColumn = "bookOwnerId", entityColumn = "bookId")
    val book: BookEntity
)

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY title COLLATE NOCASE")
    fun observeBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' ORDER BY title COLLATE NOCASE")
    fun searchBooks(query: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isAvailable = 1 ORDER BY title COLLATE NOCASE")
    fun observeAvailableBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books")
    suspend fun allBooksOnce(): List<BookEntity>

    @Query("SELECT * FROM books WHERE bookId = :bookId LIMIT 1")
    suspend fun getBook(bookId: Long): BookEntity?

    @Insert
    suspend fun insertAll(books: List<BookEntity>)

    @Insert
    suspend fun insert(book: BookEntity): Long

    @Query("UPDATE books SET isAvailable = :available WHERE bookId = :bookId")
    suspend fun setAvailability(bookId: Long, available: Boolean)
}

@Dao
interface BookingDao {
    @Transaction
    @Query("SELECT * FROM bookings WHERE status IN ('PENDING', 'ACTIVE') ORDER BY returnDeadline")
    fun observeCurrentBookings(): Flow<List<BookingWithBook>>

    @Insert
    suspend fun insert(booking: BookingEntity): Long

    @Query("UPDATE bookings SET returnDeadline = :deadline WHERE bookingId = :bookingId AND status IN ('PENDING', 'ACTIVE')")
    suspend fun renew(bookingId: Long, deadline: Long)

    @Query("UPDATE bookings SET status = 'RETURNED' WHERE bookingId = :bookingId")
    suspend fun markReturned(bookingId: Long)

    @Query("DELETE FROM bookings WHERE bookingId = :bookingId AND status = 'PENDING'")
    suspend fun cancelPending(bookingId: Long): Int

    @Query("SELECT * FROM bookings WHERE bookingId = :bookingId LIMIT 1")
    suspend fun getById(bookingId: Long): BookingEntity?
}

@Database(entities = [BookEntity::class, BookingEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun bookingDao(): BookingDao

    companion object {
        @Volatile private var INSTANCE: LibraryDatabase? = null

        fun getInstance(context: Context): LibraryDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LibraryDatabase::class.java,
                    "campus_library.db"
                ).build().also { INSTANCE = it }
            }
    }
}
