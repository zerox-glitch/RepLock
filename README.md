# RepLock 🤘📱

A screen-time blocker that locks distracting apps (Instagram, TikTok, YouTube, Reddit, …)
until you complete a set number of **verified pushups via the front camera**.

> "I made an app that forces me to do pushups before I can scroll."

## How it works

1. **Pick the apps** you want to block (app picker with icons + toggles).
2. **Open a blocked app** → a full-screen overlay slams on top of it.
3. **Do your pushups** in front of the front camera. ML Kit pose detection counts
   reps in real time with a huge counter, a progress ring, and a live skeleton
   (green = good form, red = out of frame / asymmetric arms).
4. **Hit the rep target** → the overlay dismisses and the app opens for a limited
   window (default 5 minutes), then re-locks.

## Features (MVP)

- ✅ Onboarding with sequential permission rationale screens
  (Camera → Usage Stats → Display over apps → Notifications)
- ✅ App picker: all installed apps, icons, toggles, search
- ✅ Foreground service polling `UsageStatsManager.queryEvents()` every 500 ms
- ✅ Full-screen blocking overlay with CameraX + ML Kit pose detection
- ✅ Rep counter for **pushups and squats** (see logic below), with an animated exercise demo
  and a camera-angle guide in the overlay
- ✅ Camera-angle guide (which side should face the camera) + looping animated demo of the exercise
- ✅ Unlock window logic, persisted across reboots (Room)
- ✅ Settings: rep target (default 10), unlock window (default 5 min), difficulty
- ✅ Stats: reps today, reps this week, streak, time earned back
- ✅ Hardcoded paywall stub (free: 3 unlocks/day, 1 blocked app)
- 🚧 v2: deeper stats/history (per-exercise breakdown)
- 🚧 v3: social features

## Pose detection logic (critical)

- ML Kit `PoseDetector` in **STREAM_MODE** (`PoseDetectorOptions.STREAM_MODE`).
- Only **every 3rd camera frame** is processed (battery saving), and frames are
  dropped while a previous detection is in flight.
- Elbow angle = shoulder–elbow–wrist angle (degrees). Arms straight ≈ 180°,
  elbows bent at the bottom ≈ ≤ 90°.
- A rep counts when **both** elbow angles go **> 160° → < 90° → > 160°**.
- **≥ 800 ms** between counted reps (anti-cheat).
- **Both arms must stay within 30°** of each other — waving one arm doesn't count;
  the state machine freezes while asymmetric.
- Visual feedback: **green** skeleton when form is detected, **red** when the user
  is out of frame or asymmetric. Haptic tick on every rep.
- **Squats** use the same state machine on the **knee angle** (hip–knee–ankle):
  standing **> 150°** → deep squat **< 100°** → standing **> 150°**. Same 800 ms
  anti-cheat and 30° left/right symmetry rule (both legs).
- **Both** mode counts a rep of either exercise toward the target (each exercise
  has its own counter; a rep from either increments the shared count).
- The overlay shows a **camera-angle guide** (side view — profile facing the
  camera; phone placement hints) and a collapsible **animated demo** of the
  selected exercise (stick figure looping through one rep).

The counting logic lives in a pure, unit-tested class:
[`app/src/main/java/com/replock/domain/RepCounter.kt`](app/src/main/java/com/replock/domain/RepCounter.kt)
with tests in
[`app/src/test/java/com/replock/domain/RepCounterTest.kt`](app/src/test/java/com/replock/domain/RepCounterTest.kt).

## App blocking logic (critical)

- `LockMonitorService` (foreground service, `specialUse` type) polls
  `UsageStatsManager.queryEvents()` every **500 ms** for
  `MOVE_TO_FOREGROUND` / `ACTIVITY_RESUMED` events, with a `queryUsageStats()`
  fallback so the lock also re-engages when the unlock window expires while you
  sit inside a blocked app.
- When the foreground package is on the blocklist **and** its unlock window has
  expired → launches `BlockOverlayActivity` with `FLAG_ACTIVITY_NEW_TASK`.
- The overlay relies on the **SYSTEM_ALERT_WINDOW** grant (a normal activity
  drawn over other apps).
- After reps are completed, an unlock-until timestamp is stored in Room.
  The app is **not** technically unlocked — the overlay is just suppressed for
  the window. Timestamps persist across reboots; `BootReceiver` restarts the
  service after boot.

## Permissions

| Permission | How it's requested |
|---|---|
| `CAMERA` | Runtime dialog (onboarding) |
| `PACKAGE_USAGE_STATS` | `Settings.ACTION_USAGE_ACCESS_SETTINGS` (onboarding) |
| `SYSTEM_ALERT_WINDOW` | `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` (onboarding) |
| `POST_NOTIFICATIONS` | Runtime dialog on API 33+ (onboarding) |
| `FOREGROUND_SERVICE` (+ `SPECIAL_USE`) | Manifest; persistent notification keeps the service alive |

## Monetization

- **Free:** 3 unlocks per day, 1 blocked app.
- **Pro ($9.99/mo or $29.99/yr, 3-day free trial):** unlimited unlocks, unlimited
  blocked apps, custom rep targets, squats mode, stats history.
- The paywall is a **hardcoded stub** for the MVP: tapping "Start 3-day free
  trial" flips a local `isPro` flag. Wire **RevenueCat** or **Play Billing** in
  `OverlayViewModel.purchasePro()` and `PaywallScreen` when ready.

## Build

Requirements: JDK 17, Android SDK (platform 35, build-tools), then:

```bash
./gradlew :app:testDebugUnitTest   # unit tests
./gradlew :app:assembleDebug       # APK at app/build/outputs/apk/debug/app-debug.apk
```

A GitHub Actions workflow (`.github/workflows/build-apk.yml`) builds the APK and
runs the tests on every push to `arena/4546cc7a-replock`, publishing the APK to
the `builds/replock-apk` branch and as a workflow artifact.

## Install on a physical device (via ADB)

> Emulators don't have reliable camera pose detection — test on a real phone.

```bash
# The latest CI-built APK is also committed to the builds/replock-apk branch:
git fetch origin builds/replock-apk
git checkout origin/builds/replock-apk -- RepLock-debug.apk

adb install -r RepLock-debug.apk     # or: adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## First-run checklist

1. Launch RepLock → finish onboarding (grant all 4 permissions).
2. Go to **Blocked Apps** → toggle on e.g. Instagram.
3. Make sure **BLOCKING ACTIVE** is on (Home screen).
4. Open Instagram → the lock overlay should appear within ~1 s.
5. Put the phone on the floor, camera facing you, and do 10 pushups.
6. Counter hits 10 → green **UNLOCKED** → Instagram opens for 5 minutes.
7. Close and reopen Instagram after the window → it re-locks.
8. Use your 3 free unlocks → the 4th attempt shows the paywall.

## Architecture

- **Language:** Kotlin, **UI:** Jetpack Compose (Material 3, dark theme,
  electric green `#00FF88` / red `#FF3B30`)
- **Architecture:** MVVM + repository layer
  (`RepLockRepository` over Room + DataStore)
- **Camera:** CameraX (`camera-camera2`, `camera-lifecycle`, `camera-view`)
- **Pose detection:** ML Kit (`com.google.mlkit:pose-detection`, bundled model —
  works offline on device)
- **Storage:** Room (blocked apps, rep sessions, unlock events) + DataStore
  (settings)

```
app/src/main/java/com/replock/
├── RepLockApp.kt               # Application: DI-ish singletons, notification channel, service autostart
├── MainActivity.kt             # Compose host
├── data/                       # Room (AppDatabase, entities, DAOs), SettingsDataStore, models
├── domain/RepCounter.kt        # Pure pushup rep-counting state machine (unit-tested)
├── repository/                 # RepLockRepository — single layer over Room/DataStore
├── service/                    # LockMonitorService (usage-stats polling), BootReceiver
├── overlay/                    # BlockOverlayActivity + OverlayViewModel + PoseAnalyzer (ML Kit)
├── ui/
│   ├── theme/                  # Dark "gym" theme: #00FF88 green, #FF3B30 red
│   ├── navigation/             # NavHost + bottom bar
│   ├── onboarding/             # Mechanic explanation + sequential permission screens
│   ├── home/                   # Status, permissions, today, blocklist shortcut
│   ├── picker/                 # Installed-app picker with icons + toggles
│   ├── settings/               # Rep target, unlock window, difficulty, Pro
│   ├── stats/                  # Reps today/week, streak, time earned
│   ├── paywall/                # Hardcoded paywall stub
│   └── components/             # RepCounterDisplay (huge counter + progress ring), StatCard
└── util/                       # PermissionUtils, Haptics
```

## Launch plan

- **TikTok (15 s):** hook in the first 3 seconds.
  - 0–3 s: open Instagram → red lock overlay slams in ("INSTAGRAM IS LOCKED").
  - 3–8 s: "Do 10 pushups to unlock" — phone on the floor, POV pushups.
  - 8–13 s: counter hits 10/10 → green UNLOCKED → Instagram opens.
  - 13–15 s: text overlay: "I made an app that forces me to do pushups before I can scroll."
  - Caption: "RepLock — the screen blocker that makes you earn your screen time 💪📱
    #android #indieapp #fitness #nosurf #getdisciplined #buildinpublic #appdev"
- **Reddit:** r/AndroidApps, r/getdisciplined, r/nosurf, r/SideProject
  (title suggestion: "I built RepLock — an app that locks Instagram/TikTok until you
  do verified pushups with your camera [APK]")
- **Pricing:** $9.99/month with a 3-day free trial ($29.99/year).

## Notes

- `QUERY_ALL_PACKAGES` is declared so the picker can list all apps when sideloaded
  via ADB. Google Play restricts it — for a Play release, migrate the picker to
  package-visibility filtering or the `QUERY_ALL_PACKAGES` declaration review.
- Release builds keep minification off for the MVP (ML Kit / CameraX / Room
  reflection paths). Ship debug APKs for device testing.
