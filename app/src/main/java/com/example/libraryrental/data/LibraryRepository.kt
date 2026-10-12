package com.example.libraryrental.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class LibraryRepository(private val database: LibraryDatabase) {
    private val books = database.bookDao()
    private val bookings = database.bookingDao()

    fun searchBooks(query: String): Flow<List<BookEntity>> =
        if (query.isBlank()) books.observeBooks() else books.searchBooks(query.trim())

    fun observeBookings(): Flow<List<BookingWithBook>> = bookings.observeCurrentBookings()

    suspend fun seedIfEmpty() {
        if (books.allBooksOnce().isEmpty()) {
            books.insertAll(
                listOf(
                    BookEntity(title = "Clean Code", author = "Robert C. Martin", category = "Software", coverImage = "clean_code"),
                    BookEntity(title = "The Pragmatic Programmer", author = "Andrew Hunt", category = "Software", coverImage = "pragmatic_programmer"),
                    BookEntity(title = "Introduction to Algorithms", author = "Thomas H. Cormen", category = "Computer Science", coverImage = "intro_algorithms"),
                    BookEntity(title = "Atomic Habits", author = "James Clear", category = "Personal Development", coverImage = "atomic_habits"),
                    BookEntity(title = "To Kill a Mockingbird", author = "Harper Lee", category = "Fiction", coverImage = "mockingbird"),
                    BookEntity(title = "Database System Concepts", author = "Abraham Silberschatz", category = "Database", coverImage = "database_system"),
                    BookEntity(title = "Kotlin in Action", author = "Dmitry Jemerov", category = "Programming", coverImage = "kotlin_in_action"),
                    BookEntity(title = "The Great Gatsby", author = "F. Scott Fitzgerald", category = "Fiction", coverImage = "great_gatsby")
                )
            )
        }
    }

    suspend fun reserve(book: BookEntity, userName: String, days: Int) {
        require(userName.isNotBlank()) { "Enter your name before reserving a book." }
        require(days in 1..30) { "Rental duration must be between 1 and 30 days." }
        database.withTransaction {
            val current = books.getBook(book.bookId) ?: error("Book no longer exists.")
            check(current.isAvailable) { "This book is no longer available." }
            val now = System.currentTimeMillis()
            bookings.insert(
                BookingEntity(
                    bookOwnerId = book.bookId,
                    userName = userName.trim().take(60),
                    bookingDate = now,
                    returnDeadline = now + days * 24L * 60 * 60 * 1000,
                    status = BookingStatus.ACTIVE
                )
            )
            books.setAvailability(book.bookId, false)
        }
    }

    suspend fun renew(bookingId: Long, extraDays: Int) {
        require(extraDays in 1..30) { "Choose between 1 and 30 extra days." }
        val booking = bookings.getById(bookingId) ?: error("Booking not found.")
        check(booking.status == BookingStatus.ACTIVE || booking.status == BookingStatus.PENDING) {
            "Only current bookings can be renewed."
        }
        val newDeadline = maxOf(booking.returnDeadline, System.currentTimeMillis()) +
            extraDays * 24L * 60 * 60 * 1000
        bookings.renew(bookingId, newDeadline)
    }

    suspend fun returnBook(bookingId: Long) {
        database.withTransaction {
            val booking = bookings.getById(bookingId) ?: error("Booking not found.")
            check(booking.status == BookingStatus.ACTIVE) { "This booking is not active." }
            bookings.markReturned(bookingId)
            books.setAvailability(booking.bookOwnerId, true)
        }
    }

    suspend fun cancel(bookingId: Long) {
        database.withTransaction {
            val booking = bookings.getById(bookingId) ?: error("Booking not found.")
            check(booking.status == BookingStatus.PENDING) { "Only pending reservations can be cancelled." }
            bookings.cancelPending(bookingId)
            books.setAvailability(booking.bookOwnerId, true)
        }
    }
}

