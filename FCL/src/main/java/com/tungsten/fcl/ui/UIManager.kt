package com.tungsten.fcl.ui

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.tungsten.fcl.R
import com.tungsten.fcl.lylee.LyleeCobblemonUI
import com.tungsten.fcl.ui.account.AccountUI
import com.tungsten.fcl.ui.controller.ControllerUI
import com.tungsten.fcl.ui.download.DownloadUI
import com.tungsten.fcl.ui.main.MainUI
import com.tungsten.fcl.ui.manage.ManageUI
import com.tungsten.fcl.ui.multiplayer.MultiplayerUI
import com.tungsten.fcl.ui.setting.SettingUI
import com.tungsten.fcl.ui.version.VersionUI
import com.tungsten.fcllibrary.component.ui.FCLBaseUI
import com.tungsten.fcllibrary.component.ui.FCLCommonUI

/**
 * Bộ quản lý UI màn hình chính: dùng ViewPager2 chứa 9 trang UI chính.
 *
 * Instance UI tạo/hủy theo vòng đời trang ViewPager (không giữ trạng thái):
 * khi trang bị ViewPager thu hồi (vượt offscreenPageLimit) thì hủy instance UI
 * tương ứng, lần vào sau tạo lại hoàn toàn mới.
 */
class UIManager(val context: Context, val pager: ViewPager2) {
    companion object {
        @JvmStatic
        lateinit var instance: UIManager
    }

    /** Bảng đăng ký vị trí trang → instance UI, hủy và xóa ô tương ứng khi trang bị thu hồi */
    private val uiRegistry = arrayOfNulls<FCLCommonUI>(9)

    /** Factory UI theo vị trí trang. LyleeCobblemonUI thêm vào cuối (vị trí 8),
     *  không đổi số thứ tự account(6)/version(7) cũ, tránh phải sửa dây chuyền
     *  các vị trí viết chết khác trong MainActivity. */
    private val factories: List<() -> FCLCommonUI> = listOf(
        { MainUI(context, R.layout.ui_main) },
        { ManageUI(context, R.layout.ui_manage) },
        { DownloadUI(context, R.layout.ui_download) },
        { ControllerUI(context, R.layout.ui_controller) },
        { MultiplayerUI(context, R.layout.ui_multiplayer) },
        { SettingUI(context, R.layout.ui_setting) },
        { AccountUI(context, R.layout.ui_account) },
        { VersionUI(context, R.layout.ui_version) },
        { LyleeCobblemonUI(context, R.layout.ui_lylee_cobblemon) }
    )

    var currentUI: FCLBaseUI? = null

    /** Callback khi chuyển trang, MainActivity dùng để đồng bộ menu nổi bật và tiêu đề */
    var pageSelectedListener: ((Int) -> Unit)? = null

    /** Vị trí trang lần onPageSelected trước đó, dùng để lọc bỏ việc ViewPager2
     *  dispatch lại trang hiện tại (VD bàn phím ảo bật lên làm đổi layout) */
    private var lastSelectedPosition = -1

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            currentUI = getUI(position)
            pageSelectedListener?.invoke(position)
            // Hoạt ảnh chuyển cảnh thống nhất: mỗi lần qua trang khác đều làm mờ
            // dần + trượt lên cho trang đích.
            // Chạy đồng bộ (không post): lúc onPageSelected trang đã gắn nhưng
            // chưa vẽ, đặt trong suốt lúc này sẽ không bị chớp hiện-rồi-mất.
            // Chỉ phát khi vị trí trang thực sự đổi: ViewPager2 dispatch lại
            // trang hiện tại khi layout đổi (VD bàn phím ảo bật, nội dung trang
            // làm mới), lúc đó không phát hoạt ảnh để tránh chớp hình
            if (position != lastSelectedPosition) {
                currentUI?.contentView?.apply {
                    animate().cancel()
                    alpha = 0f
                    translationY = resources.displayMetrics.density * 30f
                    animate().alpha(1f).translationY(0f).setDuration(250).start()
                }
            }
            lastSelectedPosition = position
        }
    }

    val mainUI: MainUI get() = getUI(0) as MainUI
    val manageUI: ManageUI get() = getUI(1) as ManageUI
    val downloadUI: DownloadUI get() = getUI(2) as DownloadUI
    val controllerUI: ControllerUI get() = getUI(3) as ControllerUI
    val multiplayerUI: MultiplayerUI get() = getUI(4) as MultiplayerUI
    val settingUI: SettingUI get() = getUI(5) as SettingUI
    val accountUI: AccountUI get() = getUI(6) as AccountUI
    val versionUI: VersionUI get() = getUI(7) as VersionUI
    val lyleeCobblemonUI: LyleeCobblemonUI get() = getUI(8) as LyleeCobblemonUI

    fun init() {
        instance = this
        pager.adapter = UIAdapter()
        // Không tải trước trang lân cận: vào trang nào chỉ tạo trang đó, tránh
        // chi phí inflate/khởi tạo của trang lân cận tạo trước (trang chỉ tạo khi chuyển tới)
        pager.offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
        // Hoạt ảnh chuyển màn hình chính là chuyển cảnh trên-dưới (chiều dọc)
        pager.orientation = ViewPager2.ORIENTATION_VERTICAL
        // Tắt cử chỉ vuốt: nội dung cuộn dọc trong trang xung đột với vuốt chuyển trang, chỉ chuyển qua menu
        pager.isUserInputEnabled = false
        // Không giữ trạng thái vị trí trang: Activity tạo lại luôn bắt đầu từ trang chủ,
        // tránh ViewPager2 khôi phục vị trí cũ đi qua trang chưa khởi tạo (VD ManageUI chưa setVersion)
        pager.isSaveEnabled = false
        pager.registerOnPageChangeCallback(pageChangeCallback)
    }

    fun switchUI(ui: FCLCommonUI) {
        val position = uiRegistry.indexOf(ui)
        if (position < 0) return
        if (ui === currentUI) return
        if (pager.currentItem == position) {
            // Trùng vị trí trang hiện tại (VD trang khởi động ban đầu): chỉ cập nhật UI hiện tại
            currentUI = ui
        } else {
            // Chuyển trang thống nhất nhảy tức thì, hoạt ảnh chuyển cảnh do onPageSelected xử lý.
            // Không dùng vuốt mượt: nhảy xa mà cuộn mượt sẽ đi qua trang giữa khiến UI nặng bị tạo/hủy lần lượt
            pager.setCurrentItem(position, false)
        }
    }

    /** Lấy UI tại vị trí chỉ định, chưa có thì tạo mới rồi gọi onCreate */
    fun getUI(position: Int): FCLCommonUI {
        return uiRegistry[position] ?: factories[position]().also {
            uiRegistry[position] = it
            it.onCreate()
        }
    }

    /** Xóa khỏi bảng đăng ký khi trang bị ViewPager thu hồi (không giữ trạng thái), tài nguyên UI tự giải phóng theo view tree */
    fun destroyUI(position: Int) {
        uiRegistry[position] = null
    }

    fun registerDefaultBackEvent(runnable: Runnable?) {
        FCLBaseUI.setDefaultBackEvent(runnable)
    }

    fun onBackPressed() {
        currentUI?.onBackPressed()
    }

    fun onPause() {
        for (baseUI in uiRegistry) {
            baseUI?.onPause()
        }
    }

    fun onResume() {
        for (baseUI in uiRegistry) {
            baseUI?.onResume()
        }
    }

    inner class UIAdapter : RecyclerView.Adapter<UIAdapter.Holder>() {

        inner class Holder(val container: FrameLayout) : RecyclerView.ViewHolder(container) {
            var boundPosition: Int = 0
        }

        override fun getItemCount(): Int = 9

        override fun getItemViewType(position: Int): Int = position

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            // ViewPager2 yêu cầu View con trực tiếp của trang phải MATCH_PARENT
            val container = FrameLayout(parent.context)
            container.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            return Holder(container)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.boundPosition = position
            holder.container.removeAllViews()
            val contentView = getUI(position).contentView
            // Phòng ngừa: GapWorker prefetch có thể gắn cùng 1 view UI vào container
            // khác (bind prefetch và bind chính thức tranh chấp nhau) — gỡ parent cũ
            // trước để tránh addView ném lỗi "child already has a parent"
            (contentView.parent as? ViewGroup)?.removeView(contentView)
            holder.container.addView(
                contentView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        override fun onViewRecycled(holder: Holder) {
            destroyUI(holder.boundPosition)
        }
    }
}
