package com.tungsten.fcllibrary.component.theme

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tungsten.fclauncher.utils.FCLPath
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Kiểm chứng việc tái cấu trúc ThemeData (thuộc tính fakefx → data class bất biến + lưu bền DataStore):
 * Tính giá trị dẫn xuất, lấy màu sáng/tối động, lưu bền khứ hồi.
 */
@RunWith(AndroidJUnit4::class)
class ThemeDataTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        FCLPath.loadPaths(context)
    }

    private fun themeData(
        color: Int = Color.parseColor("#7797CF"),
        color2: Int = Color.BLACK,
        color2Dark: Int = Color.WHITE
    ) = ThemeData(color, color2, color2Dark, false, false, 8,
        BitmapDrawableStub(), BitmapDrawableStub())

    /** Nền placeholder (test không phụ thuộc resource ảnh thật) */
    private class BitmapDrawableStub : android.graphics.drawable.BitmapDrawable() {
        override fun getIntrinsicWidth(): Int = 1
        override fun getIntrinsicHeight(): Int = 1
    }

    @Test
    fun derivedColorsFollowColor() {
        val base = Color.parseColor("#7797CF")
        val theme = themeData(color = base)
        // ltColor làm sáng, dkColor làm tối (khác màu chính, và khác nhau)
        assertNotEquals(base, theme.ltColor)
        assertNotEquals(base, theme.dkColor)
        assertNotEquals(theme.ltColor, theme.dkColor)
        // Quan hệ độ sáng: sau khi làm sáng thì sáng hơn, sau khi làm tối thì tối hơn
        val ltLum = luminance(theme.ltColor)
        val dkLum = luminance(theme.dkColor)
        val baseLum = luminance(base)
        assertTrue("ltColor phải sáng hơn màu chính", ltLum > baseLum)
        assertTrue("dkColor phải tối hơn màu chính", dkLum < baseLum)
    }

    @Test
    fun copyRecalculatesDerivedColors() {
        val theme = themeData(color = Color.parseColor("#7797CF"))
        val ltBefore = theme.ltColor
        val changed = theme.copy(color = Color.parseColor("#FF0000"))
        // Giá trị dẫn xuất tính lại sau khi màu chính đổi (không còn bằng giá trị dẫn xuất cũ)
        assertNotEquals(ltBefore, changed.ltColor)
        // Field không đổi giữ nguyên
        assertEquals(theme.color2, changed.color2)
        assertEquals(theme.animationSpeed, changed.animationSpeed)
    }

    @Test
    fun autoTintContrastsWithColor() {
        // Màu chính sáng → chữ đen; màu chính tối → chữ trắng
        val lightTheme = themeData(color = Color.parseColor("#FFFFFF"))
        assertEquals(Color.BLACK, lightTheme.autoTint)
        assertEquals(0x99000000.toInt(), lightTheme.autoHintTint)
        val darkTheme = themeData(color = Color.parseColor("#000000"))
        assertEquals(Color.WHITE, darkTheme.autoTint)
        assertEquals(0x99FFFFFF.toInt(), darkTheme.autoHintTint)
    }

    @Test
    fun getColor2ReturnsRawValueWithoutActivity() {
        // Khi không có Activity foreground (môi trường instrumented test) về giá trị màu sáng, không ném exception
        val theme = themeData(color2 = 0x112233, color2Dark = 0x445566)
        assertEquals(0x112233, theme.getColor2())
        assertEquals(0x112233, theme.color2)
        assertEquals(0x445566, theme.color2Dark)
    }

    @Test
    fun saveAndLoadRoundTrip() {
        val theme = themeData(
            color = 0x123456,
            color2 = 0x654321,
            color2Dark = 0xAABBCC,
        ).copy(fullscreen = true, closeSkinModel = true, animationSpeed = 5)
        ThemeData.saveTheme(context, theme)
        // saveTheme ghi bất đồng bộ, đợi DataStore ghi xong xuống đĩa
        runBlocking { context.themeDataStore.data.first { it.color == 0x123456 } }
        val loaded = ThemeData.getTheme(context)
        assertEquals(0x123456, loaded.color)
        assertEquals(0x654321, loaded.color2)
        assertEquals(0xAABBCC, loaded.color2Dark)
        assertTrue(loaded.fullscreen)
        assertTrue(loaded.closeSkinModel)
        assertEquals(5, loaded.animationSpeed)
        // Nền không rỗng (không có nền tùy chỉnh thì về ảnh mặc định tích hợp sẵn)
        assertTrue(loaded.backgroundLt.intrinsicWidth > 0)
        assertTrue(loaded.backgroundDk.intrinsicWidth > 0)
    }

    @Test
    fun saveKeepsRawColor2NotDynamic() {
        // saveTheme lưu bền color2 gốc (không phải giá trị tính động theo sáng/tối), sau khi tải giá trị gốc không đổi
        val theme = themeData(color2 = 0x111111, color2Dark = 0x222222)
        ThemeData.saveTheme(context, theme)
        runBlocking { context.themeDataStore.data.first { it.color2 == 0x111111 } }
        val loaded = ThemeData.getTheme(context)
        assertEquals(0x111111, loaded.color2)
        assertEquals(0x222222, loaded.color2Dark)
    }

    private fun luminance(color: Int): Float {
        val r = Color.red(color) / 255f
        val g = Color.green(color) / 255f
        val b = Color.blue(color) / 255f
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }
}
