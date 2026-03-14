# NewsFlow Android App

Native Android client for your self-hosted NewsFlow news aggregator.

## Features

| Screen | What it does |
|--------|-------------|
| **Feed** | Paginated article cards with topic chips, search, pull-to-refresh, infinite scroll |
| **Article** | Full view with hero image, AI summary on-demand, like/dislike/save/share actions |
| **Topics** | Browse all 60+ topics grouped by category; subscribe/unsubscribe with a toggle |
| **Saved** | Read-later list; tap bookmark again to remove |
| **Stats** | Likes / dislikes / reads / saved counts, top liked topics, AI affinity scores |
| **Settings** | Ollama config + test, feed settings, email digest config, admin panel |

## Requirements

- Android Studio Iguana (2023.2.1) or newer
- Android SDK 34 · minSdk 26 (Android 8.0+)
- Java 17 / Kotlin 1.9
- A running [NewsFlow](https://github.com/faikwo/newsflow) server

## Quick Start

1. **Open** Android Studio → **File → Open** → select the `newsflow-android/` folder
2. Wait for Gradle sync to complete
3. **Run** on an emulator or physical device
4. On first launch, enter your server URL, e.g. `http://192.168.1.100:3000`
5. Sign in or register

> **Tip:** The app uses `android:usesCleartextTraffic="true"` to allow local HTTP
> connections. This is fine for a home server; for a public HTTPS server it's harmless.

## Navigation

```
Bottom nav:  Feed | Topics | Saved | Stats | Settings
Settings ──► Email Digest (full schedule config + send-now)
```

## Authentication Flow

```
App Launch
  ├─ No server URL saved → ServerSetupActivity  (pings /api/health)
  ├─ No JWT token        → LoginActivity        (login or register)
  └─ Session valid       → MainActivity         (bottom nav)
```

JWT tokens are stored in SharedPreferences and injected into every API request via
an OkHttp `Interceptor`. Tokens last 24 hours (configurable server-side).

## Project Structure

```
app/src/main/
├── java/com/newsflow/
│   ├── NewsFlowApp.kt          Application — initialises SessionManager
│   ├── MainActivity.kt         Entry point: auth routing → bottom nav
│   ├── api/
│   │   ├── NewsFlowApi.kt      Retrofit interface (all endpoints)
│   │   ├── RetrofitClient.kt   OkHttp singleton + dynamic auth interceptor
│   │   └── ApiRepository.kt   Suspend fns, ApiResult<T> sealed class
│   ├── data/Models.kt          All request / response data classes
│   ├── utils/SessionManager.kt SharedPreferences session store
│   └── ui/
│       ├── auth/               ServerSetupActivity, LoginActivity
│       ├── feed/               FeedFragment, FeedViewModel, ArticleAdapter
│       ├── article/            ArticleFragment
│       ├── topics/             TopicsFragment, TopicsAdapter
│       ├── saved/              SavedFragment
│       ├── stats/              StatsFragment
│       ├── digest/             DigestFragment
│       └── settings/           SettingsFragment
└── res/
    ├── layout/                 XML layouts for all screens + item cards
    ├── navigation/nav_graph.xml
    ├── menu/bottom_nav_menu.xml
    ├── anim/                   Slide transition animations
    ├── drawable/               Vector icons
    └── values/                 colors, strings, themes, dimens
```

## API Coverage

Every backend endpoint is mapped in `NewsFlowApi.kt`:

- Auth: login, register, me, delete-me, forgot-password, signup-enabled
- Admin: list-users, update-user, delete-user
- Articles: feed (paginated + search + filter), saved, interact, refresh, summarize, share-token, affinity
- Topics: all, subscribed, subscribe, unsubscribe
- Settings: get, update, test-ollama
- Stats: preferences/stats
- Digest: get schedule, update schedule, send-now

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Retrofit + Gson | 2.9.0 | HTTP + JSON serialisation |
| OkHttp + logging | 4.12.0 | Network client |
| Glide | 4.16.0 | Image loading / caching |
| Navigation Component | 2.7.5 | Fragment nav + back stack |
| ViewModel + LiveData | 2.7.0 | MVVM state |
| Material Components | 1.11.0 | UI components |
| Kotlin Coroutines | 1.7.3 | Async / suspend |
