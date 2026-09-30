# Pharma Trade

A B2B pharmaceutical trading platform that connects **pharmacies** (buyers) with **suppliers** (sellers), with an **admin** role that approves new accounts.

Built with **Kotlin Multiplatform** and **Jetpack Compose / Compose Multiplatform**, sharing business logic and UI across Android, desktop and iOS.

## Features

- **Authentication**: login and registration for pharmacies and suppliers, with a pending-approval flow for new accounts
- **Admin dashboard**: review, approve or reject pending pharmacies and suppliers
- **Pharmacy ordering**
  - Browse a combined catalog of every supplier's drugs from the home screen
  - Create orders in *best discount* mode (the backend allocates across suppliers) or *specific supplier* mode
  - Add items by search, from a supplier's inventory, or by uploading an Excel/CSV file
  - Track orders by status, see supplier splits, and resolve shortages
- **Supplier side**: drug listings, inventory, minimum order rules and incoming orders
- **Push notifications** through Firebase Cloud Messaging
- **Localization**: English and Arabic

## Project structure

```
PharamaTrade/
├── app/              Android application (entry point, DI, navigation, Firebase setup)
├── desktopApp/       Desktop (JVM) launcher
├── iosApp/           iOS app shell (see iosApp/README.md)
├── build-logic/      Gradle convention plugins (pharmatrade.kmp.*, pharmatrade.feature.*)
├── shared/           Umbrella module: re-exports all modules to the apps / iOS framework,
│                     and hosts the Home shell that combines screens from several features
├── core/
│   ├── common/       Result, shared models, i18n, session, utils, push, reminders, file IO
│   ├── network/      Ktor HttpClient (per-platform engine), ApiResponse, shared DTOs
│   └── ui/           Theme + reusable composables
└── feature/<name>/
    ├── domain/       Models, repository contracts, use cases
    ├── data/         DTOs, APIs, repository implementations
    └── presentation/ Compose screens + ViewModels
```

Every module is Kotlin Multiplatform (Android, desktop, iOS) with its own `androidMain` /
`desktopMain` / `iosMain` source sets where needed.

Features: `auth`, `admin`, `pharmacyorder`, `supplierorder`, `seller`, `drugs` (domain + data only),
`catalog`, `cart`, `search` (presentation only), `profile` and `notification`.

Dependency rules:
- `domain` → `:core:common`
- `data` → its own `domain`, `:core:common`, `:core:network`
- `presentation` → its own `domain`, `:core:common`, `:core:ui`
- A feature may depend on **another feature's `domain`** (e.g. `seller:presentation` → `drugs:domain`),
  never on another feature's `data` or `presentation`. Screens that combine several features are
  composed in `:shared` (Home) or `:app` (e.g. the admin Profile tab is passed in as a slot).

## Tech stack

| Area | Library |
|------|---------|
| Language | Kotlin 2.0 (Multiplatform) |
| UI | Jetpack Compose, Compose Multiplatform, Material 3 |
| Networking | Ktor client + kotlinx.serialization |
| Async | Kotlin Coroutines / Flow |
| Architecture | MVVM + clean architecture, manual DI |
| Storage | Multiplatform Settings |
| Images | Coil 3 |
| Firebase | Cloud Messaging, Crashlytics |

## Getting started

### Requirements

- Android Studio (latest stable)
- JDK 11 or newer (Android Studio's bundled JBR works)
- Android SDK 36
- `app/google-services.json` from the Firebase console

### Run on Android

1. Clone the repository:
   ```bash
   git clone https://github.com/Hagersamy/PharmaTrade.git
   ```
2. Open the project in Android Studio and let Gradle sync.
3. Pick a build variant (**Build → Select Build Variant**):
   - `devDebug`: uses the development backend
   - `prodDebug` / `prodRelease`: use the production backend
4. Run the `app` configuration.

To build from the command line:

```bash
./gradlew :app:assembleDevDebug
```

### Run on desktop

```bash
./gradlew :desktopApp:run
```

### iOS

iOS targets must be built on macOS with Xcode. See [iosApp/README.md](iosApp/README.md).

## Backend

The API base URL is set per product flavor in `app/build.gradle.kts` (`BASE_URL`) and applied to `ApiClient` at startup.

| Flavor | Backend |
|--------|---------|
| `dev` | Development (ngrok) |
| `prod` | Production (Railway) |
