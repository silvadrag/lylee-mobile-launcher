package com.tungsten.fcllibrary.component.theme

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.ColorUtils
import com.mio.util.ImageUtil
import com.tungsten.fcl.R
import com.tungsten.fcllibrary.util.ConvertUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Dữ liệu theme (data class bất biến, thay cho phiên bản Theme dùng thuộc tính fakefx cũ).
 *
 * Chuyển sáng/tối không đổi dữ liệu gốc, lấy màu động xem [getColor2] và [getBackground];
 * ltColor/dkColor/autoTint... đều suy ra từ màu chính, tự tính lại khi [color] đổi.
 */
data class ThemeData(
    val color: Int,
    /** Màu phụ ở chế độ sáng (giá trị gốc, phía Java lấy màu động qua [getColor2] hoặc lấy giá trị gốc qua [_getColor2]) */
    @get:JvmName("_getColor2")
    val color2: Int,
    val color2Dark: Int,
    /** Java getter đặt tên isFullscreen (thuộc tính Boolean Kotlin mặc định là getFullscreen) */
    @get:JvmName("isFullscreen")
    val fullscreen: Boolean,
    /** Java getter đặt tên isCloseSkinModel */
    @get:JvmName("isCloseSkinModel")
    val closeSkinModel: Boolean,
    val animationSpeed: Int,
    val backgroundLt: BitmapDrawable,
    val backgroundDk: BitmapDrawable
) {
    /** Biến thể sáng của màu chính (tăng sáng HSV, dùng cho nền sáng/trạng thái nhấn) */
    val ltColor: Int
        get() = deriveColor(color, brighten = true)

    /** Biến thể tối của màu chính (giảm sáng HSV, dùng cho trạng thái chọn/progress bar...) */
    val dkColor: Int
        get() = deriveColor(color, brighten = false)

    /** Màu chữ tự động tương phản với màu chính (đen/trắng) */
    val autoTint: Int
        get() = if (ColorUtils.calculateLuminance(color) >= 0.5) Color.BLACK else Color.WHITE

    /** Màu chữ tự động bán trong suốt (dùng cho text gợi ý) */
    val autoHintTint: Int
        get() = if (ColorUtils.calculateLuminance(color) >= 0.5) 0x99000000.toInt() else 0x99FFFFFF.toInt()

    /** Lấy màu phụ theo chế độ sáng/tối hiện tại (sáng dùng [color2], tối dùng [color2Dark]) */
    fun getColor2(): Int {
        val activity = com.tungsten.fcl.FCLApplication.getCurrentActivity()
        return if (activity != null && ThemeEngine.isNightMode(activity)) color2Dark else color2
    }

    /** Lấy nền theo chế độ sáng/tối hiện tại */
    fun getBackground(context: Context): BitmapDrawable {
        val dark = ThemeEngine.isNightMode(context)
        return if (dark) backgroundDk else backgroundLt
    }

    private fun deriveColor(base: Int, brighten: Boolean): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(base, hsv)
        if (brighten) {
            hsv[1] -= (1 - hsv[1]) * 0.3f
            hsv[2] += (1 - hsv[2]) * 0.3f
        } else {
            hsv[1] += (1 - hsv[1]) * 0.3f
            hsv[2] -= (1 - hsv[2]) * 0.3f
        }
        return Color.HSVToColor(hsv)
    }

    companion object {
        /** Tải theme từ DataStore (thiếu nền thì lùi về ảnh mặc định có sẵn; lần đầu di chuyển cấu hình SharedPreferences cũ) */
        @JvmStatic
        fun getTheme(context: Context): ThemeData {
            val pref = runBlocking { context.themeDataStore.data.first() }
            // Di chuyển 1 lần: DataStore chưa từng ghi mà SharedPreferences cũ có cấu hình theme thì di chuyển rồi ghi lại
            val migrated = pref == ThemePreference() &&
                    context.getSharedPreferences("theme", Context.MODE_PRIVATE)
                        .contains("theme_color")
            val effective = if (migrated) migrateFromSharedPreferences(context) else pref
            val lt = ImageUtil.load(context.filesDir.absolutePath + "/background/lt.png")
                .orElse(ConvertUtils.getBitmapFromRes(context, R.drawable.background_light))
            val dk = ImageUtil.load(context.filesDir.absolutePath + "/background/dk.png")
                .orElse(ConvertUtils.getBitmapFromRes(context, R.drawable.background_dark))
            return ThemeData(
                effective.color, effective.color2, effective.color2Dark,
                effective.fullscreen, effective.closeSkinModel, effective.animationSpeed,
                BitmapDrawable(context.resources, lt),
                BitmapDrawable(context.resources, dk)
            )
        }

        /** Đọc cấu hình SharedPreferences cũ rồi ghi vào DataStore (di chuyển 1 lần) */
        private fun migrateFromSharedPreferences(context: Context): ThemePreference {
            val old = context.getSharedPreferences("theme", Context.MODE_PRIVATE)
            val migrated = ThemePreference(
                color = old.getInt("theme_color", ThemePreference().color),
                color2 = old.getInt("theme_color2", ThemePreference().color2),
                color2Dark = old.getInt("theme_color2_dark", ThemePreference().color2Dark),
                fullscreen = old.getBoolean("fullscreen", ThemePreference().fullscreen),
                closeSkinModel = old.getBoolean(
                    "close_skin_model",
                    ThemePreference().closeSkinModel
                ),
                animationSpeed = old.getInt("animation_speed", ThemePreference().animationSpeed)
            )
            runBlocking {
                context.themeDataStore.updateData { migrated }
            }
            return migrated
        }

        /** Lưu bền vững theme (chỉ lưu field có thể cấu hình, không lưu màu suy ra và nền).
         *  Ghi bất đồng bộ không chặn luồng gọi (callback trang cài đặt ở luồng UI, chờ đồng bộ sẽ giật lag);
         *  ghi lỗi thì bỏ qua âm thầm (giống ngữ nghĩa SharedPreferences.apply cũ) */
        @JvmStatic
        fun saveTheme(context: Context, theme: ThemeData) {
            CoroutineScope(Dispatchers.IO).launch {
                runCatching {
                    context.themeDataStore.updateData {
                        ThemePreference(
                            color = theme.color,
                            color2 = theme.color2,
                            color2Dark = theme.color2Dark,
                            fullscreen = theme.fullscreen,
                            closeSkinModel = theme.closeSkinModel,
                            animationSpeed = theme.animationSpeed
                        )
                    }
                }
            }
        }
    }
}
