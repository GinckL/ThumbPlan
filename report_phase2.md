# ThumbPlan: Phase 2 - Visibility & Logic Fixes Report

**Role:** Senior Staff Software Architect & Principal Code Auditor  
**Date:** October 16, 2026  
**Project:** ThumbPlan (Android / Jetpack Compose)

This report addresses the critical visibility issues in Light Mode for the Alarm and Task Completion screens, as well as the missing logic for coin updates and the "View Details" action.

---

## Part 1: Issue Inventory

### 1. Alarm Screen Invisible Elements (Light Mode)
- **Category:** Visual Bug / Theming
- **Location:** [AlarmActivity.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/alarm/AlarmActivity.kt) (`AlarmScreen` composable)
- **Description:** The screen uses hardcoded dark theme colors (`Background`, `SurfaceCard`, `TextPrimary`). In Light Mode, the background becomes white, but the text remains white/light gray, making the title, category, time, and motivational text completely invisible. Only the high-contrast `SwipeToComplete` button is visible.

### 2. Missing Task Image in Alarm Screen
- **Category:** Unfinished Feature
- **Location:** [AlarmActivity.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/alarm/AlarmActivity.kt#L98)
- **Description:** The "Hero Image Placeholder" is just a solid color box. It needs to load the actual `imagePath` from the task entity and display it in a 1:1 aspect ratio with rounded corners (circled-edge square).

### 3. Task Completion Modal Invisible Elements (Light Mode)
- **Category:** Visual Bug / Theming
- **Location:** [TaskCompletionModal.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/task/TaskCompletionModal.kt)
- **Description:** Similar to the Alarm Screen, this modal uses hardcoded dark colors (`Background`, `TextPrimary`, `SurfaceCard`). In Light Mode, the "Task Completed!" text, task title, note field, and location text are invisible against the white background.

### 4. "View Details" Button Non-Functional
- **Category:** Unfinished Feature
- **Location:** [TaskCompletionModal.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/task/TaskCompletionModal.kt#L153)
- **Description:** The `TextButton` for "View Details" has an empty onClick lambda `{ /* View Details */ }`. It needs to navigate the user back to the main app dashboard, ideally highlighting or scrolling to the completed task.

### 5. Coins Not Updating in Real-Time / Logic Gap
- **Category:** Logic Bug
- **Location:** [AlarmActivity.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/ui/features/alarm/AlarmActivity.kt) & [MainViewModel.kt](file:///C:/Users/kiril/Desktop/thumbplan/app/src/main/java/com/ginckl/thumbplan/MainViewModel.kt)
- **Description:** When a task is completed via the Alarm Activity swipe, the `TaskCompletionModal` shows "+50", but this completion event is not currently wired back to the `MainViewModel` to update the database and the user's total coin balance. The Alarm Activity operates in isolation.

---

## Part 2: End-to-End Resolution Strategy

### Fix 1 & 3: Resolve Light Mode Visibility (Alarm & Completion Screens)

**Root Cause:** Hardcoded color references (`Background`, `TextPrimary`) instead of using `MaterialTheme.colorScheme`.

**Step-by-Step Implementation Plan:**
1.  **AlarmActivity.kt (`AlarmScreen`):**
    *   Replace `Background` with `MaterialTheme.colorScheme.background`.
    *   Replace `SurfaceCard` with `MaterialTheme.colorScheme.surface`.
    *   Replace `TextPrimary` with `MaterialTheme.colorScheme.onBackground`.
    *   Replace `TextSecondary` with `MaterialTheme.colorScheme.onSurfaceVariant`.
    *   Replace `PrimaryAccent` with `MaterialTheme.colorScheme.primary`.
2.  **TaskCompletionModal.kt:**
    *   Apply the exact same `MaterialTheme.colorScheme` replacements as above.
    *   Ensure the `OutlinedTextField` uses `MaterialTheme.colorScheme.surface` for its container color so it's visible in both modes.

**Verification:** Toggle the app to Light Mode. Trigger an alarm. Verify all text (Title, Category, Time, "Small steps...", "Task Completed!", Note placeholder) is clearly visible in dark gray/black against the white background.

### Fix 2: Add 1:1 Rounded Task Image to Alarm Screen

**Root Cause:** Placeholder `Box` instead of `AsyncImage`.

**Step-by-Step Implementation Plan:**
1.  Pass the `imagePath` from the Intent to the `AlarmScreen` composable.
2.  In `AlarmScreen`, replace the Hero Image Placeholder `Box` with:
    ```kotlin
    if (!imagePath.isNullOrEmpty()) {
        AsyncImage(
            model = imagePath,
            contentDescription = "Task Image",
            modifier = Modifier
                .size(120.dp) // 1:1 aspect ratio
                .clip(RoundedCornerShape(24.dp)) // Circled-edge square
                .align(Alignment.CenterHorizontally),
            contentScale = ContentScale.Crop
        )
    } else {
        // Keep a styled placeholder if no image
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
        )
    }
    ```

**Verification:** Create a task with an image. Trigger the alarm. Verify the image appears centered above the title in a rounded square.

### Fix 4: Wire "View Details" Button

**Root Cause:** Empty lambda.

**Step-by-Step Implementation Plan:**
1.  The `TaskCompletionModal` is shown inside `AlarmActivity`. When "View Details" is clicked, we want to open the `MainActivity` and clear the alarm activity stack.
2.  Update the `TextButton` onClick:
    ```kotlin
    TextButton(onClick = {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
        // The onDismiss (finish()) will also be called or handled by the new task flag
    })
    ```
    *(Note: We need to pass `LocalContext.current` or handle this via the `onDismiss` callback passed from `AlarmActivity`)*.
    *Better approach:* Add an `onViewDetails: () -> Unit` parameter to `TaskCompletionModal`. In `AlarmActivity`, implement it to launch `MainActivity` with `FLAG_ACTIVITY_CLEAR_TOP`.

**Verification:** Complete a task via alarm. Click "View Details". Verify the app opens to the Dashboard and the Alarm screen is closed.

### Fix 5: Wire Coin Updates from Alarm Completion

**Root Cause:** `AlarmActivity` does not have access to `MainViewModel` or the DAOs to update the database.

**Step-by-Step Implementation Plan:**
1.  **Inject DAOs into AlarmActivity:** Since `AlarmActivity` is not a Compose entry point with Hilt ViewModel easily available in the same way, we can use Hilt to inject the `PlanTaskDao` and `UserDao` directly into `AlarmActivity` using `@AndroidEntryPoint` and `@Inject`.
2.  **Update Logic on Swipe Complete:**
    *   In `AlarmActivity`, when `onComplete` is triggered (swipe finished):
    *   Launch a coroutine (using `lifecycleScope`).
    *   Fetch the task by `taskId`.
    *   Update task status to "completed".
    *   Fetch the local user.
    *   Update user's `totalCoins` (+ task.rewardCoins).
    *   Insert a record into `DoneTaskDao` and `CoinDao` (as per Phase 2 data layer setup).
    *   Cancel the alarm.
    *   *Then* show the `TaskCompletionModal`.
3.  **Pass dynamic coins to Modal:** Pass the actual `task.rewardCoins` to `TaskCompletionModal` instead of hardcoded `50`.

**Verification:** Check coin balance on Dashboard (e.g., 1250). Trigger alarm, swipe to complete. Verify modal shows correct coins (e.g., +75). Click Great/View Details. Return to Dashboard. Verify coin balance updated (e.g., 1325).

---

## Visual Overhaul Guidelines for Alarm & Completion

To make these screens feel premium and consistent with the Dashboard:

1.  **Glassmorphism Card:** Wrap the Alarm details in a `Surface` with `MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)` and a subtle border `Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(28.dp))`.
2.  **Typography:** Use `MaterialTheme.typography.displayMedium` for the Task Title to make it pop.
3.  **Completion Animation:** Ensure the Lottie animation (`coin_burst`) is visible. If the raw resource is missing, fallback to a large animated Icon (e.g., `Icons.Filled.CheckCircle` scaling up with a spring animation).
4.  **Button Polish:** The "Great!" button should use `MaterialTheme.colorScheme.primary` with `MaterialTheme.colorScheme.onPrimary` text, full width, 56dp height, and `RoundedCornerShape(16.dp)`.
