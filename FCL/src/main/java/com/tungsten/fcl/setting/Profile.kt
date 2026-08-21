package com.tungsten.fcl.setting

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.JsonAdapter
import com.tungsten.fcl.game.FCLCacheRepository
import com.tungsten.fcl.game.FCLGameRepository
import com.tungsten.fcl.util.WeakListenerHolder
import com.tungsten.fclcore.download.DefaultDependencyManager
import com.tungsten.fclcore.download.DownloadProvider
import com.tungsten.fclcore.event.EventBus
import com.tungsten.fclcore.event.EventPriority
import com.tungsten.fclcore.event.RefreshedVersionsEvent
import com.tungsten.fclcore.game.Version
import com.tungsten.fclcore.util.ToStringBuilder
import java.io.File
import java.lang.reflect.Type

/**
 * Cấu hình thư mục game. Dùng field kiểu thường, không còn phụ thuộc fakefx property;
 * version đang chọn đổi thì thông báo qua [addSelectedVersionListener].
 */
@JsonAdapter(Profile.Serializer::class)
class Profile {
    private val listenerHolder = WeakListenerHolder()

    /** Tên */
    var name: String = ""
        set(value) {
            if (field == value) return
            field = value
            onChanged?.invoke()
        }

    /** Thư mục game (đổi thì chuyển thư mục repository) */
    var gameDir: File = File("")
        set(value) {
            field = value
            repository.changeDirectory(value)
            onChanged?.invoke()
        }

    /** Repository game (tạo theo thư mục ban đầu lúc khởi tạo, đổi thư mục thì chuyển) */
    val repository: FCLGameRepository

    /** Version đang chọn (đổi thì kiểm tra hợp lệ và thông báo listener) */
    var selectedVersion: String? = null
        set(value) {
            if (field == value) return
            field = value
            checkSelectedVersion()
            // Sao chép rồi duyệt: callback có thể thêm/xóa listener (VD DownloadUI đổi đối tượng lắng nghe), tránh sửa đổi đồng thời
            selectedVersionListeners.toList().forEach { it.run() }
            onChanged?.invoke()
        }

    /** Cài đặt toàn cục (đổi thì kích hoạt [onChanged]) */
    val globalVersionSetting: VersionSetting

    /** Callback khi field đổi (do Profiles đặt, dùng để kích hoạt lưu cấu hình) */
    var onChanged: (() -> Unit)? = null

    private val selectedVersionListeners = mutableListOf<Runnable>()

    /** Cài đặt của version đang chọn (phía Java truy cập qua getVersionSetting()) */
    val versionSetting: VersionSetting
        get() = repository.getVersionSetting(selectedVersion)

    constructor(name: String, gameDir: File) : this(
        name,
        gameDir,
        VersionSetting(),
        null
    )

    constructor(
        name: String,
        gameDir: File,
        globalVersionSetting: VersionSetting?,
        selectedVersion: String?
    ) {
        this.name = name
        //Bắt buộc đặt trước gameDir
        this.repository = FCLGameRepository(this, gameDir)
        this.gameDir = gameDir
        this.globalVersionSetting = globalVersionSetting ?: VersionSetting()
        this.globalVersionSetting.addOnChangeListener { onChanged?.invoke() }
        this.selectedVersion = selectedVersion

        listenerHolder.add(
            EventBus.EVENT_BUS.channel(RefreshedVersionsEvent::class.java)
                .registerWeak({ checkSelectedVersion() }, EventPriority.HIGHEST)
        )
    }

    /** Đăng ký listener khi version đang chọn đổi (setter thông báo đồng bộ, luồng gọi cũng là luồng callback) */
    fun addSelectedVersionListener(listener: Runnable) {
        selectedVersionListeners.add(listener)
    }

    fun removeSelectedVersionListener(listener: Runnable) {
        selectedVersionListeners.remove(listener)
    }

    private fun checkSelectedVersion() {
        if (!repository.isLoaded) return
        val newValue = selectedVersion
        if (!repository.hasVersion(newValue)) {
            val version = repository.getVersions().stream().findFirst().map(Version::getId)
            if (version.isPresent) {
                selectedVersion = version.get()
            } else if (newValue != null) {
                selectedVersion = null
            }
        }
    }

    fun getDependency(): DefaultDependencyManager =
        getDependency(DownloadProviders.getDownloadProvider())

    fun getDependency(downloadProvider: DownloadProvider): DefaultDependencyManager =
        DefaultDependencyManager(repository, downloadProvider, FCLCacheRepository.REPOSITORY)

    fun getVersionSetting(id: String?): VersionSetting = repository.getVersionSetting(id)

    override fun toString(): String = ToStringBuilder(this)
        .append("gameDir", gameDir)
        .append("name", name)
        .toString()

    class ProfileVersion(val profile: Profile, val version: String?)

    class Serializer : JsonSerializer<Profile?>, JsonDeserializer<Profile?> {
        override fun serialize(
            src: Profile?,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            if (src == null) return JsonNull.INSTANCE
            return JsonObject().apply {
                add("global", context.serialize(src.globalVersionSetting))
                addProperty("gameDir", src.gameDir.path)
                addProperty("selectedMinecraftVersion", src.selectedVersion)
            }
        }

        @Throws(JsonParseException::class)
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): Profile? {
            if (json === JsonNull.INSTANCE || json !is JsonObject) return null
            return Profile(
                "Default",
                File(json["gameDir"]?.asString ?: ""),
                context.deserialize(json["global"], VersionSetting::class.java),
                json["selectedMinecraftVersion"]?.asString ?: ""
            )
        }
    }
}
