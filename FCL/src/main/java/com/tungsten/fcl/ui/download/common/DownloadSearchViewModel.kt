package com.tungsten.fcl.ui.download.common

import androidx.lifecycle.ViewModel
import com.tungsten.fclcore.mod.RemoteMod
import com.tungsten.fclcore.mod.RemoteModRepository

/**
 * Trạng thái tìm kiếm trang tải: gắn ViewModelStore của Activity, cùng vòng đời với Activity.
 *
 * Khi trang tải dùng chung chuyển giữa 5 chế độ (Mod/Modpack/Resource Pack/World/Shader),
 * điều kiện và kết quả tìm kiếm mỗi chế độ lưu riêng theo id trang, chuyển về là khôi phục ngay,
 * không tìm lại, không tải lại ảnh.
 */
class DownloadSearchViewModel : ViewModel() {

    /** Trạng thái tìm kiếm của 1 chế độ tải (phân biệt theo id trang) */
    class State {
        @JvmField
        var searchFilter: String = ""
        @JvmField
        var userGameVersion: String? = null
        @JvmField
        var category: RemoteModRepository.Category? = null
        @JvmField
        var sortType: RemoteModRepository.SortType = RemoteModRepository.SortType.POPULARITY
        @JvmField
        var pageOffset: Int = 0
        /** Tên nguồn tải (Modrinth/CurseForge, chỉ chế độ Localized) */
        @JvmField
        var source: String? = null
        @JvmField
        var result: ArrayList<RemoteMod>? = null
        @JvmField
        var pageCount: Int = -1
        /** Vị trí bộ lọc loader của chế độ Mod */
        @JvmField
        var modLoaderPosition: Int = 0
        /** Adapter list của chế độ đó (tái dùng khi đổi chế độ, tránh tạo lại list phát lại hoạt ảnh item) */
        @JvmField
        var adapter: RemoteModListAdapter? = null
    }

    private val states = HashMap<Int, State>()

    fun getState(pageId: Int): State = states.getOrPut(pageId) { State() }
}
