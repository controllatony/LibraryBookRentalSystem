# Campus Library — Book Rental & Reservation System

An Android app built with Kotlin, Jetpack Compose, Room, Coroutines, Flow, Navigation Compose, and a simple MVVM architecture.

## Features in this App
- Reactive catalog with title/author search.
- Available/Borrowed status badges.
- Reserve with a name and 7, 14, 21, or 30-day duration.
- Room persistence and sample book records.
- Current rentals dashboard with reservation date, deadline, days remaining, renewal, and return.
- Database transactions keep booking creation and availability updates together.
- Basic input validation and user feedback using snackbars.

## Architecture
- `data/`: Room entities, DAOs, database and repository.
- `domain/`: `LibraryViewModel` and state exposed to Compose.
- `MainActivity.kt`: Compose screens and Navigation Compose.

