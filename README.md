# Campus Library — Book Rental & Reservation System

A starter Android app built with Kotlin, Jetpack Compose, Room, Coroutines, Flow, Navigation Compose, and a simple MVVM architecture.

## Features in this starter
- Reactive catalog with title/author search.
- Available/Borrowed status badges.
- Reserve with a name and 7, 14, 21, or 30-day duration.
- Room persistence and sample book records.
- Current rentals dashboard with reservation date, deadline, days remaining, renewal, and return.
- Database transactions keep booking creation and availability updates together.
- Basic input validation and user feedback using snackbars.

## Open and run
1. Open this folder in Android Studio (a recent version with Android SDK 35 installed).
2. Allow Gradle sync.
3. Run the `app` configuration on an emulator or Android device (API 26+).

## Architecture
- `data/`: Room entities, DAOs, database and repository.
- `domain/`: `LibraryViewModel` and state exposed to Compose.
- `MainActivity.kt`: Compose screens and Navigation Compose.

## Current limitations
- This is a local, single-user learning app; there is no sign-in, cloud sync, late-fee payment, or multi-user isolation.
- The first version treats a reservation as an active rental immediately. Pending pickup flow and cancellation UI are planned enhancements.
- This generated starter has not been built or run in an Android Studio environment here. Compile and test it locally; use the build errors, if any, as the first validation step.

## Suggested next milestones
See `FEATURE_ROADMAP.md` for incremental enhancements suitable for separate Git commits.
