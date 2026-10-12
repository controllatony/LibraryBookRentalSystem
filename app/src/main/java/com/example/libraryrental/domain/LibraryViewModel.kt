package com.example.libraryrental.domain

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.libraryrental.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = LibraryDatabase.getInstance(application)
    private val repository = LibraryRepository(database)

    private val query = MutableStateFlow("")
    val searchQuery: StateFlow<String> = query.asStateFlow()
    val books: StateFlow<List<BookEntity>> = query
        .flatMapLatest(repository::searchBooks)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bookings: StateFlow<List<BookingWithBook>> =
        repository.observeBookings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableSharedFlow<String>()
    val message = _message.asSharedFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.seedIfEmpty() }
                .onFailure { _message.emit(it.message ?: "Could not load sample books.") }
        }
    }

    fun updateSearch(value: String) { query.value = value.take(100) }

    fun reserve(book: BookEntity, user: String, days: Int) = perform {
        repository.reserve(book, user, days)
        "Reserved '${book.title}' successfully."
    }

    fun renew(bookingId: Long) = perform {
        repository.renew(bookingId, 7)
        "Return deadline extended by 7 days."
    }

    fun returnBook(bookingId: Long) = perform {
        repository.returnBook(bookingId)
        "Book returned successfully."
    }

    fun cancel(bookingId: Long) = perform {
        repository.cancel(bookingId)
        "Reservation cancelled."
    }

    private fun perform(action: suspend () -> String) {
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess { _message.emit(it) }
                .onFailure { _message.emit(it.message ?: "Something went wrong.") }
        }
    }
}
