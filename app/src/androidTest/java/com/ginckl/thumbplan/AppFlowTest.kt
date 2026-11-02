package com.ginckl.thumbplan

import android.Manifest
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.ginckl.thumbplan.data.backup.BackupManager
import com.ginckl.thumbplan.data.backup.SyncStore
import com.ginckl.thumbplan.data.local.entity.PlanTaskEntity
import com.ginckl.thumbplan.data.repo.CompletionResult
import com.ginckl.thumbplan.data.repo.PurchaseResult
import com.ginckl.thumbplan.di.RepositoryEntryPoint
import com.ginckl.thumbplan.domain.ShopCatalog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import java.io.File

private val ctx: Context get() = ApplicationProvider.getApplicationContext()
private val repo get() = RepositoryEntryPoint.repository(ctx)
private val settings get() = RepositoryEntryPoint.settings(ctx)
private val entry get() = object {
    fun backup() = RepositoryEntryPoint.backup(ctx)
    fun syncStore() = RepositoryEntryPoint.syncStore(ctx)
}

/** Wipes the app (data, prefs, alarms) before every test so tests are independent. */
class FreshAppRule : TestWatcher() {
    override fun starting(description: Description?) {
        runBlocking { repo.resetAll() }
        entry.syncStore().disconnect()
    }
}

private fun pastTask(title: String, coins: Int = 50) = PlanTaskEntity(
    userId = 0, title = title, scheduledTime = System.currentTimeMillis() - 60_000, rewardCoins = coins
)

private suspend fun seedUser(name: String = "Tester") = repo.createUser(name)

@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val chain: RuleChain = RuleChain
        .outerRule(GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS))
        .around(FreshAppRule())
        .around(composeRule)

    private fun waitForText(text: String, timeoutMs: Long = 8000) {
        composeRule.waitUntil(timeoutMs) { composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun goTo(tab: String) {
        composeRule.onNodeWithText(tab).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun onboarding_thenDashboard() {
        waitForText("Welcome to ThumbPlan")
        composeRule.onNodeWithText("Get started").assertIsDisplayed() // disabled until a name is typed
        composeRule.onNodeWithText("What should we call you?").performTextInput("Azim")
        composeRule.onNodeWithText("Get started").performClick()
        waitForText("Azim")
        composeRule.onNodeWithText("Today's progress").assertIsDisplayed()
    }

    @Test
    fun createTaskThroughTheUi_showsUpInAll() {
        runBlocking { seedUser() }
        waitForText("Tester")
        composeRule.onNodeWithTag("fab_add").performClick()
        waitForText("New task")
        composeRule.onNodeWithText("e.g. Read a book").performTextInput("Water the plants")
        composeRule.onNodeWithText("Save task").performScrollTo().performClick()
        waitForText("Task added")
        goTo("All")
        waitForText("Water the plants")
    }

    @Test
    fun emptyTitle_isRejected() {
        runBlocking { seedUser() }
        waitForText("Tester")
        composeRule.onNodeWithTag("fab_add").performClick()
        waitForText("New task")
        composeRule.onNodeWithText("Save task").performScrollTo().performClick()
        waitForText("Give your task a name")
    }

    @Test
    fun completingTasks_earnCoins_andShopPurchaseSpendsThem() {
        runBlocking {
            seedUser()
            repeat(4) { i ->
                val id = repo.saveTask(pastTask("Task $i"))
                assertTrue(repo.completeTask(id) is CompletionResult.Completed)
            }
        }
        waitForText("200") // balance in the header
        goTo("Shop")
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("shop_item_chime"))
        // the item may sit right behind the floating nav bar: nudge the list so its button is really tappable
        composeRule.onNode(hasScrollAction()).performTouchInput { swipeUp(startY = height * 0.7f, endY = height * 0.4f) }
        composeRule.onNode(hasText("🪙 200") and hasAnyAncestor(hasTestTag("shop_item_chime"))).performClick()
        composeRule.waitUntil(8000) {
            composeRule.onAllNodes(hasText("Equipped") and hasAnyAncestor(hasTestTag("shop_item_chime"))).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals("chime", settings.tone.value) // bought tones are equipped straight away
        composeRule.onNode(hasText("Equipped") and hasAnyAncestor(hasTestTag("shop_item_chime"))).assertIsDisplayed()
        assertEquals(1, runBlocking { repo.inventory.first()["chime"] })
    }

    @Test
    fun settings_openAppAfterSwipe_toggleIsPersisted() {
        runBlocking { seedUser() }
        waitForText("Tester")
        goTo("Settings")
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("toggle_open_after_swipe"))
        assertTrue(settings.openAppAfterSwipe.value) // default: on
        composeRule.onNodeWithTag("toggle_open_after_swipe").performClick()
        composeRule.waitForIdle()
        assertEquals(false, settings.openAppAfterSwipe.value)
        composeRule.onNodeWithTag("toggle_open_after_swipe").performClick()
        composeRule.waitForIdle()
        assertEquals(true, settings.openAppAfterSwipe.value)
    }

    @Test
    fun shopScreen_listsAllSections() {
        runBlocking { seedUser() }
        waitForText("Tester")
        goTo("Shop")
        waitForText("Coin shop")
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("shop_item_double_coins"))
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("shop_item_sunset"))
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("shop_item_zen"))
    }

    @Test
    fun rewardsScreen_showsLevelAndAchievements() {
        runBlocking { seedUser(); val id = repo.saveTask(pastTask("One")); repo.completeTask(id) }
        waitForText("Tester")
        goTo("Rewards")
        waitForText("Level 1")
        waitForText("Achievements")
        waitForText("First Step")
    }
}

/** Pure repository / backup behaviour against the real database (no UI). */
@RunWith(AndroidJUnit4::class)
class RepositoryInstrumentedTest {
    @get:Rule val fresh = FreshAppRule()

    @Test
    fun completing_updatesCoinsStreakAndIsIdempotent(): Unit = runBlocking<Unit> {
        seedUser()
        val id = repo.saveTask(pastTask("A", 70))
        val r = repo.completeTask(id) as CompletionResult.Completed
        assertEquals(70, r.coins)
        assertEquals(1, r.streak)
        assertEquals(70, repo.user.first()!!.totalCoins)
        assertEquals(CompletionResult.AlreadyDone, repo.completeTask(id))
        assertEquals(70, repo.user.first()!!.totalCoins) // no double pay
    }

    @Test
    fun futureTask_cannotBeCompletedEarly(): Unit = runBlocking<Unit> {
        seedUser()
        val id = repo.saveTask(PlanTaskEntity(userId = 0, title = "Later", scheduledTime = System.currentTimeMillis() + 3 * 86_400_000L))
        assertEquals(CompletionResult.TooEarly, repo.completeTask(id))
        assertEquals(0, repo.user.first()!!.totalCoins)
    }

    @Test
    fun buying_checksBalance_andOwnership(): Unit = runBlocking<Unit> {
        seedUser()
        assertEquals(PurchaseResult.NotEnoughCoins, repo.buy("sunset"))
        repeat(6) { repo.completeTask(repo.saveTask(pastTask("t$it"))) } // 300 coins
        assertEquals(PurchaseResult.Bought("Sunset"), repo.buy("sunset"))
        assertEquals(0, repo.user.first()!!.totalCoins)
        assertEquals(PurchaseResult.AlreadyOwned, repo.buy("sunset"))
        assertEquals(PurchaseResult.AlreadyOwned, repo.buy("violet")) // free items can't be bought
        // spending doesn't lower lifetime earnings (level / badges)
        assertEquals(300, repo.lifetimeCoins.first())
        // the purchase is in the history as a negative entry
        assertTrue(repo.coinHistory.first().any { it.amount == -300 && it.label?.contains("Sunset") == true })
    }

    @Test
    fun doubleCoins_isConsumedByTheNextTask(): Unit = runBlocking<Unit> {
        seedUser()
        repeat(4) { repo.completeTask(repo.saveTask(pastTask("t$it"))) } // 200
        assertEquals(PurchaseResult.Bought("Double Coins"), repo.buy(ShopCatalog.BOOST))
        val first = repo.completeTask(repo.saveTask(pastTask("boosted", 50))) as CompletionResult.Completed
        assertEquals(100, first.coins)
        assertTrue(first.boosted)
        val second = repo.completeTask(repo.saveTask(pastTask("normal", 50))) as CompletionResult.Completed
        assertEquals(50, second.coins)
        assertEquals(false, second.boosted)
        assertEquals(0, repo.inventory.first()[ShopCatalog.BOOST] ?: 0)
    }

    @Test
    fun streakFreeze_isNotAppliedWithoutAGap(): Unit = runBlocking<Unit> {
        seedUser()
        repeat(5) { repo.completeTask(repo.saveTask(pastTask("t$it"))) }
        assertEquals(PurchaseResult.Bought("Streak Freeze"), repo.buy(ShopCatalog.FREEZE))
        assertEquals(0, repo.applyStreakFreezes()) // completed today -> nothing to cover
        assertEquals(1, repo.inventory.first()[ShopCatalog.FREEZE])
    }

    @Test
    fun repeatingTask_spawnsNextOccurrenceOnCompletion(): Unit = runBlocking<Unit> {
        seedUser()
        val id = repo.saveTask(pastTask("Daily thing").copy(repeatMode = "daily"))
        repo.completeTask(id)
        val all = repo.tasks.first().filter { it.title == "Daily thing" }
        assertEquals(2, all.size)
        assertEquals(1, all.count { it.status == "pending" && it.scheduledTime > System.currentTimeMillis() })
    }

    @Test
    fun backup_roundTrip_restoresEverything(): Unit = runBlocking<Unit> {
        seedUser("Backup Person")
        repeat(4) { repo.completeTask(repo.saveTask(pastTask("done$it"))) } // 200 coins
        repo.buy("chime")
        repo.saveTask(PlanTaskEntity(userId = 0, title = "Upcoming", scheduledTime = System.currentTimeMillis() + 86_400_000L, repeatMode = "weekly", reminderIntervals = "10,5"))
        settings.setSnoozeMinutes(15)
        val backup = entry.backup()
        val hashBefore = backup.localHash()
        val file = File(ctx.cacheDir, "roundtrip.zip")
        file.outputStream().use { backup.write(it) }
        assertTrue(file.length() > 0)

        // the manifest is readable on its own
        val info = file.inputStream().use { backup.readInfo(it) }!!
        assertEquals(hashBefore, info.hash)
        assertEquals(5, info.tasks)

        repo.resetAll()
        assertEquals(null, repo.user.first())
        file.inputStream().use { backup.restore(it) }

        assertEquals("Backup Person", repo.user.first()!!.name)
        assertEquals(0, repo.user.first()!!.totalCoins) // 200 earned - 200 spent
        assertEquals(5, repo.tasks.first().size)
        assertEquals(1, repo.inventory.first()["chime"])
        assertEquals(15, settings.snoozeMinutes.value)
        assertEquals(hashBefore, backup.localHash()) // identical fingerprint after the round trip
        assertEquals(200, repo.lifetimeCoins.first())
        file.delete()
    }

    @Test
    fun backup_rejectsGarbage_andKeepsData(): Unit = runBlocking<Unit> {
        seedUser("Keep Me")
        val backup = entry.backup()
        val bad = "definitely not a zip".toByteArray()
        val failed = runCatching { backup.restore(bad.inputStream()) }.isFailure
        assertTrue(failed)
        assertEquals("Keep Me", repo.user.first()!!.name) // nothing was wiped
        assertEquals(null, backup.readInfo(bad.inputStream()))
    }

    @Test
    fun backup_includesPhotosAndAudio(): Unit = runBlocking<Unit> {
        seedUser()
        val img = File(ctx.filesDir, "image_test.jpg").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val aud = File(ctx.filesDir, "audio_test.mp3").apply { writeBytes(byteArrayOf(9, 8, 7)) }
        repo.saveTask(PlanTaskEntity(userId = 0, title = "Media", scheduledTime = System.currentTimeMillis() + 86_400_000L, imagePath = img.absolutePath, audioPath = aud.absolutePath))
        val backup = entry.backup()
        val out = java.io.ByteArrayOutputStream().also { backup.write(it) }.toByteArray()
        repo.resetAll() // also deletes the media files
        assertTrue(!img.exists() && !aud.exists())
        backup.restore(out.inputStream())
        val t = repo.tasks.first().single()
        assertEquals(img.absolutePath, t.imagePath)
        assertEquals(aud.absolutePath, t.audioPath)
        assertEquals(listOf<Byte>(1, 2, 3, 4), img.readBytes().toList())
        assertEquals(listOf<Byte>(9, 8, 7), aud.readBytes().toList())
    }

    @Test
    fun tones_areGeneratedAsValidWavFiles() {
        for (id in com.ginckl.thumbplan.services.ToneSynth.TONES) {
            val f = com.ginckl.thumbplan.services.ToneSynth.fileFor(ctx, id)!!
            val head = f.readBytes().copyOf(12)
            assertEquals("RIFF", String(head, 0, 4))
            assertEquals("WAVE", String(head, 8, 4))
            assertTrue("$id too small", f.length() > 20_000)
        }
        assertEquals(null, com.ginckl.thumbplan.services.ToneSynth.fileFor(ctx, "system"))
    }
}
