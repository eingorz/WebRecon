# WebRecon Toolkit

A mobile web penetration testing companion for Android. Combines target reconnaissance with standalone security utilities, all stored in local history.

Made for a school project and not intended for any actual long term use.

## What it does

**Recon tab** — Enter any domain and run an automated pipeline: subdomain enumeration via certificate transparency (crt.sh) + common wordlist, DNS resolution of all discovered subdomains, technology fingerprinting, security headers audit (graded A–F), robots.txt / sitemap.xml analysis, and sensitive path probing (HEAD requests against ~90 common paths like `/.git/config`, `/.env`, `/admin`). Findings stream into the UI live as each stage completes, colour-coded by severity (INFO / WARN / CRIT). Results are persisted to SQLite and can be shared as a Markdown report.

**Tools tab** — Four standalone utilities:
- **JWT Inspector** — decode header/payload, flag issues (alg=none, expired, missing exp), brute-force signature against a built-in weak-secrets wordlist.
- **HTTP Inspector** — full HTTP client (GET/POST/PUT/DELETE/HEAD), custom headers, response body, timing, and security headers grade on every response. "Open in browser" button for any URL.
- **Encoder / Decoder** — Base64, URL, Hex, HTML entities in both directions, with auto-detect.
- **Hash Lab** — MD5 / SHA-1 / SHA-256 / SHA-512, hash-type identification by length, and HIBP k-anonymity range check.

**History tab** — Unified searchable timeline of all past engagements and tool operations. Tap any entry to reopen it.

---

## Build & run

**Prerequisites:** Android Studio (any recent version), Android SDK platform 35 installed.

The Gradle build requires JDK 17–21. If your system JDK is newer (e.g. JDK 25), `gradle.properties` already sets:
```
org.gradle.java.home=C:\\Program Files\\Android\\Android Studio\\jbr
```
Adjust the path if your Android Studio is installed elsewhere.

**Build debug APK:**
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

**Install on connected device / AVD:**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Minimum Android version:** API 31 (Android 12).

---

## Assignment requirement mapping

| Requirement | Where it is satisfied |
|---|---|
| ≥3 Activities with proper lifecycle | `MainActivity`, `EngagementActivity`, `ToolActivity`, `SettingsActivity` — all 4 handle `onCreate`/`onDestroy` and background executor shutdown |
| Bottom navigation | `BottomNavigationView` in `activity_main.xml`, wired in `MainActivity` |
| Toolbar / ActionBar with title, up-navigation, overflow menu | `MaterialToolbar` in every activity; up-nav via `parentActivityName` + `setDisplayHomeAsUpEnabled`; overflow in `MainActivity` → Settings |
| Full instance state preservation | Every fragment/activity saves text fields, spinner positions, scroll positions, and tab selection in `onSaveInstanceState` / restores in `onViewCreated` / `onCreate` |
| SharedPreferences | `Prefs.java` + `SettingsActivity` / `SettingsFragment` — theme, timeout, user-agent, HIBP toggle, recon intensity |
| Internal storage (wordlists) | `WordlistManager.copyAssetsToInternalStorage()` copies 3 `.txt` wordlists from `assets/` to `getFilesDir()` on first launch; all subsequent reads go through `getFilesDir()` |
| SQLite via Room | `AppDatabase` with 3 entities (`Engagement`, `Finding`, `ToolOperation`), 3 DAOs, `TypeConverters` — `db/` package |
| At least one implicit intent | (1) `ACTION_SEND` share in `EngagementActivity`; (2) `ACTION_SEND` share in `JwtFragment`; (3) `ACTION_VIEW` "Open in browser" in `HttpInspectorFragment`; (4) `ACTION_SEND` share in `HttpInspectorFragment` |
| Online JSON data source | crt.sh (`CrtShStep`) returns JSON; HIBP range API (`HibpChecker`) — both accessed via OkHttp |
| Custom colour theme | `colors.xml` (hacker green `#00E676`, severity palette); `themes.xml` overrides `colorPrimary`; dark-mode variant in `values-night/themes.xml` |

---

## Known limitations

- **crt.sh latency** — the API can take 10–30 s or time out under load; the app retries once and degrades gracefully to DNS-only enumeration with the bundled wordlist.
- **TLS validation** — OkHttp uses the system trust store; self-signed certs on target hosts will cause connection errors.
- **Sensitive path scan** — uses HTTPS only; HTTP-only targets will fail silently per path.
- **HIBP** — requires internet access; can be disabled in Settings.
- **No signing config** — release builds are unsigned; use the debug APK for submission.
