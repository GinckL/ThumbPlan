package com.ginckl.thumbplan

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.ginckl.thumbplan.data.local.entity.PlanTaskEntity
import com.ginckl.thumbplan.di.RepositoryEntryPoint
import com.ginckl.thumbplan.services.ToneSynth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real, system-level alarm test: the task fires while the app is in the background and we verify what the
 * user would experience (music, full-screen page, what happens after the swipe).
 */
@RunWith(AndroidJUnit4::class)
class AlarmE2ETest {
    @get:Rule val grant: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
    @get:Rule val fresh = FreshAppRule()

    private val inst = InstrumentationRegistry.getInstrumentation()
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()
    private val repo get() = RepositoryEntryPoint.repository(ctx)
    private val settings get() = RepositoryEntryPoint.settings(ctx)
    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(inst)
        // "Display over other apps" is what lets the page pop up while the phone is in use.
        inst.uiAutomation.executeShellCommand("appops set ${ctx.packageName} SYSTEM_ALERT_WINDOW allow").close()
        device.wakeUp()
        device.pressHome()
    }

    private fun serviceRunning(): Boolean {
        @Suppress("DEPRECATION")
        return (ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(100).any { it.service.className.endsWith("AlarmService") }
    }

    private fun alarmPlaying(): Boolean =
        (ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager).activePlaybackConfigurations
            .any { it.audioAttributes.usage == AudioAttributes.USAGE_ALARM }

    private fun scheduleAlarm(title: String, inSeconds: Int, reminders: String = ""): Long = runBlocking {
        repo.createUser("Tester")
        val music = ToneSynth.fileFor(ctx, "chime")!!.absolutePath // stands in for "the music the user attached"
        repo.saveTask(
            PlanTaskEntity(
                userId = 0, title = title, scheduledTime = System.currentTimeMillis() + inSeconds * 1000L,
                audioPath = music, rewardCoins = 50, reminderIntervals = reminders
            )
        )
    }

    private fun swipeToComplete() {
        val thumb = device.wait(Until.findObject(By.desc("Swipe to complete")), 5000)
        assertNotNull("swipe thumb not found", thumb)
        val b = thumb.visibleBounds
        device.swipe(b.centerX(), b.centerY(), device.displayWidth - 30, b.centerY(), 40)
    }

    @Test
    fun alarm_playsChosenMusic_opensFullScreenPage_andSwipeOpensTheApp() {
        settings.setOpenAppAfterSwipe(true)
        scheduleAlarm("E2E wake up", inSeconds = 8)

        // the page opens by itself, no notification tap needed
        assertTrue("alarm page did not open on its own", device.wait(Until.hasObject(By.text("E2E wake up")), 40_000))
        assertTrue("alarm service should be running", serviceRunning())
        assertTrue("the music should be playing", alarmPlaying())
        assertNotNull(device.findObject(By.text("Swipe to complete")))

        swipeToComplete()

        // default setting: the app opens and celebrates
        assertTrue("app should open after the swipe", device.wait(Until.hasObject(By.text("Task completed!")), 10_000))
        assertFalse("music must stop after the swipe", alarmPlaying())
        assertFalse("service must stop after the swipe", serviceRunning())
        assertEquals(50, runBlocking { repo.user.first()!!.totalCoins })
        device.findObject(By.text("Great!")).click()
    }

    @Test
    fun alarm_withOpenAppTurnedOff_justFinishes() {
        settings.setOpenAppAfterSwipe(false)
        scheduleAlarm("E2E quiet swipe", inSeconds = 8)

        assertTrue(device.wait(Until.hasObject(By.text("E2E quiet swipe")), 40_000))
        assertTrue(alarmPlaying())
        swipeToComplete()

        assertTrue("alarm page should close", device.wait(Until.gone(By.text("E2E quiet swipe")), 10_000))
        // ...and ThumbPlan must NOT come to the front
        assertFalse(device.wait(Until.hasObject(By.text("Task completed!")), 3_000))
        assertTrue("ThumbPlan must stay in the background", device.currentPackageName != ctx.packageName)
        assertFalse(alarmPlaying())
        assertEquals(50, runBlocking { repo.user.first()!!.totalCoins })
        settings.setOpenAppAfterSwipe(true)
    }

    @Test
    fun snooze_silencesNow_andRingsAgain() {
        settings.setSnoozeMinutes(5)
        val id = scheduleAlarm("E2E snooze", inSeconds = 8)
        assertTrue(device.wait(Until.hasObject(By.text("E2E snooze")), 40_000))
        device.findObject(By.textContains("Snooze")).click()
        assertTrue(device.wait(Until.gone(By.text("E2E snooze")), 10_000))
        assertFalse(alarmPlaying())
        // the task is still pending and a snooze alarm exists for it
        val t = runBlocking { repo.getTask(id)!! }
        assertEquals("pending", t.status)
        val dump = device.executeShellCommand("dumpsys alarm")
        assertTrue("snooze alarm should be armed", dump.contains(ctx.packageName) && dump.contains("AlarmReceiver"))
    }

    @Test
    fun withoutOverlayPermission_musicStillPlays_andTheNotificationOpensThePage() {
        inst.uiAutomation.executeShellCommand("appops set ${ctx.packageName} SYSTEM_ALERT_WINDOW deny").close()
        scheduleAlarm("E2E no overlay", inSeconds = 8)
        val deadline = System.currentTimeMillis() + 40_000
        while (!alarmPlaying() && System.currentTimeMillis() < deadline) Thread.sleep(500)
        assertTrue("the music must play even if the page can't pop up", alarmPlaying())
        if (!device.hasObject(By.text("Swipe to complete"))) {
            device.openNotification()
            assertTrue("fallback notification missing", device.wait(Until.hasObject(By.textContains("E2E no overlay")), 10_000))
            device.findObject(By.textContains("E2E no overlay")).click()
        }
        assertTrue(device.wait(Until.hasObject(By.text("Swipe to complete")), 10_000))
        device.findObject(By.textContains("Dismiss")).click()
        assertTrue(device.wait(Until.gone(By.text("Swipe to complete")), 10_000))
        assertFalse("dismiss must silence the alarm", alarmPlaying())
        assertFalse(serviceRunning())
    }

    @Test
    fun reminder_isANotification_butTheMainAlarmIsNot() {
        // reminder 1 minute before; the task is 65s away -> the reminder fires after ~5s
        scheduleAlarm("E2E reminder", inSeconds = 65, reminders = "1")
        device.openNotification()
        assertTrue("reminder notification should appear", device.wait(Until.hasObject(By.textContains("Starts in 1 minute")), 30_000))
        // at this point no alarm page / service yet
        assertFalse(serviceRunning())
        device.pressHome()
    }
}
