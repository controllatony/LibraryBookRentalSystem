package com.example.libraryrental

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import coil.compose.SubcomposeAsyncImage
import com.example.libraryrental.data.*
import com.example.libraryrental.domain.LibraryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LibraryApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryApp(vm: LibraryViewModel = viewModel()) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val message by vm.message.collectAsState(initial = "")
    LaunchedEffect(message) { if (message.isNotBlank()) snackbar.showSnackbar(message) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF245B4A),
            secondary = androidx.compose.ui.graphics.Color(0xFFB56B36),
            surface = androidx.compose.ui.graphics.Color(0xFFFFFBF5)
        )
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Campus Library", fontWeight = FontWeight.Bold) }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = navController.currentDestination?.route == "catalog",
                        onClick = { navController.navigate("catalog") { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Book, contentDescription = null) },
                        label = { Text("Catalog") }
                    )
                    NavigationBarItem(
                        selected = navController.currentDestination?.route == "reservations",
                        onClick = { navController.navigate("reservations") { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                        label = { Text("My rentals") }
                    )
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "catalog",
                modifier = Modifier.padding(padding)
            ) {
                composable("catalog") { CatalogScreen(vm) }
                composable("reservations") { ReservationsScreen(vm) }
            }
        }
    }
}

@Composable
private fun CatalogScreen(vm: LibraryViewModel) {
    val books by vm.books.collectAsStateWithLifecycle()
    val query by vm.searchQuery.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<BookEntity?>(null) }
    var userName by remember { mutableStateOf("") }
    var duration by remember { mutableIntStateOf(14) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("Find your next read", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        Text("Browse the collection and reserve a book.", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = query,
            onValueChange = vm::updateSearch,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            label = { Text("Search title or author") },
            singleLine = true
        )
        Text("${books.size} books", style = MaterialTheme.typography.labelLarge)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
            items(books, key = { it.bookId }) { book ->
                BookCard(book = book, action = {
                    if (book.isAvailable) selected = book
                })
            }
        }
    }

    selected?.let { book ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("Reserve book") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(book.title, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it.take(60) },
                        label = { Text("Your name") },
                        singleLine = true
                    )
                    Text("Rental duration")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(7, 14, 21, 30).forEach { days ->
                            FilterChip(
                                selected = duration == days,
                                onClick = { duration = days },
                                label = { Text("$days d") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.reserve(book, userName, duration)
                    selected = null
                }, enabled = userName.isNotBlank()) { Text("Reserve") }
            },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("Not now") } }
        )
    }
}

@Composable
private fun BookCoverImage(coverImage: String?, title: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val model = remember(coverImage) {
        val img = coverImage?.trim()
        when {
            img.isNullOrEmpty() -> null
            img.startsWith("http://") || img.startsWith("https://") -> img
            else -> {
                val resId = context.resources.getIdentifier(img, "drawable", context.packageName)
                if (resId != 0) resId else null
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.size(54.dp, 72.dp)
    ) {
        if (model != null) {
            SubcomposeAsyncImage(
                model = model,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun BookCard(book: BookEntity, action: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            BookCoverImage(coverImage = book.coverImage, title = book.title)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(book.author, style = MaterialTheme.typography.bodyMedium)
                Text(book.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(6.dp))
                StatusBadge(available = book.isAvailable)
            }
            if (book.isAvailable) {
                Button(onClick = action, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Reserve") }
            }
        }
    }
}

@Composable
private fun StatusBadge(available: Boolean) {
    val color = if (available) androidx.compose.ui.graphics.Color(0xFF24734B) else androidx.compose.ui.graphics.Color(0xFFB74735)
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) {
        Text(
            if (available) "Available" else "Borrowed",
            color = color,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ReservationsScreen(vm: LibraryViewModel) {
    val bookings by vm.bookings.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("My rentals", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        Text("Keep track of current books and return dates.")
        if (bookings.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("No active rentals yet", style = MaterialTheme.typography.titleMedium)
                    Text("Reserve a book from the catalog to see it here.")
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                items(bookings, key = { it.booking.bookingId }) { item ->
                    val booking = item.booking
                    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
                    val remaining = ((booking.returnDeadline - System.currentTimeMillis()) / (24L * 60 * 60 * 1000)).toInt()
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            BookCoverImage(coverImage = item.book.coverImage, title = item.book.title)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text(item.book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("By ${item.book.author} • ${item.book.category}", style = MaterialTheme.typography.bodyMedium)
                                Text("Reserved: ${dateFormat.format(Date(booking.bookingDate))}")
                                Text("Return by: ${dateFormat.format(Date(booking.returnDeadline))}")
                                Text(
                                    when {
                                        remaining < 0 -> "Overdue by ${-remaining} day(s)"
                                        remaining == 0 -> "Due today"
                                        else -> "$remaining day(s) remaining"
                                    },
                                    color = if (remaining <= 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { vm.renew(booking.bookingId) }) { Text("+7 days") }
                                    Button(onClick = { vm.returnBook(booking.bookingId) }) { Text("Return") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
