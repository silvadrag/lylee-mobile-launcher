package com.tungsten.fcllibrary.browser

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.tungsten.fcllibrary.browser.options.LibMode
import com.tungsten.fcllibrary.browser.options.SelectionMode

/**
 * Trình khởi chạy file browser kiểu hiện đại, dùng Activity Result API
 * Thay thế cho onActivityResult và ResultListener truyền thống
 */
class FileBrowserLauncher(val activity: AppCompatActivity) {
    private val launcher: ActivityResultLauncher<Intent?>
    private var currentFileBrowser: FileBrowser? = null

    init {
        this.launcher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult? ->
            if (currentFileBrowser != null && currentFileBrowser!!.callback != null) {
                if (result!!.resultCode == Activity.RESULT_OK && result.data != null) {
                    val files = FileBrowser.getSelectedFiles(result.data)
                    currentFileBrowser!!.callback.onResult(files)
                } else {
                    currentFileBrowser!!.callback.onResult(null)
                }
            }
        }
    }

    /**
     * Khởi chạy trình duyệt file
     * @param fileBrowser Instance FileBrowser đã cấu hình sẵn
     */
    fun launch(fileBrowser: FileBrowser) {
        this.currentFileBrowser = fileBrowser
        val intent = Intent(activity, FileBrowserActivity::class.java)
        val bundle = Bundle()
        bundle.putSerializable("config", fileBrowser)
        intent.putExtras(bundle)

        launcher.launch(intent)
    }

    /**
     * Phương thức tiện lợi: tạo và khởi chạy bộ chọn file trực tiếp
     */
    fun launch(
        libMode: LibMode, selectionMode: SelectionMode,
        suffix: List<String>?, initDir: String?,
        callback: FileBrowser.Callback?
    ) {
        val fileBrowser = FileBrowser.Builder(activity)
            .setLibMode(libMode)
            .setSelectionMode(selectionMode)
            .setSuffix(suffix?.toList())
            .setInitDir(initDir)
            .setCallback(callback)
            .create()
        launch(fileBrowser)
    }

    @JvmOverloads
    fun launchSingleSelection(
        initDir: String? = null,
        suffix: List<String>? = null,
        isDirectory: Boolean = false,
        callback: FileBrowser.Callback
    ) {
        launch(
            if (isDirectory) LibMode.FOLDER_CHOOSER else LibMode.FILE_CHOOSER,
            SelectionMode.SINGLE_SELECTION,
            if (isDirectory) null else suffix,
            initDir,
            callback
        )
    }

    fun launchMultiSelection(
        initDir: String?,
        suffix: List<String>? = null,
        callback: FileBrowser.Callback
    ) {
        launch(
            LibMode.FILE_CHOOSER,
            SelectionMode.MULTIPLE_SELECTION,
            suffix,
            initDir,
            callback
        )
    }
}
