package com.tungsten.fcllibrary.component.theme

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.mio.util.ImageUtil
import com.tungsten.fcl.R
import com.tungsten.fclauncher.utils.FCLPath
import com.tungsten.fcllibrary.component.theme.ThemeEngine.registerEvent
import com.tungsten.fcllibrary.util.ConvertUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.WeakHashMap

/**
 * Singleton theme (Repository): giữ StateFlow của [ThemeData],
 * control/trang đăng ký callback làm mới qua [registerEvent] (làm mới toàn bộ, thay cho binding thuộc tính fakefx cũ).
 */
object ThemeEngine {

    private val _theme = MutableStateFlow<ThemeData?>(null)

    /** Theme hiện tại (StateFlow, null trước khi khởi tạo, code mới có thể collect để nhận biết theme đổi).
     *  getter đặt tên getThemeFlow để tránh xung đột chữ ký với getTheme() (trả về ThemeData) phía Java */
    @get:JvmName("getThemeFlow")
    val theme: StateFlow<ThemeData?> = _theme.asStateFlow()

    val handler = Handler(Looper.getMainLooper())

    // Khóa yếu: sau khi trang bị ViewPager2 thu hồi/hủy (không có vòng đời onDestroy), view không còn bị giữ tham chiếu mạnh,
    // các mục registerEvent chưa hủy đăng ký sẽ tự bị GC dọn, tránh rò rỉ tích lũy theo view tree của trang
    private val runnables = WeakHashMap<View, Runnable>()
    private val refreshListeners = ArrayList<Runnable>()

    /** Tương thích Java: trả về chính singleton (thay cho getInstance() cũ) */
    @JvmStatic
    fun getInstance(): ThemeEngine = this

    /** Khởi tạo theme (idempotent, gọi ở dòng đầu FCLActivity.onCreate) */
    fun setupThemeEngine(context: Context) {
        if (_theme.value != null) return
        _theme.value = ThemeData.getTheme(context)
    }

    /** Theme hiện tại (bên gọi phải tuân thủ quy ước setup trước; chưa khởi tạo thì ném exception, tương đương việc getTheme() cũ trả null rồi dùng sẽ crash).
     *  không @JvmStatic: phía Java gọi qua instance getInstance().getTheme() (@JvmStatic sẽ sinh thêm hàm static gây mơ hồ khi gọi chuỗi) */
    fun getTheme(): ThemeData = _theme.value!!

    /** Đăng ký callback làm mới theme cho control/trang (chạy ngay 1 lần sau khi đăng ký) */
    fun registerEvent(view: View, runnable: Runnable) {
        runnables[view] = runnable
        // Chạy đồng bộ: control mới áp màu theme ngay. Post bất đồng bộ sẽ trễ tới khung hình sau,
        // khi vuốt/chuyển trang nhanh thì control mới inflate (VD bg_container_white + tint) sẽ lộ nền trắng ở khung hình đầu.
        // Theme chưa khởi tạo thì lùi về bất đồng bộ (control có thể được tạo trước setupThemeEngine, VD layout Splash)
        if (_theme.value != null) {
            runnable.run()
        } else {
            handler.post(runnable)
        }
    }

    fun unregisterEvent(view: View) {
        runnables.remove(view)
    }

    fun addRefreshListener(runnable: Runnable) {
        refreshListeners.add(runnable)
    }

    fun removeRefreshListener(runnable: Runnable) {
        refreshListeners.remove(runnable)
    }

    /** Làm mới toàn bộ: callback control + listener làm mới toàn cục (theme chưa khởi tạo thì bỏ qua, giống hiện thực cũ) */
    fun refreshTheme() {
        if (_theme.value == null) return
        notifyThemeChanged()
    }

    private fun notifyThemeChanged() {
        for ((_, runnable) in runnables) {
            handler.post(runnable)
        }
        for (runnable in refreshListeners) {
            handler.post(runnable)
        }
    }

    /** Cập nhật dữ liệu theme rồi làm mới toàn bộ */
    private fun updateTheme(transform: (ThemeData) -> ThemeData) {
        val current = _theme.value ?: return
        _theme.value = transform(current)
        notifyThemeChanged()
    }

    /** Xác định sáng/tối: chế độ theme riêng của FCL (0 theo hệ thống / 1 ép sáng / 2 ép tối) ưu tiên hơn uiMode */
    @JvmStatic
    fun isNightMode(context: Context): Boolean {
        val themeMode =
            context.getSharedPreferences("launcher", Context.MODE_PRIVATE).getInt("themeMode", 0)
        if (themeMode == 1) return false
        if (themeMode == 2) return true
        return (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    @JvmStatic
    fun getSystemAutoTint(context: Context): Int =
        if (isNightMode(context)) Color.WHITE else Color.BLACK

    fun applyColor(color: Int) {
        updateTheme { it.copy(color = color) }
    }

    fun applyColor2(color: Int) {
        updateTheme { it.copy(color2 = color) }
    }

    fun applyColor2Dark(color: Int) {
        updateTheme { it.copy(color2Dark = color) }
    }

    fun applyFullscreen(window: Window?, fullscreen: Boolean) {
        updateTheme { it.copy(fullscreen = fullscreen) }
        if (window != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val params = window.attributes
                params.layoutInDisplayCutoutMode = if (fullscreen)
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                else
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
                window.attributes = params
            }
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            )
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    private fun applyBackground(context: Context, view: View?, ltPath: String?, dkPath: String?) {
        runCatching {
            if (ltPath != null && File(ltPath).exists()) {
                File(ltPath).copyTo(File(FCLPath.LT_BACKGROUND_PATH), overwrite = true)
            }
            if (dkPath != null && File(dkPath).exists()) {
                File(dkPath).copyTo(File(FCLPath.DK_BACKGROUND_PATH), overwrite = true)
            }
        }
        val ltBitmap = ImageUtil.load(FCLPath.LT_BACKGROUND_PATH)
            .orElse(ConvertUtils.getBitmapFromRes(context, R.drawable.background_light))
        val dkBitmap = ImageUtil.load(FCLPath.DK_BACKGROUND_PATH)
            .orElse(ConvertUtils.getBitmapFromRes(context, R.drawable.background_dark))
        val lt = BitmapDrawable(context.resources, ltBitmap)
        val dk = BitmapDrawable(context.resources, dkBitmap)
        updateTheme { it.copy(backgroundLt = lt, backgroundDk = dk) }
        if (view != null) {
            val isNight =
                (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            ImageUtil.loadInto(view, if (isNight) dk else lt)
        }
    }

    fun applyAndSave(context: Context, color: Int) {
        applyColor(color)
        ThemeData.saveTheme(context, getTheme())
    }

    fun applyAndSave2(context: Context, color: Int) {
        applyColor2(color)
        ThemeData.saveTheme(context, getTheme())
    }

    fun applyAndSave2Dark(context: Context, color: Int) {
        applyColor2Dark(color)
        ThemeData.saveTheme(context, getTheme())
    }

    fun applyAndSave(context: Context, window: Window, fullscreen: Boolean) {
        applyFullscreen(window, fullscreen)
        ThemeData.saveTheme(context, getTheme())
    }

    fun applyAndSave(context: Context, view: View, lt: String?, dk: String?) {
        applyBackground(context, view, lt, dk)
        ThemeData.saveTheme(context, getTheme())
    }

    /** Công tắc tắt mô hình skin (thay cho việc sửa trực tiếp field Theme.setiIgnoreSkinContainer cũ) */
    fun setCloseSkinModel(closeSkinModel: Boolean) {
        updateTheme { it.copy(closeSkinModel = closeSkinModel) }
    }

    /** Tốc độ hoạt ảnh (thay cho việc sửa trực tiếp field animationSpeedProperty().set cũ) */
    fun setAnimationSpeed(animationSpeed: Int) {
        updateTheme { it.copy(animationSpeed = animationSpeed) }
    }

}
