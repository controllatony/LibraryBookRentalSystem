package com.example.libraryrental

import com.example.libraryrental.data.BookEntity
import com.example.libraryrental.data.BookingEntity
import com.example.libraryrental.data.BookingStatus
import com.example.libraryrental.domain.SortOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryEntityTest {

    @Test
    fun bookEntity_defaultValues_isAvailable() {
        val book = BookEntity(
            title = "Clean Code",
            author = "Robert C. Martin",
            category = "Software"
        )
        assertTrue(book.isAvailable)
        assertEquals("Clean Code", book.title)
        assertEquals("Software", book.category)
    }

    @Test
    fun bookingEntity_defaultStatus_isPending() {
        val booking = BookingEntity(
            bookOwnerId = 1,
            userName = "Alice",
            bookingDate = 1000L,
            returnDeadline = 2000L
        )
        assertEquals(BookingStatus.PENDING, booking.status)
        assertEquals("Alice", booking.userName)
    }

    @Test
    fun sortOption_labels_areCorrect() {
        assertEquals("Title (A-Z)", SortOption.TITLE_ASC.label)
        assertEquals("Available First", SortOption.AVAILABILITY.label)
    }

    @Test
    fun bookingStatus_conversions() {
        val active = BookingStatus.ACTIVE
        assertEquals("ACTIVE", active.name)
        assertEquals(BookingStatus.ACTIVE, BookingStatus.valueOf("ACTIVE"))
    }
}
