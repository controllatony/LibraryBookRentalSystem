package com.example.libraryrental.domain

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.libraryrental.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOption(val label: String) {
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    AUTHOR_ASC("Author (A-Z)"),
    AVAILABILITY("Available First")
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = LibraryDatabase.getInstance(application)
    private val repository = LibraryRepository(database)

    private val query = MutableStateFlow("")
    val searchQuery: StateFlow<String> = query.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.TITLE_ASC)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    val books: StateFlow<List<BookEntity>> = combine(
        query.flatMapLatest(repository::searchBooks),
        _selectedCategory,
        _sortOption
    ) { bookList, category, sort ->
        val filtered = if (category == "All") bookList else bookList.filter { it.category.equals(category, ignoreCase = true) }
        when (sort) {
            SortOption.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            SortOption.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            SortOption.AUTHOR_ASC -> filtered.sortedBy { it.author.lowercase() }
            SortOption.AVAILABILITY -> filtered.sortedByDescending { it.isAvailable }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<String>> = repository.searchBooks("")
        .map { list -> listOf("All") + list.map { it.category }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), listOf("All"))

    val bookings: StateFlow<List<BookingWithBook>> =
        repository.observeBookings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pastBookings: StateFlow<List<BookingWithBook>> =
        repository.observePastBookings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableSharedFlow<String>()
    val message = _message.asSharedFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.seedIfEmpty() }
                .onFailure { _message.emit(it.message ?: "Could not load sample books.") }
        }
    }

    fun updateSearch(value: String) { query.value = value.take(100) }

    fun selectCategory(category: String) { _selectedCategory.value = category }

    fun selectSortOption(option: SortOption) { _sortOption.value = option }

    fun toggleTheme() { _isDarkTheme.value = !_isDarkTheme.value }

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
