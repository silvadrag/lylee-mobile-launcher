package com.tungsten.fcl.ui.manage

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.view.View
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mio.ui.adapter.SpacingItemDecoration
import com.mio.ui.dialog.DriverSelectDialog
import com.mio.ui.dialog.JavaManageDialog
import com.mio.ui.dialog.RendererSelectDialog
import com.mio.util.showErrorDialog
import com.mio.util.showItemSelectionDialog
import com.tungsten.fcl.R
import com.tungsten.fcl.activity.MainActivity
import com.tungsten.fcl.control.SelectControllerDialog
import com.tungsten.fcl.databinding.PageVersionSettingBinding
import com.tungsten.fcl.setting.Controllers
import com.tungsten.fcl.setting.Profile
import com.tungsten.fcl.setting.Profiles
import com.tungsten.fcl.setting.Profiles.getSelectedProfile
import com.tungsten.fcl.setting.VersionSetting
import com.tungsten.fcl.ui.UIManager
import com.tungsten.fcl.ui.manage.ManageUI.VersionLoadable
import com.tungsten.fcl.util.AndroidUtils
import com.tungsten.fcl.util.WeakListenerHolder
import com.tungsten.fclauncher.plugins.DriverPlugin.driverList
import com.tungsten.fclauncher.plugins.DriverPlugin.selected
import com.tungsten.fclauncher.utils.FCLPath
import com.tungsten.fclcore.event.Event
import com.tungsten.fclcore.fakefx.beans.property.BooleanProperty
import com.tungsten.fclcore.fakefx.beans.property.IntegerProperty
import com.tungsten.fclcore.fakefx.beans.property.SimpleBooleanProperty
import com.tungsten.fclcore.fakefx.beans.property.SimpleIntegerProperty
import com.tungsten.fclcore.fakefx.beans.property.SimpleStringProperty
import com.tungsten.fclcore.fakefx.beans.property.StringProperty
import com.tungsten.fclcore.task.Schedulers
import com.tungsten.fclcore.task.Task
import com.tungsten.fclcore.util.Logging
import com.tungsten.fclcore.util.io.FileUtils
import com.tungsten.fclcore.util.platform.MemoryUtils
import com.tungsten.fcllibrary.component.dialog.EditDialog
import com.tungsten.fcllibrary.component.dialog.FCLAlertDialog
import com.tungsten.fcllibrary.component.dialog.FullEditDialog
import com.tungsten.fcllibrary.component.ui.FCLPage
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.logging.Level

/**
 * Trang cài đặt version. Mục cài đặt do [VersionSettingAdapter] render kiểu RecyclerView tái dùng dòng,
 * trang chỉ lo thao tác model và dialog.
 */
class VersionSettingPage(
    context: Context?,
    id: Int,
    resId: Int,
    private val globalSetting: Boolean
) : FCLPage(context, id, resId), VersionLoadable, VersionSettingAdapter.Listener {
    private lateinit var lastVersionSetting: VersionSetting
    private lateinit var profile: Profile
    private lateinit var listenerHolder: WeakListenerHolder
    private var versionId: String? = null

    private lateinit var binding: PageVersionSettingBinding
    private lateinit var adapter: VersionSettingAdapter

    /** Listener thay đổi của cài đặt version hiện tại (chuyển đối tượng đăng ký khi loadVersion) */
    private var settingsChangeListener: Runnable? = null
    private var lastIsolateGameDir = true
    private var profileCollectJob: Job? = null
    private var profileVersionListener: Runnable? = null
    private val selectedVersion: StringProperty = SimpleStringProperty()
    private val enableSpecificSettings: BooleanProperty = SimpleBooleanProperty(false)
    private val usedMemory: IntegerProperty = SimpleIntegerProperty(0)
    private val modpack: BooleanProperty = SimpleBooleanProperty()

    init {
        create()
    }

    private fun create() {
        binding = PageVersionSettingBinding.bind(contentView)
        adapter = VersionSettingAdapter(context, globalSetting, this)
        binding.settingList.layoutManager = LinearLayoutManager(context)
        // Dòng cách nhau bằng khoảng trống (ItemDecoration), dòng cuối không thêm
        binding.settingList.addItemDecoration(
            SpacingItemDecoration((8 * context.resources.displayMetrics.density).toInt())
        )
        binding.settingList.adapter = adapter

        // Làm mới cài đặt khi đổi Profile (Repository StateFlow đơn lẻ, thu thập lúc attach, hủy lúc detach để tránh rò rỉ)
        contentView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                profileCollectJob = activity.lifecycleScope.launch {
                    Profiles.selectedProfile.collect { profile ->
                        if (profile != null) loadVersion(profile, versionId)
                    }
                }
            }

            override fun onViewDetachedFromWindow(v: View) {
                profileCollectJob?.cancel()
                // Gỡ listener gắn trên singleton, tránh bị gọi lại sau khi trang đã hủy
                // (lúc attach collect sẽ phát ngay giá trị hiện tại, sẽ đăng ký lại loadVersion)
                profileVersionListener?.let {
                    if (::profile.isInitialized) profile.removeSelectedVersionListener(it)
                }
                profileVersionListener = null
                settingsChangeListener?.let {
                    if (::lastVersionSetting.isInitialized) lastVersionSetting.removeOnChangeListener(
                        it
                    )
                }
                settingsChangeListener = null
            }
        })
    }

    private fun editForceResolution() {
        if (lastVersionSetting.isForceResolution) {
            val preferences = context.getSharedPreferences("launcher", MODE_PRIVATE)
            val dialog = EditDialog(context) { str ->
                try {
                    val split =
                        str.lowercase(Locale.getDefault()).split("x".toRegex())
                            .dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (split.size == 2) {
                        val w = split[0].toInt()
                        val h = split[1].toInt()
                        preferences.edit {
                            putString("force_resolution", w.toString() + "x" + h)
                        }
                    }
                } catch (e: Exception) {
                    showErrorDialog(context, e.toString())
                }
            }
            dialog.getEditText().setText(preferences.getString("force_resolution", "1920x1080"))
            dialog.onCancelListener = {
                lastVersionSetting.isForceResolution = false
                adapter.refreshRow(VersionSettingTag.FORCE_RESOLUTION)
            }
            dialog.show()
        }
    }

    /** Chọn nguồn tải để cài loader (Github / ổ mạng) */
    private fun installDialog(githubUrl: String, netdiskUrl: String) {
        showItemSelectionDialog(
            context,
            context.getString(R.string.message_install_plugin),
            listOf("Github", context.getString(R.string.settings_download_netdisk))
        ) { pos, _ ->
            AndroidUtils.openLink(
                context, when (pos) {
                    0 -> githubUrl
                    1 -> netdiskUrl
                    else -> return@showItemSelectionDialog
                }
            )
        }
    }

    override fun refresh(vararg param: Any?): Task<*>? {
        return null
    }

    override fun loadVersion(profile: Profile, versionId: String?) {
        this.profile = profile
        this.versionId = versionId
        this.listenerHolder = WeakListenerHolder()

        if (versionId == null) {
            enableSpecificSettings.set(true)
            // Lắng nghe version của profile đổi (thay cho việc lắng nghe fakefx property cũ)
            profileVersionListener?.let { profile.removeSelectedVersionListener(it) }
            val listener = Runnable { this.selectedVersion.value = profile.selectedVersion }
            profileVersionListener = listener
            profile.addSelectedVersionListener(listener)
        }

        val versionSetting = profile.getVersionSetting(versionId)
        versionSetting.checkController()

        modpack.set(versionId != null && profile.repository.isModpack(versionId))
        usedMemory.set(MemoryUtils.getUsedDeviceMemory(context))

        // Đổi đối tượng lắng nghe: gỡ instance cũ, đăng ký instance mới (làm mới khi thuộc tính đổi)
        settingsChangeListener?.let { lastVersionSetting.removeOnChangeListener(it) }
        lastIsolateGameDir = versionSetting.isIsolateGameDir
        val listener = Runnable {
            // Làm mới list mod/world khi đổi thư mục cô lập (chỉ trang cài đặt game)
            if (id == ManageUI.PAGE_ID_MANAGE_SETTING && lastIsolateGameDir != versionSetting.isIsolateGameDir) {
                lastIsolateGameDir = versionSetting.isIsolateGameDir
                UIManager.instance.manageUI.onRunDirectoryChange(profile, versionId)
            }
            // Làm mới mục cài đặt khi usesGlobal đổi
            if (enableSpecificSettings.get() != !versionSetting.isUsesGlobal) {
                enableSpecificSettings.set(!versionSetting.isUsesGlobal)
                Schedulers.androidUIThread().execute {
                    adapter.update(
                        versionSetting,
                        modpack.get(),
                        enableSpecificSettings.get(),
                        usedMemory.get()
                    )
                }
            }
        }
        settingsChangeListener = listener
        versionSetting.addOnChangeListener(listener)

        // Kiểm tra driver: driver không hợp lệ thì lùi về Turnip
        if (versionSetting.driver != "Turnip") {
            var isSelected = false
            for (driver in driverList) {
                if (driver.driver == versionSetting.driver) {
                    selected = driver
                    versionSetting.driver = driver.driver
                    isSelected = true
                }
            }
            if (!isSelected) {
                versionSetting.driver = "Turnip"
            }
        }

        if (versionId != null) enableSpecificSettings.set(!versionSetting.isUsesGlobal)

        lastVersionSetting = versionSetting
        adapter.update(
            versionSetting,
            modpack.get(),
            enableSpecificSettings.get(),
            usedMemory.get()
        )
        Controllers.addCallback {
            adapter.refreshRow(VersionSettingTag.EDIT_CONTROLLER)
        }
        loadIcon()
    }

    private fun onExploreIcon() {
        if (versionId == null) return

        MainActivity.getInstance().fileLauncher.launchSingleSelection(null, listOf(".png")) {
            var path = it?.get(0) ?: return@launchSingleSelection
            val uri = path.toUri()
            if (AndroidUtils.isDocUri(uri)) {
                path =
                    AndroidUtils.copyFileToDir(activity, uri, File(FCLPath.CACHE_DIR))
            }
            val selectedFile = File(path)
            val iconFile = profile.repository.getVersionIconFile(versionId)
            try {
                FileUtils.copyFile(selectedFile, iconFile)

                profile.repository.onVersionIconChanged.fireEvent(Event(this))
                loadIcon()
            } catch (e: IOException) {
                Logging.LOG.log(
                    Level.SEVERE,
                    "Failed to copy icon file from $selectedFile to $iconFile",
                    e
                )
            }
        }
    }

    private fun onDeleteIcon() {
        if (versionId == null) return

        val iconFile = profile.repository.getVersionIconFile(versionId)
        if (iconFile.exists()) iconFile.delete()
        profile.repository.onVersionIconChanged.fireEvent(Event(this))
        loadIcon()
    }

    private fun loadIcon() {
        if (versionId == null) {
            return
        }
        Schedulers.defaultScheduler().execute {
            val icon = profile.repository.getVersionIconImage(versionId)
            Schedulers.androidUIThread().execute { adapter.setIcon(icon) }
        }
    }

    override fun onButtonClick(tag: VersionSettingTag) {
        when (tag) {
            VersionSettingTag.EDIT_ICON -> onExploreIcon()
            VersionSettingTag.DELETE_ICON -> onDeleteIcon()
            VersionSettingTag.EDIT_JAVA -> {
                JavaManageDialog(context) {
                    lastVersionSetting.java = it
                    adapter.refreshRow(VersionSettingTag.EDIT_JAVA)
                }.show()
            }

            VersionSettingTag.INSTALL_JAVA -> installDialog(
                "https://github.com/FCL-Team/FoldCraftLauncher/releases/tag/java",
                "https://pan.quark.cn/s/a5f230c3da03"
            )

            VersionSettingTag.EDIT_CONTROLLER -> {
                if (Controllers.isInitialized()) {
                    val dialog = SelectControllerDialog(
                        context,
                        lastVersionSetting.controller
                    ) {
                        lastVersionSetting.controller = it.id
                        adapter.refreshRow(VersionSettingTag.EDIT_CONTROLLER)
                    }
                    dialog.show()
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.message_data_is_loading),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            VersionSettingTag.INSTALL_CONTROLLER -> {
                MainActivity.getInstance().binding.controller.setSelected(true)
                MainActivity.getInstance().uiManager.controllerUI.showPage(1)
            }

            VersionSettingTag.EDIT_BACKEND -> {
                showItemSelectionDialog(
                    context,
                    context.getString(R.string.settings_fcl_graphics_backend),
                    listOf("default", "opengl", "vulkan"),
                    false
                ) { _, backendName: String ->
                    lastVersionSetting.graphicsBackend = backendName
                    adapter.refreshRow(VersionSettingTag.EDIT_BACKEND)
                }
            }

            VersionSettingTag.EDIT_RENDERER -> {
                RendererSelectDialog(context, globalSetting) {
                    if (globalSetting && !getSelectedProfile().versionSetting.isUsesGlobal) {
                        val builder = FCLAlertDialog.Builder(context)
                        builder.setAlertLevel(FCLAlertDialog.AlertLevel.INFO)
                        builder.setMessage(context.getString(R.string.message_warn_renderer_global_setting))
                        builder.setNegativeButton(
                            context.getString(com.tungsten.fcl.R.string.dialog_positive),
                            null
                        )
                        builder.create().show()
                    }
                    adapter.refreshRow(VersionSettingTag.EDIT_RENDERER)
                }.show()
            }

            VersionSettingTag.INSTALL_RENDERER -> installDialog(
                "https://github.com/ShirosakiMio/FCLRendererPlugin/releases/tag/Renderer",
                "https://pan.quark.cn/s/a9f6e9d860d9"
            )

            VersionSettingTag.EDIT_DRIVER -> {
                DriverSelectDialog(
                    context,
                    globalSetting
                ) { adapter.refreshRow(VersionSettingTag.EDIT_DRIVER) }.show()
            }

            VersionSettingTag.INSTALL_DRIVER -> installDialog(
                "https://github.com/FCL-Team/FCLDriverPlugin/releases/tag/Turnip",
                "https://pan.quark.cn/s/d87c59695250"
            )

            VersionSettingTag.EDIT_ENV -> {
                val preferences = context.getSharedPreferences("launcher", MODE_PRIVATE)
                val dialog = FullEditDialog(context, true) {
                    val env = getEnvironmentFromString(it)
                    preferences.edit {
                        putString("env", env.joinToString("\n"))
                    }
                }
                dialog.binding.editText.setText(preferences.getString("env", ""))
                dialog.show()
            }

            else -> {}
        }
    }

    override fun onSpecialSwitch(tag: VersionSettingTag, checked: Boolean) {
        when (tag) {
            VersionSettingTag.SPECIAL -> {
                if (versionId == null) return
                enableSpecificSettings.set(checked)
                // do not call versionSettings.setUsesGlobal(true/false)
                // because versionSettings can be the global one.
                // global versionSettings.usesGlobal is always true.
                if (checked) profile.repository.specializeVersionSetting(versionId)
                else profile.repository.globalizeVersionSetting(versionId)
                Schedulers.androidUIThread().execute { loadVersion(profile, versionId) }
            }

            VersionSettingTag.VULKAN -> {
                lastVersionSetting.isVKDriverSystem = checked
                if (checked && AndroidUtils.isAdrenoGPU()) {
                    val builder = FCLAlertDialog.Builder(context)
                    builder.setAlertLevel(FCLAlertDialog.AlertLevel.INFO)
                    builder.setMessage(context.getString(R.string.message_vulkan_driver_system))
                    builder.setNegativeButton(
                        context.getString(com.tungsten.fcl.R.string.dialog_positive),
                        null
                    )
                    builder.create().show()
                }
                adapter.update(
                    lastVersionSetting,
                    modpack.get(),
                    enableSpecificSettings.get(),
                    usedMemory.get()
                )
            }

            VersionSettingTag.FORCE_RESOLUTION -> {
                lastVersionSetting.isForceResolution = checked
                if (checked) editForceResolution()
            }

            else -> {}
        }
    }

    override fun onSwitchLongClick(tag: VersionSettingTag) {
        if (tag == VersionSettingTag.FORCE_RESOLUTION && lastVersionSetting.isForceResolution) {
            editForceResolution()
        }
    }

    override fun onLongPressEdit(tag: VersionSettingTag) {
        val dialog = FullEditDialog(context, true) { text ->
            when (tag) {
                VersionSettingTag.JVM_ARGS -> lastVersionSetting.javaArgs = text
                VersionSettingTag.MC_ARGS -> lastVersionSetting.minecraftArgs = text
                else -> {}
            }
            adapter.refreshRow(tag)
        }
        dialog.getEditText().setText(
            when (tag) {
                VersionSettingTag.JVM_ARGS -> lastVersionSetting.javaArgs
                VersionSettingTag.MC_ARGS -> lastVersionSetting.minecraftArgs
                else -> ""
            }
        )
        dialog.show()
    }

    fun getEnvironmentFromString(input: String): List<String> {
        val result = mutableListOf<String>()
        val lines = input.trim().lines()

        lines.forEachIndexed { _, rawLine ->
            val line = rawLine.trim()

            // Bỏ qua dòng trống
            if (line.isEmpty()) {
                return@forEachIndexed
            }

            // Kiểm tra có chứa '=' không
            val firstEq = line.indexOf('=')
            if (firstEq == -1) {
                return@forEachIndexed
            }

            val name = line.substring(0, firstEq).trim()
            val value = line.substring(firstEq + 1).trim() // giá trị có thể để trống

            // Tên biến không được để trống
            if (name.isEmpty()) {
                return@forEachIndexed
            }

            // Quy tắc tên biến: chữ, số, gạch dưới, không bắt đầu bằng số
            val validNameRegex = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")
            if (!validNameRegex.matches(name)) {
                return@forEachIndexed
            }

            // Kiểm tra độ dài (tên biến môi trường thường không quá 255 ký tự)
            if (name.length > 255) {
                return@forEachIndexed
            }
            result.add("$name=$value")
        }

        return result
    }
}
