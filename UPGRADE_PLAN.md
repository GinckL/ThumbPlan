# ThumbPlan — Audit & Upgrade Plan (v1.0 → v1.1)

## 1. What the app is
ThumbPlan is an Android (Kotlin / Jetpack Compose / Room / Hilt) **gamified daily planner**.
You create tasks with a time, colour, photo, category and reward; at the scheduled time a
**full-screen alarm** wakes the phone, you swipe to complete, and you earn **coins** and keep a **streak**.

## 2. Audit of v1.0 (what I found)

### Working
- Task creation (title, description, date/time, category → coins, colour, photo), Room DB, dashboard list with swipe to complete / delete.
- Exact alarm → full-screen notification → AlarmActivity → swipe → coins + streak written to DB.
- Light/dark theme toggle, glass bottom navigation, rewards screen with coin history, invite/share.

### Broken logic / bugs
| # | Problem | Where |
|---|---------|-------|
| B1 | Snooze rebuilt the task from scratch → lost reward, image, colour; used `userId = 0` | AlarmActivity |
| B2 | Alarm data travelled in Intent extras (stale after edits); no recovery after **reboot** (no boot receiver) → alarms silently lost | AlarmScheduler |
| B3 | Reminder chips (15/10/5/1 min) were saved but **never scheduled** | TaskCreationSheet |
| B4 | Alarm made **no sound or vibration**; "custom sound" was stored as a temporary content-URI that can't be read later | Alarm / Sheet |
| B5 | `FLAG_ACTIVITY_CLEAR_TASK` on the alarm intent wiped the user's open app task | AlarmReceiver |
| B6 | `FollowUpWorker` never scheduled and no `HiltWorkerFactory` → would crash if it ever ran | Worker |
| B7 | Streak = "number of distinct days in the last 7" (UTC), not a real consecutive streak | ViewModel / DAO |
| B8 | Fake seeded user "Alex", 1250 coins, streak 5 for every new install | ViewModel |
| B9 | "Done" tab always empty (completed tasks filtered out first) | Dashboard |
| B10 | Week / Weekend chips did nothing (no recurrence at all) | Sheet |
| B11 | Date picker returns UTC millis, converted with local zone → wrong day in UTC-negative zones | Sheet |
| B12 | Validation hack: blank title toggled the *coin* error flag; past times accepted | Sheet |
| B13 | Completion screen saved nothing: note ignored, "Near Home" hard-coded fake location | CompletionModal |
| B14 | Coins farmable: tasks scheduled weeks ahead could be completed immediately | ViewModel |
| B15 | Alarm / swipe / typography hard-coded dark colours → unreadable in light mode; AlarmActivity ignored the saved theme | many |
| B16 | Greeting always "Good morning"; tasks not tappable (no details / edit); delete had no undo | Dashboard |
| B17 | Settings switches faked state (`!state`) instead of reading system state; exact-alarm settings screen was forced on **every** launch | Settings / Main |
| B18 | Backup rules referenced a non-existent DB name; Android 14 full-screen-intent permission never checked | Manifest |
| B19 | Default Android-robot launcher icon; empty `coin_burst.json` (invalid Lottie) + unused Lottie / adaptive-suite dependencies | Res / Gradle |
| B20 | Release build: minify on but **unsigned** → not installable; missing keep rules | Gradle |

### Missing for a "premium" product
Onboarding, task editing & details, search, overdue state, real recurrence, levels / achievements /
weekly insights, alarm preferences (snooze length, vibration, sound), live permission health,
test-alarm button, tests, signed release.

## 3. Plan (executed)
1. **Foundation** – `TaskRepository` as the single source of truth (used by UI *and* alarm); DB migration 2→3 (`repeatMode`); `SettingsStore`; pure-Kotlin domain helpers (`Streaks`, `Recurrence`, `Progression`) with unit tests.
2. **Alarm engine** – DB-driven receiver, reminders, snooze, boot / time-change / permission-change rescheduling, sound + vibration (custom audio copied locally), notification channels, WorkManager follow-ups, full-screen-intent handling.
3. **Fix every bug above.**
4. **UI upgrade** – onboarding, progress-ring dashboard with search, day headers, overdue + repeat chips, task detail & edit sheet, undo snackbars; Rewards with level card, weekly chart, badges, named history; richer Settings; new launcher icon; themed alarm screen.
5. **Ship** – signed release APK (+ debug APK), install & run end-to-end on an emulator/device, collect into `dist/`.

## 4. Roadmap beyond v1.1
Cloud backup/sync & accounts (the entity already has google/apple/phone fields), home-screen widget,
coin shop (spend coins on themes / sounds), friends & referral rewards, JSON export, localisation (uz/ru), Wear OS.

## 5. Install & build
- **Install:** copy `dist/ThumbPlan-1.2.0-release.apk` to the phone (or `adb install -r dist/ThumbPlan-1.2.0-release.apk`) and allow "install unknown apps". The `.aab` is for Google Play.
- **Build:** `./gradlew :app:assembleRelease` (needs JDK 17+, e.g. Android Studio's JBR as `JAVA_HOME`). Signing config is read from `keystore.properties`.
- **Back up `keystore/` and `keystore.properties`** – without them you can't ship updates over this APK. Both are git-ignored.
- **Tests:** `./gradlew :app:testDebugUnitTest` (32 JVM tests) and `./gradlew :app:connectedDebugAndroidTest` (22 on-device UI / alarm / backup tests, needs an emulator or phone).
- **Verified on:** Android 17 (API 37) emulator, release (R8-minified) build: fresh install → onboarding → create task → reminder + exact alarm fire → alarm screen → swipe → coins/streak/rewards; v1.0 → v1.1 in-place upgrade (DB migration 2→3) keeps data.

## 6. v1.2.0 – what changed
- **Alarm behaviour (as requested):** reminders (30/15/10/5/1 min before, 10 min preselected) are normal notifications. At the exact time there is **no notification to tap**: a foreground service starts your chosen music (task audio → shop tone → phone tone) and the full-screen alarm page opens by itself. A silent ongoing notification is only the fallback entry point; the system never plays its own alarm sound any more.
- **After swiping:** the app opens and celebrates (default). Settings → Alarm → "Open the app after swiping" turns that off – you just carry on.
- Pop-up while using the phone needs *Display over other apps* (banner + Settings row guide the user); when locked/idle the full-screen intent does it.
- **Coin shop:** Double Coins, Streak Freeze (auto-covers a missed day), 5 app colour themes, 6 avatars, 5 synthesized alarm tones (+ preview). Spending never lowers level/achievements (lifetime coins).
- **Backup & sync:** export/import ZIP (tasks, photos, audio, coins, purchases, settings); folder sync (Google Drive/Dropbox/etc. via the system picker) with three-way conflict detection; restore from the welcome screen.
- **Tests:** 32 JVM + 22 instrumented (UI flows, repository, backup round-trip, real alarm E2E incl. music, swipe, snooze, reminder, no-overlay fallback).
