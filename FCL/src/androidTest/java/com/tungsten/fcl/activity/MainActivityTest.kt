package com.tungsten.fcl.activity

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tungsten.fclauncher.utils.FCLPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Kiểm chứng việc tái cấu trúc ViewPager2 màn hình chính và cử chỉ 2 ngón của right menu:
 * 8 trang của UIManager, chuyển đổi switchUI, xác định vùng vuốt 2 ngón của right_menu và hiện/ẩn.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Before
    fun setup() {
        FCLPath.loadPaths(ApplicationProvider.getApplicationContext<Context>())
    }

    /** Khởi chạy MainActivity và đợi uiManager khởi tạo, block chạy ở luồng test */
    private fun withMainActivity(block: (MainActivity) -> Unit) {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.use {
            block(waitForUiManager(scenario))
        }
    }

    /** Thực thi thao tác UI ở luồng chính (exception assertion ném trong runOnMainSync sẽ lan sang luồng test) */
    private fun onMain(action: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action)
    }

    private fun waitForUiManager(scenario: ActivityScenario<MainActivity>): MainActivity {
        val deadline = SystemClock.uptimeMillis() + 15000
        var activity: MainActivity? = null
        while (SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity { activity = it }
            val a = activity
            if (a != null) {
                var ready = false
                InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    // Không thể dùng ::isInitialized cho thuộc tính lateinit từ bên ngoài, truy cập lỗi nghĩa là chưa khởi tạo
                    ready = runCatching {
                        a.uiManager
                        true
                    }.getOrDefault(false)
                }
                if (ready) return a
            }
            Thread.sleep(100)
        }
        fail("MainActivity uiManager chưa được khởi tạo")
        error("unreachable")
    }

    /** Đợi điều kiện ở luồng chính thỏa mãn (điều kiện đặt cuối cùng, hỗ trợ trailing lambda) */
    private fun waitForCondition(timeoutMs: Long = 8000, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            var result = false
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                result = condition()
            }
            if (result) return
            Thread.sleep(100)
        }
        fail("Đợi điều kiện quá thời gian")
    }

    /** Chèn chuỗi sự kiện vuốt ngang 2 ngón (không qua input chạm thật, phân phối trực tiếp) */
    private fun swipe(activity: MainActivity, startX: Float, endX: Float, y: Float) {
        val downTime = SystemClock.uptimeMillis()
        fun obtain(action: Int, pointerCount: Int, x: Float, eventTime: Long): MotionEvent {
            val props = Array(pointerCount) {
                MotionEvent.PointerProperties().apply { id = it }
            }
            val coords = Array(pointerCount) { i ->
                MotionEvent.PointerCoords().apply {
                    this.x = if (i == 0) x else x + 40f
                    this.y = if (i == 0) y else y - 40f
                    pressure = 1f
                    size = 1f
                }
            }
            return MotionEvent.obtain(
                downTime, eventTime, action, pointerCount,
                props, coords, 0, 0, 1f, 1f, 0, 0, 0, 0
            )
        }
        val pointerDown = MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        val pointerUp = MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        val steps = 12
        activity.dispatchTouchEvent(obtain(MotionEvent.ACTION_DOWN, 1, startX, downTime))
        activity.dispatchTouchEvent(obtain(pointerDown, 2, startX, downTime + 10))
        for (i in 1..steps) {
            val x = startX + (endX - startX) * i / steps
            activity.dispatchTouchEvent(obtain(MotionEvent.ACTION_MOVE, 2, x, downTime + 20 + i * 16L))
        }
        activity.dispatchTouchEvent(obtain(pointerUp, 2, endX, downTime + 250))
        activity.dispatchTouchEvent(obtain(MotionEvent.ACTION_UP, 1, endX, downTime + 260))
    }

    @Test
    fun uiManagerHasAllEightUIs() {
        withMainActivity { activity ->
            onMain {
                val ui = activity.uiManager
                assertNotNull(ui.mainUI)
                assertNotNull(ui.manageUI)
                assertNotNull(ui.downloadUI)
                assertNotNull(ui.controllerUI)
                assertNotNull(ui.multiplayerUI)
                assertNotNull(ui.settingUI)
                assertNotNull(ui.accountUI)
                assertNotNull(ui.versionUI)
                // Trang ban đầu là màn hình chính
                assertEquals(ui.mainUI, ui.currentUI)
                // Right menu hiển thị ban đầu
                assertEquals(View.VISIBLE, activity.binding.rightMenu.visibility)
            }
        }
    }

    @Test
    fun switchUIChangesCurrentUI() {
        withMainActivity { activity ->
            val ui = activity.uiManager
            onMain {
                assertEquals(ui.mainUI, ui.currentUI)
                ui.switchUI(ui.settingUI)
            }
            waitForCondition { ui.currentUI === ui.settingUI }
            onMain { ui.switchUI(ui.mainUI) }
            waitForCondition { ui.currentUI === ui.mainUI }
        }
    }

    @Test
    fun twoFingerSwipeOutsideRightMenuDoesNotToggle() {
        withMainActivity { activity ->
            onMain {
                val width = activity.binding.root.width
                val y = activity.binding.root.height * 0.5f
                val before = activity.binding.rightMenu.visibility
                // Điểm bắt đầu ở bên trái màn hình (ngoài vùng right_menu), vuốt trái không kích hoạt
                swipe(activity, startX = width * 0.1f, endX = width * 0.01f, y = y)
                assertEquals(before, activity.binding.rightMenu.visibility)
            }
        }
    }

    @Test
    fun twoFingerSwipeInRightMenuToggles() {
        withMainActivity { activity ->
            onMain {
                val width = activity.binding.root.width
                val y = activity.binding.root.height * 0.5f
                // Hiển thị ban đầu: vuốt phải trong vùng sẽ ẩn
                swipe(activity, startX = width * 0.9f, endX = width * 0.99f, y = y)
            }
            waitForCondition { activity.binding.rightMenu.visibility == View.GONE }
            onMain {
                val width = activity.binding.root.width
                val y = activity.binding.root.height * 0.5f
                // Sau khi ẩn, vuốt trái trong vùng sẽ hiện lại
                swipe(activity, startX = width * 0.9f, endX = width * 0.7f, y = y)
                assertEquals(View.VISIBLE, activity.binding.rightMenu.visibility)
                // Khi đã hiển thị, vuốt trái tiếp tục giữ trạng thái hiển thị
                swipe(activity, startX = width * 0.9f, endX = width * 0.7f, y = y)
                assertEquals(View.VISIBLE, activity.binding.rightMenu.visibility)
            }
        }
    }
}
