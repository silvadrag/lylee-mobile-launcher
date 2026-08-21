package com.tungsten.fcl.setting

import com.tungsten.fcl.R
import com.tungsten.fcl.util.WeakListenerHolder
import com.tungsten.fclauncher.utils.FCLPath
import com.tungsten.fclcore.event.EventBus
import com.tungsten.fclcore.event.RefreshedVersionsEvent
import com.tungsten.fclcore.fakefx.collections.FXCollections
import java.io.File
import java.util.TreeMap
import java.util.function.Consumer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object Profiles {
    /**
     * True if [.init] hasn't been called.
     */
    private var initialized = false
    private var isFirstRefresh = true
    /**
     * Called when it's ready to load profiles from [ConfigHolder.config].
     */
    private val holder = WeakListenerHolder()
    /** Danh sách Profile (Repository đơn lẻ, sửa đổi đều qua addProfile/removeProfile để kích hoạt lưu và kiểm tra mục đang chọn) */
    @JvmStatic
    val profiles = mutableListOf<Profile>()
    private val _selectedProfile = MutableStateFlow<Profile?>(null)
    /** Profile đang chọn (trạng thái Repository đơn lẻ, phía Java truy cập qua getSelectedProfileFlow()) */
    @get:JvmName("getSelectedProfileFlow")
    val selectedProfile: StateFlow<Profile?> = _selectedProfile.asStateFlow()
    /** Listener khi Profile đang chọn đổi (setter thông báo đồng bộ, luồng gọi cũng là luồng callback) */
    private val selectedProfileListeners = mutableListOf<Runnable>()
    private val _selectedVersion = MutableStateFlow<String?>(null)
    /** Version đang chọn của Profile đang chọn (trạng thái Repository đơn lẻ, phía Java truy cập qua getSelectedVersionFlow()) */
    @get:JvmName("getSelectedVersionFlow")
    val selectedVersion: StateFlow<String?> = _selectedVersion.asStateFlow()
    private var selectedVersionProfile: Profile? = null
    private var selectedVersionListener: Runnable? = null
    private val versionsListeners: MutableList<Consumer<Profile>> =
        ArrayList(4)

    /** Thêm Profile (kích hoạt lưu cấu hình, bù mặc định và kiểm tra mục đang chọn) */
    @JvmStatic
    fun addProfile(profile: Profile) {
        registerProfileSave(profile)
        profiles.add(profile)
        onProfilesChanged()
    }

    /** Gỡ Profile (kích hoạt lưu cấu hình, bù mặc định và kiểm tra mục đang chọn) */
    @JvmStatic
    fun removeProfile(profile: Profile) {
        profiles.remove(profile)
        onProfilesChanged()
    }

    private fun onProfilesChanged() {
        updateProfileStorages()
        checkProfiles()
        // Kiểm tra mục đang chọn vẫn còn trong list khi list đổi (logic listener list fakefx cũ)
        val current = _selectedProfile.value
        if (current != null && !profiles.contains(current)) {
            setSelectedProfileInternal(profiles[0])
        }
    }

    private fun checkProfiles() {
        if (profiles.isEmpty()) {
            val current = Profile(
                FCLPath.CONTEXT.getString(R.string.profile_shared),
                File(FCLPath.SHARED_COMMON_DIR),
                VersionSetting(),
                null
            )
            val home = Profile(
                FCLPath.CONTEXT.getString(R.string.profile_private),
                File(FCLPath.PRIVATE_COMMON_DIR)
            )
            registerProfileSave(current)
            registerProfileSave(home)
            profiles.addAll(listOf(current, home))
        }
    }

    /** Kích hoạt lưu cấu hình khi field đổi (cài đặt toàn cục/version đang chọn/thư mục/tên) */
    private fun registerProfileSave(profile: Profile) {
        profile.onChanged = { updateProfileStorages() }
    }

    private fun updateProfileStorages() {
        // don't update the underlying storage before data loading is completed
        // otherwise it might cause data loss
        if (!initialized) return
        // update storage
        val newConfigurations = TreeMap<String, Profile>()
        for (profile in profiles) {
            newConfigurations[profile.name] = profile
        }
        ConfigHolder.config().configurations.value =
            FXCollections.observableMap(newConfigurations)
    }

    @JvmStatic
    fun init() {
        if (initialized) return

        val names = HashSet<String>()
        ConfigHolder.config().configurations.forEach { (name, profile) ->
            if (!names.add(name)) return@forEach
            profile.name = name
            registerProfileSave(profile)
            profiles.add(profile)
        }
        checkProfiles()

        initialized = true
        val profile =
            profiles.find { it.name == ConfigHolder.config().selectedProfile } ?: profiles[0]
        profile.repository.refreshVersions()
        setSelectedProfileInternal(profile)
        holder.add(
            EventBus.EVENT_BUS.channel(RefreshedVersionsEvent::class.java)
                .registerWeak { event ->
                    val profile = _selectedProfile.value ?: return@registerWeak
                    if (profile.repository === event!!.getSource()) {
                        bindSelectedVersion(profile)
                        for (listener in versionsListeners) listener.accept(profile)
                    }
                }
        )
        isFirstRefresh = false
    }

    /** Đặt Profile đang chọn (đều qua logic kiểm tra, lưu và gắn version, thông báo listener đồng bộ) */
    private fun setSelectedProfileInternal(profile: Profile) {
        if (_selectedProfile.value === profile) return
        _selectedProfile.value = profile
        if (!initialized) return
        if (!profiles.contains(profile)) {
            _selectedProfile.value = profiles[0]
        } else {
            ConfigHolder.config().selectedProfile = profile.name
            profile.gameDir.resolve(".nomedia").let {
                if (!it.exists()) {
                    runCatching {
                        it.parentFile?.mkdirs()
                        it.createNewFile()
                    }
                }
            }
            if (profile.repository.isLoaded) {
                bindSelectedVersion(profile)
            } else {
                unbindSelectedVersion()
                // bind when repository was reloaded.
//                    profile.repository.refreshVersionsAsync().start()
            }
        }
        // Sao chép rồi duyệt: callback có thể thêm/xóa listener, tránh sửa đổi đồng thời
        selectedProfileListeners.toList().forEach { listener -> listener.run() }
        // Chỉ làm mới khi chuyển Profile chưa tải version (refreshVersions sẽ xóa cache phân tích/jar,
        // Profile đã tải giữ cache để chuyển nhanh hơn; version đổi do sự kiện làm mới và làm mới thủ công điều khiển)
        if (!isFirstRefresh && !profile.repository.isLoaded) {
            profile.repository.refreshVersionsAsync().start()
        }
    }

    @JvmStatic
    fun getSelectedProfile(): Profile {
        checkProfiles()
        return _selectedProfile.value ?: profiles[0]
    }

    @JvmStatic
    fun setSelectedProfile(profile: Profile) {
        setSelectedProfileInternal(profile)
    }

    /** Đăng ký listener khi Profile đang chọn đổi (thân thiện Java, bên trong dựa trên StateFlow collect) */
    @JvmStatic
    fun addSelectedProfileListener(listener: Runnable) {
        selectedProfileListeners.add(listener)
    }

    @JvmStatic
    fun removeSelectedProfileListener(listener: Runnable) {
        selectedProfileListeners.remove(listener)
    }

    /** Theo thuộc tính version của Profile chỉ định (ngữ nghĩa bind fakefx cũ) */
    private fun bindSelectedVersion(profile: Profile) {
        selectedVersionProfile?.let { old ->
            selectedVersionListener?.let { old.removeSelectedVersionListener(it) }
        }
        val listener = Runnable {
            _selectedVersion.value = profile.selectedVersion
        }
        selectedVersionListener = listener
        selectedVersionProfile = profile
        profile.addSelectedVersionListener(listener)
        _selectedVersion.value = profile.selectedVersion
    }

    private fun unbindSelectedVersion() {
        selectedVersionProfile?.let { old ->
            selectedVersionListener?.let { old.removeSelectedVersionListener(it) }
        }
        selectedVersionProfile = null
        selectedVersionListener = null
        _selectedVersion.value = null
    }

    // Guaranteed that the repository is loaded.
    @JvmStatic
    fun getSelectedVersion(): String? {
        return _selectedVersion.value
    }

    @JvmStatic
    fun registerVersionsListener(listener: Consumer<Profile>) {
        val profile = getSelectedProfile()
        if (profile.repository.isLoaded) listener.accept(profile)
        versionsListeners.add(listener)
    }

    @JvmStatic
    fun unregisterVersionsListener(listener: Consumer<Profile>) {
        versionsListeners.remove(listener)
    }

}
