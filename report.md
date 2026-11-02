# ThumbPlan: Comprehensive Code Audit & Gap Analysis Report

**Role:** Senior Staff Software Architect & Principal Code Auditor  
**Date:** October 16, 2026  
**Project:** ThumbPlan (Android / Jetpack Compose / Room / Hilt)

This report provides an exhaustive audit of the ThumbPlan codebase. It is divided into two parts: a granular inventory of incomplete tasks, technical debt, and missing features, followed by a step-by-step resolution strategy for each item to bring the application to a production-ready, visually polished state.

---

## Part 1: Comprehensive Improvement & Incomplete Task Inventory

### 1. Hardcoded Rewards Screen Data
- **Category:** Unfinished Feature / Placeholder
- **Location:** [RewardsScreen.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/rewards/RewardsScreen.kt#L48)
- **Description:** The total coins display is hardcoded to `"🪙 1,250"` instead of using the dynamic `currentUser?.totalCoins` value from the ViewModel.

### 2. Non-Functional Settings Toggles
- **Category:** Unfinished Feature / Stub
- **Location:** [SettingsScreen.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/settings/SettingsScreen.kt#L33-L34)
- **Description:** The "Notifications" and "Exact Alarm Permission" switches have empty `onCheckedChange` lambdas (`{}`) and do not interact with system settings or SharedPreferences.

### 3. Missing Lottie Animation Resource
- **Category:** Runtime Crash Risk / Missing Asset
- **Location:** [TaskCompletionModal.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/task/TaskCompletionModal.kt#L28)
- **Description:** The code references `R.raw.coin_burst`, but there is no `res/raw/coin_burst.json` file in the project. This will cause a compilation error or runtime crash.

### 4. Unimplemented Snooze Functionality
- **Category:** Unfinished Feature / Stub
- **Location:** [AlarmActivity.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/alarm/AlarmActivity.kt#L148)
- **Description:** The "Snooze" button in the alarm screen has an empty click listener (`{ /* Snooze */ }`).

### 5. Hardcoded Alarm Screen Metadata
- **Category:** Placeholder / Stub
- **Location:** [AlarmActivity.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/alarm/AlarmActivity.kt#L113-L126)
- **Description:** The category ("Health & Fitness") and time ("08:00") displayed in the alarm modal are hardcoded strings rather than being passed from the triggered `PlanTaskEntity`.

### 6. Missing DoneTaskEntity Persistence on Completion
- **Category:** Data Integrity Gap / Unfinished Feature
- **Location:** [MainViewModel.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/MainViewModel.kt#L53-L59)
- **Description:** When a task is marked complete, the `PlanTaskEntity` status is updated, but no corresponding `DoneTaskEntity` is inserted into the database. This breaks the foreign key relationship intended for historical tracking and coin auditing.

### 7. Unimplemented FollowUpWorker Logic
- **Category:** Unfinished Feature / Stub
- **Location:** [FollowUpWorker.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/services/work/FollowUpWorker.kt#L23-L26)
- **Description:** The WorkManager worker checks for pending tasks but contains only a comment placeholder for triggering follow-up notifications. It is also never scheduled anywhere in the app.

### 8. Static Streak Count Logic
- **Category:** Unfinished Feature / Business Logic Gap
- **Location:** [MainViewModel.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/MainViewModel.kt#L43) & [UserEntity.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/data/local/entity/UserEntity.kt#L25)
- **Description:** The user is initialized with a hardcoded `streakCount = 5`. There is no logic to calculate, increment, or reset the daily streak based on completed tasks.

### 9. Dashboard "Done" Tab Filtering Bug
- **Category:** Logic Bug
- **Location:** [DashboardScreen.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/dashboard/DashboardScreen.kt#L63-L71)
- **Description:** The `filteredTasks` logic filters out all completed tasks (`tasks.filter { it.status != "completed" }`) before applying tab logic. Consequently, the "Done" tab (index 3) will always be empty.

### 10. Destructive Database Migration Strategy
- **Category:** Technical Debt / Data Loss Risk
- **Location:** [DatabaseModule.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/di/DatabaseModule.kt#L28)
- **Description:** The Room database is configured with `.fallbackToDestructiveMigration()`. Any future schema change will silently wipe all user data.

### 11. Unused Media3 and Image Cropper Dependencies
- **Category:** Technical Debt / APK Bloat
- **Location:** [build.gradle.kts](file:///C:/Users/kiril/Desktop/thumbplan/app/build.gradle.kts#L73-L79)
- **Description:** Media3 (ExoPlayer) and Android-Image-Cropper are declared as dependencies but are never imported or used in the source code.

### 12. Missing ProGuard Rules for Room & Hilt
- **Category:** Release Build Risk
- **Location:** [build.gradle.kts](file:///C:/Users/kiril/Desktop/thumbplan/app/build.gradle.kts#L33)
- **Description:** `isMinifyEnabled = true` is set for release builds, but there is no `proguard-rules.pro` file with keep rules for Room entities, Hilt components, or Kotlin serialization. This will cause runtime crashes in release APKs.

### 13. Hardcoded Light Theme Colors in Composables
- **Category:** Visual Inconsistency / Theming Debt
- **Location:** [LiquidGlassNavBar.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/components/LiquidGlassNavBar.kt#L163) & [TaskCompletionModal.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/task/TaskCompletionModal.kt)
- **Description:** Several composables use hardcoded `Color.White`, `Color.Black`, or static theme colors (`TextPrimary`, `Background`) instead of `MaterialTheme.colorScheme`, causing poor contrast in light mode.

### 14. Missing Edge-to-Edge Insets Handling in Bottom Sheet
- **Category:** UI/UX Bug
- **Location:** [TaskCreationSheet.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/task/TaskCreationSheet.kt#L185-L195)
- **Description:** The `ModalBottomSheet` content does not account for system navigation bar insets. On devices with gesture navigation, the "Save Task" button may be obscured by the home indicator.

### 15. No Input Validation Feedback on Task Creation
- **Category:** UX Gap
- **Location:** [TaskCreationSheet.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/task/TaskCreationSheet.kt#L538-L553)
- **Description:** If the title is blank, the save button silently does nothing. There is no visual error state, shake animation, or snackbar to inform the user why the task wasn't saved.

### 16. Missing Task Deletion Capability
- **Category:** Unfinished Feature
- **Location:** [DashboardScreen.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/dashboard/DashboardScreen.kt#L280-L310)
- **Description:** The `SwipeToDismissBox` only supports completing a task. There is no way for a user to delete or cancel a planned task from the dashboard.

### 17. Alarm Permission Not Requested at Runtime (API 31+)
- **Category:** Security / Functionality Gap
- **Location:** [MainActivity.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/MainActivity.kt#L43-L46)
- **Description:** The app only requests `POST_NOTIFICATIONS`. On Android 12+ (API 31), `SCHEDULE_EXACT_ALARM` requires explicit user permission via `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` intent, which is missing.

### 18. Hardcoded Greeting Time
- **Category:** UX Polish Gap
- **Location:** [strings.xml](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/res/values/strings.xml#L23) & [DashboardScreen.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/dashboard/DashboardScreen.kt#L118)
- **Description:** The greeting is permanently "Good morning," regardless of the actual time of day.

### 19. Missing Coin Transaction History UI
- **Category:** Unfinished Feature
- **Location:** [RewardsScreen.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/rewards/RewardsScreen.kt)
- **Description:** The rewards screen only shows totals. There is no list or graph showing how coins were earned (via `CoinEntity` or `DoneTaskEntity` history).

### 20. No Haptic Feedback on Interactions
- **Category:** UX Polish Gap
- **Location:** Multiple files ([SwipeToComplete.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/alarm/SwipeToComplete.kt), [DashboardScreen.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/dashboard/DashboardScreen.kt))
- **Description:** Critical interactions like swiping to complete, tapping the FAB, or completing a task lack haptic feedback, making the app feel less responsive.

### 21. FileUtil Stream Leak Risk
- **Category:** Performance / Resource Leak
- **Location:** [FileUtil.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/util/FileUtil.kt#L12-L18)
- **Description:** The `InputStream` and `FileOutputStream` are not wrapped in `use {}` blocks. If an exception occurs during `copyTo`, the streams may not close properly.

### 22. Missing Dark/Light Mode Persistence
- **Category:** UX Gap
- **Location:** [MainActivity.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/MainActivity.kt#L53)
- **Description:** The theme preference (`isDarkTheme`) is stored in `rememberSaveable`, which survives configuration changes but not app process death. It should be persisted to DataStore or SharedPreferences.

### 23. Inconsistent Iconography in Date Picker Trigger
- **Category:** Visual Polish Gap
- **Location:** [TaskCreationSheet.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/task/TaskCreationSheet.kt#L268)
- **Description:** The date/time selector uses `Icons.Default.Add` as a placeholder icon instead of a calendar or clock icon.

### 24. No Empty State for "Done" Tab
- **Category:** UX Gap
- **Location:** [DashboardScreen.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/dashboard/DashboardScreen.kt#L218-L240)
- **Description:** The generic "No Tasks Yet" empty state is shown for all empty tabs. The "Done" tab should have a specific message like "No completed tasks yet."

### 25. Missing Share/Invite Functionality
- **Category:** Unfinished Feature
- **Location:** [strings.xml](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/res/values/strings.xml#L68-L70)
- **Description:** String resources for sharing plans and inviting friends exist, but there is no corresponding UI or logic in the Rewards or Settings screens.

---

## Part 2: End-to-End Resolution Strategy

### 1. Resolve Hardcoded Rewards Screen Data
- **Root Cause:** The developer hardcoded the coin string during initial UI scaffolding and forgot to bind it to the ViewModel state.
- **Step-by-Step Implementation:**
  1. Open [RewardsScreen.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/rewards/RewardsScreen.kt).
  2. Locate the `Text` composable displaying `" 1,250"`.
  3. Replace the hardcoded string with `"🪙 ${currentUser?.totalCoins ?: 0}"`.
  4. Format the number with commas using `String.format("%,d", currentUser?.totalCoins ?: 0)` for better readability.
- **Verification:** Run the app, complete a task to earn coins, navigate to the Rewards tab, and confirm the number updates dynamically. Visually inspect light and dark modes.

### 2. Implement Functional Settings Toggles
- **Root Cause:** Settings UI was built as a static mockup without connecting to system APIs or local storage.
- **Step-by-Step Implementation:**
  1. Create a `SettingsViewModel` or extend `MainViewModel` to manage notification and alarm permission states.
  2. For **Notifications**: Use `NotificationManagerCompat.areNotificationsEnabled()` to read state. On toggle, launch `Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)`.
  3. For **Exact Alarms**: Use `AlarmManager.canScheduleExactAlarms()` (API 31+). On toggle, launch `Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)`.
  4. Update [SettingsScreen.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/settings/SettingsScreen.kt) to collect these states and wire the `onCheckedChange` lambdas.
- **Verification:** Toggle each switch and verify the app opens the correct system settings page. Return to the app and confirm the switch reflects the actual system state.

### 3. Add Missing Lottie Animation Resource
- **Root Cause:** The Lottie library was integrated, but the required JSON asset was never added to the resource directory.
- **Step-by-Step Implementation:**
  1. Download a suitable "coin burst" or "success checkmark" Lottie JSON file from LottieFiles.
  2. Create the directory `app/src/main/res/raw/` if it doesn't exist.
  3. Save the file as `coin_burst.json` in that directory.
  4. Alternatively, replace the Lottie animation with a Compose-native `AnimatedVisibility` + `Icon` celebration effect to reduce APK size.
- **Verification:** Build the project. Trigger the alarm completion flow and visually confirm the animation plays smoothly without crashing.

### 4. Implement Snooze Functionality
- **Root Cause:** The snooze button was added to the layout but the logic to reschedule the alarm was deferred.
- **Step-by-Step Implementation:**
  1. In [AlarmActivity.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/alarm/AlarmActivity.kt), inject `AlarmScheduler` via Hilt or pass it through intent extras.
  2. On snooze click, calculate a new time (e.g., `System.currentTimeMillis() + 10 * 60 * 1000`).
  3. Create a temporary `PlanTaskEntity` or reuse the existing ID with the new time and call `alarmScheduler.schedule()`.
  4. Call `finish()` to dismiss the current alarm screen.
- **Verification:** Trigger an alarm, tap Snooze, confirm the activity closes, and wait 10 minutes to verify the alarm fires again.

### 5. Dynamic Alarm Screen Metadata
- **Root Cause:** The alarm activity receives the task title but not the category or formatted time, leading to hardcoded placeholders.
- **Step-by-Step Implementation:**
  1. In [AlarmReceiver.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/services/alarm/AlarmReceiver.kt), extract `category` and `scheduledTime` from the intent extras (passed from `AlarmScheduler`).
  2. Pass these values to `AlarmActivity` via intent extras.
  3. In [AlarmActivity.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/alarm/AlarmActivity.kt), read these extras and pass them to `AlarmScreen`.
  4. Replace the hardcoded "Health & Fitness" and "08:00" with the dynamic values.
- **Verification:** Create tasks with different categories and times. Trigger their alarms and visually confirm the modal displays the correct metadata.

### 6. Persist DoneTaskEntity on Completion
- **Root Cause:** The completion flow only updates the plan task status, neglecting the historical audit trail designed in the schema.
- **Step-by-Step Implementation:**
  1. Inject `DoneTaskDao` (create if missing) into [MainViewModel.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/MainViewModel.kt).
  2. In `markTaskComplete`, after updating the `PlanTaskEntity`, create a `DoneTaskEntity(taskId = task.id, doneTime = System.currentTimeMillis())`.
  3. Insert the `DoneTaskEntity` into the database.
  4. Optionally, insert a corresponding `CoinEntity` linked to the `doneTaskId`.
- **Verification:** Complete a task, then inspect the Room database (via App Inspection) to confirm rows exist in `done_tasks` and `coins` tables.

### 7. Implement FollowUpWorker Logic
- **Root Cause:** WorkManager was added for background reliability, but the worker body was left as a stub.
- **Step-by-Step Implementation:**
  1. In [FollowUpWorker.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/services/work/FollowUpWorker.kt), inject `NotificationManager`.
  2. If a task is still pending past its scheduled time, build a high-priority notification reminding the user.
  3. In `AlarmSchedulerImpl`, when scheduling the main alarm, also enqueue a `OneTimeWorkRequest` for `FollowUpWorker` delayed by 15 minutes past the task time.
- **Verification:** Schedule a task, let the alarm fire, dismiss it without completing, and wait 15 minutes to confirm the follow-up notification appears.

### 8. Dynamic Streak Calculation
- **Root Cause:** Streak logic requires querying completed tasks by date, which was deferred during initial development.
- **Step-by-Step Implementation:**
  1. Add a query to `PlanTaskDao` or `DoneTaskDao`: `SELECT COUNT(DISTINCT date(doneTime)) FROM done_tasks WHERE doneTime > :sevenDaysAgo`.
  2. In `MainViewModel`, observe completed tasks and calculate the consecutive days with at least one completion.
  3. Update `UserEntity.streakCount` whenever a task is completed or the app resumes.
  4. Reset streak to 0 if a day is missed.
- **Verification:** Complete tasks on consecutive days (simulate by adjusting device date or DB timestamps) and verify the streak counter increments correctly on the Rewards screen.

### 9. Fix Dashboard "Done" Tab Filtering
- **Root Cause:** The pre-filtering logic excludes completed tasks globally, making the "Done" tab unreachable.
- **Step-by-Step Implementation:**
  1. In [DashboardScreen.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/dashboard/DashboardScreen.kt), remove the global `tasks.filter { it.status != "completed" }`.
  2. Update the `when (selectedTabIndex)` block:
     - Tab 0 (Today): `tasks.filter { it.status != "completed" && it.scheduledTime in today until tomorrow }`
     - Tab 1 (Upcoming): `tasks.filter { it.status != "completed" && it.scheduledTime >= tomorrow }`
     - Tab 2 (All): `tasks.filter { it.status != "completed" }`
     - Tab 3 (Done): `tasks.filter { it.status == "completed" }`
- **Verification:** Complete a task, switch to the "Done" tab, and confirm the completed task appears. Verify other tabs still exclude completed tasks.

### 10. Safe Database Migration Strategy
- **Root Cause:** `fallbackToDestructiveMigration()` was used for rapid prototyping but is unacceptable for production.
- **Step-by-Step Implementation:**
  1. Remove `.fallbackToDestructiveMigration()` from [DatabaseModule.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/di/DatabaseModule.kt).
  2. Create a `Migration_1_2` object implementing `Migration(1, 2)` with the appropriate `ALTER TABLE` SQL statements.
  3. Add `.addMigrations(Migration_1_2)` to the Room builder.
  4. For future versions, always increment the DB version and provide explicit migrations.
- **Verification:** Install v1, add data, upgrade to v2, and confirm data persists. Use `@Test` with `MigrationTestHelper` to automate migration validation.

### 11. Remove Unused Dependencies
- **Root Cause:** Libraries were added anticipating media playback and image cropping features that were never implemented.
- **Step-by-Step Implementation:**
  1. Open [build.gradle.kts](file:///C:/Users/kiril/Desktop/thumbplan/app/build.gradle.kts).
  2. Remove the Media3 (`androidx.media3:media3-exoplayer`, `media3-session`) and Image Cropper (`com.github.CanHub:Android-Image-Cropper`) implementation lines.
  3. Sync Gradle and rebuild.
- **Verification:** Build the release APK and compare the size before/after. Confirm no compilation errors occur.

### 12. Add ProGuard Keep Rules
- **Root Cause:** Minification is enabled but reflection-based libraries (Room, Hilt, Kotlin Serialization) need explicit keep rules.
- **Step-by-Step Implementation:**
  1. Create `app/proguard-rules.pro`.
  2. Add keep rules for Room entities (`-keep class com.ginckl.thumbplan.data.local.entity.** { *; }`), DAOs, and Hilt components.
  3. Reference this file in `build.gradle.kts` under `proguardFiles`.
- **Verification:** Build a release APK, install it, and exercise all features (DB operations, DI injection) to confirm no `NoSuchMethodError` or `ClassNotFoundException` crashes.

### 13. Migrate Hardcoded Colors to MaterialTheme
- **Root Cause:** Early composables used static color constants before the dynamic theming system was fully adopted.
- **Step-by-Step Implementation:**
  1. Search for `Color.White`, `Color.Black`, `TextPrimary`, `Background` across all UI files.
  2. Replace with `MaterialTheme.colorScheme.onSurface`, `MaterialTheme.colorScheme.background`, etc.
  3. In [LiquidGlassNavBar.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/components/LiquidGlassNavBar.kt), replace the `if (isDarkTheme) Color.White else Color.Black` logic with `MaterialTheme.colorScheme.onSurface`.
- **Verification:** Toggle between light and dark themes and visually inspect all screens for proper contrast and readability.

### 14. Handle Bottom Sheet Navigation Insets
- **Root Cause:** The bottom sheet content padding does not account for the system gesture area.
- **Step-by-Step Implementation:**
  1. In [TaskCreationSheet.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/task/TaskCreationSheet.kt), import `WindowInsets` and `navigationBars`.
  2. Add `.padding(WindowInsets.navigationBars.asPaddingValues())` to the inner `Column` or adjust the bottom padding dynamically.
  3. Alternatively, use `Modifier.navigationBarsPadding()` on the Save button container.
- **Verification:** Run on a device with gesture navigation, open the task sheet, and confirm the Save button is fully visible and tappable above the home indicator.

### 15. Add Input Validation Feedback
- **Root Cause:** The save logic silently fails on blank input, confusing the user.
- **Step-by-Step Implementation:**
  1. In [TaskCreationSheet.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/task/TaskCreationSheet.kt), add a `var isError by remember { mutableStateOf(false) }`.
  2. On save click, if `title.isBlank()`, set `isError = true` and trigger a shake animation or show a `Snackbar`.
  3. Bind `isError` to the `OutlinedTextField`'s `isError` parameter and supporting text.
- **Verification:** Attempt to save a task with an empty title and confirm visual feedback appears. Enter a title and confirm the error clears.

### 16. Implement Task Deletion via Swipe
- **Root Cause:** Only completion was wired to the swipe gesture; deletion was overlooked.
- **Step-by-Step Implementation:**
  1. In [DashboardScreen.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/dashboard/DashboardScreen.kt), modify the `SwipeToDismissBox` to support directional actions.
  2. Swipe StartToEnd → Complete. Swipe EndToStart → Delete.
  3. Update `backgroundContent` to show "Complete" (green) or "Delete" (red) based on `dismissState.dismissDirection`.
  4. Add `onDeleteTask` callback to `MainViewModel` calling `planTaskDao.deleteTaskById`.
- **Verification:** Swipe a task left to reveal the delete action, confirm it removes the task from the list and database.

### 17. Request Exact Alarm Permission at Runtime
- **Root Cause:** Android 12+ requires explicit user consent for exact alarms beyond manifest declaration.
- **Step-by-Step Implementation:**
  1. In [MainActivity.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/MainActivity.kt) or `SettingsScreen`, check `alarmManager.canScheduleExactAlarms()`.
  2. If false, launch `Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))`.
  3. Handle the result in `onActivityResult` or `ActivityResultLauncher` to update UI state.
- **Verification:** On Android 12+, install the app, attempt to schedule a task, and confirm the system permission dialog appears. Grant it and verify alarms fire precisely.

### 18. Dynamic Time-Based Greeting
- **Root Cause:** The greeting string is static, reducing personalization.
- **Step-by-Step Implementation:**
  1. In [DashboardScreen.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/dashboard/DashboardScreen.kt), calculate the current hour.
  2. Use a `when` block: 5-11 → "Good morning", 12-17 → "Good afternoon", 18-21 → "Good evening", else → "Good night".
  3. Replace the static `stringResource(R.string.dashboard_greeting)` with the dynamic string.
- **Verification:** Change device time or test at different hours to confirm the greeting updates appropriately.

### 19. Build Coin Transaction History UI
- **Root Cause:** The rewards screen only aggregates totals; users want to see earning history.
- **Step-by-Step Implementation:**
  1. Add a query to `CoinDao` or `DoneTaskDao` to fetch recent coin transactions ordered by date descending.
  2. Expose this as a `StateFlow` in `MainViewModel`.
  3. In [RewardsScreen.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/rewards/RewardsScreen.kt), add a `LazyColumn` below the streak card displaying transaction items (task name, date, +coins).
- **Verification:** Complete several tasks, navigate to Rewards, and confirm the history list populates with accurate entries.

### 20. Add Haptic Feedback to Interactions
- **Root Cause:** Tactile feedback was not integrated, making interactions feel flat.
- **Step-by-Step Implementation:**
  1. Import `LocalHapticFeedback` and `HapticFeedbackType` in relevant composables.
  2. In [SwipeToComplete.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/alarm/SwipeToComplete.kt), call `haptic.performHapticFeedback(HapticFeedbackType.LongPress)` when the swipe threshold is crossed.
  3. In [DashboardScreen.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/dashboard/DashboardScreen.kt), add light haptics on task completion and FAB tap.
- **Verification:** Enable system haptics, perform swipe and tap actions, and confirm tactile responses occur.

### 21. Fix FileUtil Stream Leaks
- **Root Cause:** Manual stream management without `use {}` blocks risks resource leaks on exceptions.
- **Step-by-Step Implementation:**
  1. In [FileUtil.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/util/FileUtil.kt), refactor `copyUriToLocal`.
  2. Wrap `context.contentResolver.openInputStream(uri)` and `FileOutputStream(file)` in `.use { input -> ... .use { output -> input.copyTo(output) } }`.
- **Verification:** Run Android Studio Inspector → Memory Profiler while picking images repeatedly. Confirm no steady memory growth or file descriptor leaks.

### 22. Persist Theme Preference
- **Root Cause:** `rememberSaveable` only survives configuration changes, not process death.
- **Step-by-Step Implementation:**
  1. Create a `ThemeDataStore` or use `SharedPreferences` to store a boolean `is_dark_theme`.
  2. In `MainViewModel`, expose `themeMode: StateFlow<Boolean>` reading from DataStore.
  3. In [MainActivity.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/MainActivity.kt), collect this flow and pass `onThemeToggle` to update DataStore.
- **Verification:** Toggle theme, kill the app process completely, relaunch, and confirm the selected theme persists.

### 23. Replace Placeholder Date Icon
- **Root Cause:** A generic "Add" icon was used temporarily during UI construction.
- **Step-by-Step Implementation:**
  1. In [TaskCreationSheet.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/task/TaskCreationSheet.kt), replace `Icons.Default.Add` with `Icons.Outlined.CalendarMonth` or `Icons.Outlined.Schedule`.
  2. Ensure the icon tint matches `MaterialTheme.colorScheme.primary`.
- **Verification:** Open the task creation sheet and visually confirm the date row displays a recognizable calendar/clock icon.

### 24. Tab-Specific Empty States
- **Root Cause:** A single generic empty state is reused for all tabs, causing confusion in the "Done" tab.
- **Step-by-Step Implementation:**
  1. In [DashboardScreen.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/dashboard/DashboardScreen.kt), parameterize the empty state composable.
  2. Pass different titles/subtitles based on `selectedTabIndex` (e.g., Tab 3 → "No Completed Tasks", "Finish a task to see it here.").
- **Verification:** Clear all tasks, switch between tabs, and confirm each tab shows contextually appropriate empty messaging.

### 25. Implement Share/Invite Flow
- **Root Cause:** String resources were prepared for a referral feature that was never built.
- **Step-by-Step Implementation:**
  1. In [RewardsScreen.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/rewards/RewardsScreen.kt), add a "Share Your Plan" card using the existing string resources.
  2. On click, launch an `Intent(Intent.ACTION_SEND)` with the user's `referralCode` from `UserEntity`.
  3. Optionally, deep-link back to the app with the referral code to award bonus coins to both parties.
- **Verification:** Tap the invite button, confirm the system share sheet opens with the correct referral message, and simulate a recipient installing the app to verify coin attribution logic.

---

## Visual Overhaul Recommendations

To elevate ThumbPlan from a functional prototype to a premium productivity app, apply these visual enhancements alongside the functional fixes:

1. **Liquid Glass Refinement:** Enhance [LiquidGlassNavBar.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/components/LiquidGlassNavBar.kt) with `RenderEffect.createBlurEffect()` (API 31+) for true frosted glass, falling back to semi-transparent surfaces on older devices.
2. **Task Card Micro-interactions:** Add `animateItemPlacement()` and subtle scale/opacity transitions when tasks are completed or deleted in the LazyColumn.
3. **Coin Celebration:** When coins are awarded, animate a floating "+50 🪙" text from the task card to the top coin pill using `Animatable` and `graphicsLayer`.
4. **Consistent Corner Radii:** Audit all `RoundedCornerShape` usages against `Dimens.radiusSm/Md/Lg` to ensure visual rhythm. Currently, some cards use hardcoded `8.dp` or `14.dp`.
5. **Typography Hierarchy:** Increase contrast between `titleLarge` and `bodyLarge` in dense lists. Consider bumping task titles to `FontWeight.Bold` for faster scanning.
6. **Empty State Illustrations:** Replace plain text empty states with lightweight Lottie or vector illustrations to add personality.
7. **Dark Mode Depth:** In dark mode, introduce subtle elevation overlays (`surfaceTint`) on cards to create depth without relying on shadows, which are invisible on dark backgrounds.

By systematically addressing these 25 items and applying the visual polish guidelines, ThumbPlan will achieve production readiness, robust data integrity, and a delightful user experience.