package com.tungsten.fcllibrary.component.theme

import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tungsten.fclauncher.utils.FCLPath
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

/**
 * Kiểm chứng việc tái cấu trúc ThemeEngine (singleton Java → object Kotlin + StateFlow):
 * Khởi tạo, xác định sáng/tối, cập nhật applyXxx và callback làm mới, đăng ký/hủy đăng ký listener.
 */
@RunWith(AndroidJUnit4::class)
class ThemeEngineTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        FCLPath.loadPaths(context)
        // Xóa chế độ theme trong tùy chọn launcher, tránh các test case ảnh hưởng lẫn nhau
        context.getSharedPreferences("launcher", Context.MODE_PRIVATE)
            .edit().remove("themeMode").apply()
    }

    @Test
    fun setupThemeEngineIsIdempotent() {
        ThemeEngine.setupThemeEngine(context)
        val first = ThemeEngine.getTheme()
        ThemeEngine.setupThemeEngine(context)
        // Gọi lần 2 không ghi đè (idempotent)
        assertEquals(first, ThemeEngine.getTheme())
        assertNotNull(first)
        // StateFlow giữ đồng bộ theme hiện tại
        runBlocking { assertEquals(first, ThemeEngine.theme.first()) }
    }

    @Test
    fun getThemeAvailableAfterSetup() {
        ThemeEngine.setupThemeEngine(context)
        assertNotNull(ThemeEngine.getTheme())
    }

    @Test
    fun isNightModeFollowsThemeModeSetting() {
        val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)
        // Ép buộc chế độ sáng
        prefs.edit().putInt("themeMode", 1).apply()
        assertFalse(ThemeEngine.isNightMode(context))
        // Ép buộc chế độ tối
        prefs.edit().putInt("themeMode", 2).apply()
        assertTrue(ThemeEngine.isNightMode(context))
        // Theo hệ thống (themeMode=0): lấy uiMode hệ thống
        prefs.edit().putInt("themeMode", 0).apply()
        val systemNight = (context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        assertEquals(systemNight, ThemeEngine.isNightMode(context))
        // Dọn dẹp
        prefs.edit().remove("themeMode").apply()
    }

    @Test
    fun applyColorUpdatesStateFlowAndNotifies() {
        ThemeEngine.setupThemeEngine(context)
        val notified = AtomicInteger(0)
        val view = View(context)
        ThemeEngine.registerEvent(view) { notified.incrementAndGet() }
        try {
            // Sau khi đăng ký registerEvent chạy ngay 1 lần (post bất đồng bộ, đợi luồng chính)
            waitForMainThread()
            assertEquals(1, notified.get())
            val oldColor = ThemeEngine.getTheme().color
            ThemeEngine.applyColor(0x123456)
            // Chạy ngay (handler.post bất đồng bộ — đợi luồng chính)
            waitForMainThread()
            assertEquals(0x123456, ThemeEngine.getTheme().color)
            assertTrue(notified.get() >= 2)
            assertNotEquals(oldColor, ThemeEngine.getTheme().color)
        } finally {
            ThemeEngine.unregisterEvent(view)
        }
    }

    @Test
    fun applyColor2AndColor2DarkUpdateIndependently() {
        ThemeEngine.setupThemeEngine(context)
        ThemeEngine.applyColor2(0x111111)
        ThemeEngine.applyColor2Dark(0x222222)
        waitForMainThread()
        val theme = ThemeEngine.getTheme()
        assertEquals(0x111111, theme.color2)
        assertEquals(0x222222, theme.color2Dark)
        // Màu chính không bị ảnh hưởng
        assertTrue(theme.color != 0x111111)
    }

    @Test
    fun unregisterEventStopsNotifications() {
        ThemeEngine.setupThemeEngine(context)
        val notified = AtomicInteger(0)
        val view = View(context)
        ThemeEngine.registerEvent(view) { notified.incrementAndGet() }
        // Chạy ngay 1 lần khi đăng ký (post bất đồng bộ, đợi luồng chính)
        waitForMainThread()
        assertEquals(1, notified.get())
        ThemeEngine.unregisterEvent(view)
        val count = notified.get()
        ThemeEngine.applyColor(ThemeEngine.getTheme().color + 1)
        waitForMainThread()
        assertEquals(count, notified.get())
    }

    @Test
    fun refreshListenersTriggeredOnApply() {
        ThemeEngine.setupThemeEngine(context)
        val notified = AtomicInteger(0)
        val listener = Runnable { notified.incrementAndGet() }
        ThemeEngine.addRefreshListener(listener)
        try {
            ThemeEngine.applyColor(ThemeEngine.getTheme().color + 1)
            waitForMainThread()
            assertTrue(notified.get() >= 1)
        } finally {
            ThemeEngine.removeRefreshListener(listener)
        }
    }

    @Test
    fun refreshThemeNotifiesBothChannels() {
        ThemeEngine.setupThemeEngine(context)
        val viewNotified = AtomicInteger(0)
        val listenerNotified = AtomicInteger(0)
        val view = View(context)
        ThemeEngine.registerEvent(view) { viewNotified.incrementAndGet() }
        val listener = Runnable { listenerNotified.incrementAndGet() }
        ThemeEngine.addRefreshListener(listener)
        try {
            // Mỗi cái chạy ngay 1 lần khi đăng ký (post bất đồng bộ, đợi luồng chính)
            waitForMainThread()
            assertEquals(1, viewNotified.get())
            ThemeEngine.refreshTheme()
            waitForMainThread()
            assertTrue(viewNotified.get() >= 2)
            assertTrue(listenerNotified.get() >= 1)
        } finally {
            ThemeEngine.unregisterEvent(view)
            ThemeEngine.removeRefreshListener(listener)
        }
    }

    @Test
    fun setCloseSkinModelAndAnimationSpeedUpdate() {
        ThemeEngine.setupThemeEngine(context)
        ThemeEngine.setCloseSkinModel(true)
        ThemeEngine.setAnimationSpeed(3)
        waitForMainThread()
        assertTrue(ThemeEngine.getTheme().closeSkinModel)
        assertEquals(3, ThemeEngine.getTheme().animationSpeed)
    }

    @Test
    fun getSystemAutoTintMatchesNightMode() {
        val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)
        prefs.edit().putInt("themeMode", 1).apply()
        assertEquals(Color.BLACK, ThemeEngine.getSystemAutoTint(context))
        prefs.edit().putInt("themeMode", 2).apply()
        assertEquals(Color.WHITE, ThemeEngine.getSystemAutoTint(context))
        prefs.edit().remove("themeMode").apply()
    }

    @Test
    fun getBackgroundFollowsNightMode() {
        ThemeEngine.setupThemeEngine(context)
        val theme = ThemeEngine.getTheme()
        val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)
        prefs.edit().putInt("themeMode", 1).apply()
        assertEquals(theme.backgroundLt, theme.getBackground(context))
        prefs.edit().putInt("themeMode", 2).apply()
        assertEquals(theme.backgroundDk, theme.getBackground(context))
        prefs.edit().remove("themeMode").apply()
    }

    /** Đợi luồng chính thực thi xong callback của handler.post */
    private fun waitForMainThread() {
        Thread.sleep(100)
    }
}
