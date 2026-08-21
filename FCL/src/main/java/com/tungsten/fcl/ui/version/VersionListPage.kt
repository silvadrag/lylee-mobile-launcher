package com.tungsten.fcl.ui.version

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonParseException
import com.tungsten.fcl.R
import com.tungsten.fcl.activity.MainActivity
import com.tungsten.fcl.databinding.PageVersionListBinding
import com.tungsten.fcl.setting.Profile
import com.tungsten.fcl.setting.Profiles
import com.tungsten.fcl.setting.Profiles.getSelectedProfile
import com.tungsten.fcl.setting.Profiles.profiles
import com.tungsten.fcl.setting.Profiles.registerVersionsListener
import com.tungsten.fcl.setting.Profiles.unregisterVersionsListener
import com.tungsten.fcl.util.AndroidUtils
import com.tungsten.fclcore.download.LibraryAnalyzer
import com.tungsten.fclcore.game.Version
import com.tungsten.fclcore.mod.ModpackConfiguration
import com.tungsten.fclcore.task.Task
import com.tungsten.fclcore.util.Logging
import com.tungsten.fcllibrary.component.ui.FCLPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.Files
import java.util.Locale
import java.util.function.Consumer
import java.util.logging.Level
import java.util.stream.Collectors
import kotlin.io.path.isRegularFile

class VersionListPage(context: Context?, id: Int, resId: Int) : FCLPage(context, id, resId),
    View.OnClickListener {
    private lateinit var binding: PageVersionListBinding
    private var adapter: VersionListAdapter? = null
    private lateinit var children: MutableList<VersionListItem>
    private var textWatcher: TextWatcher? = null
    private var highlightedProfile: Profile? = null
    private var versionHighlightListener: Runnable? = null
    private var loadJob: Job? = null
    private var versionsListener: Consumer<Profile>? = null
    private var profileCollectJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        binding = PageVersionListBinding.bind(contentView)
        binding.refresh.setOnClickListener(this)
        binding.newProfile.setOnClickListener(this)
        // Listener làm mới version: khôi phục lúc attach, hủy lúc detach, tránh list tĩnh giữ trang đã hủy (nhất quán với DownloadUI)
        val listener = Consumer<Profile> { loadVersions(it) }
        versionsListener = listener
        registerVersionsListener(listener)
        contentView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                versionsListener?.let {
                    unregisterVersionsListener(it)
                    registerVersionsListener(it)
                }
                // Tải lại list version khi đổi Profile (không phụ thuộc sự kiện làm mới)
                profileCollectJob = activity.lifecycleScope.launch {
                    Profiles.selectedProfile.collect { profile ->
                        if (profile != null) loadVersions(profile)
                    }
                }
            }

            override fun onViewDetachedFromWindow(v: View) {
                versionsListener?.let { unregisterVersionsListener(it) }
                profileCollectJob?.cancel()
                // Gỡ listener nổi bật gắn trên profile đơn lẻ, tránh bị gọi lại sau khi trang đã hủy
                // (lúc attach collect sẽ phát ngay giá trị hiện tại, sẽ đăng ký lại loadVersions)
                versionHighlightListener?.let { highlightedProfile?.removeSelectedVersionListener(it) }
                versionHighlightListener = null
                highlightedProfile = null
            }
        })
        refreshProfile()
        textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                val text = s.toString()
                adapter?.updateVersionList(if (text.isEmpty()) children else children.filter {
                    it.version.lowercase(
                        Locale.getDefault()
                    ).contains(text.lowercase(Locale.getDefault()))
                })
            }
        }
        binding.category.setOnCheckedChangeListener { _, i ->
            when (i) {
                R.id.all -> {
                    adapter?.updateVersionList(children)
                }

                R.id.fabric -> {
                    adapter?.updateVersionList(
                        children.filter {
                            it.libraries.split(",").find { lib ->
                                lib.contains(":") && lib.contains("Fabric")
                            } != null
                        }
                    )
                }

                R.id.forge -> {
                    adapter?.updateVersionList(
                        children.filter {
                            it.libraries.split(",").find { lib ->
                                lib.contains(":") && lib.contains("Forge") && !lib.contains("NeoForge")
                            } != null
                        }
                    )
                }

                R.id.neoforge -> {
                    adapter?.updateVersionList(
                        children.filter {
                            it.libraries.split(",").find { lib ->
                                lib.contains(":") && lib.contains("NeoForge")
                            } != null
                        }
                    )
                }

                R.id.other -> {
                    adapter?.updateVersionList(
                        children.filter {
                            it.libraries.split(",").none { lib ->
                                lib.contains("Fabric") || lib.contains("Forge") || lib.contains("NeoForge")
                            }
                        }
                    )
                }

            }
        }
    }

    override fun refresh(vararg param: Any?): Task<*>? {
        return Task.runAsync {}
    }

    fun refreshProfile() {
        val adapter = ProfileListAdapter(context, profiles)
        binding.profileList.adapter = adapter
    }

    private fun loadVersions(profile: Profile) {
        // Dừng lần tải trước (đổi profile thì hủy ngay việc tải version cũ, tránh kết quả quá hạn đè lên)
        loadJob?.cancel()
        var job: Job? = null
        job = MainActivity.getInstance().lifecycleScope.launch {
            binding.category.check(R.id.all)
            binding.search.removeTextChangedListener(textWatcher)
            binding.search.setText("")
            binding.refresh.isEnabled = false
            binding.layout.visibility = View.GONE
            binding.progress.visibility = View.VISIBLE
            if (profile == getSelectedProfile()) {
                val repository = profile.repository
                val result = withContext(Dispatchers.IO) {
                    repository.displayVersions
                        .parallel()
                        .map { version: Version ->
                            ensureActive()
                            val game = profile.repository.getGameVersion(version.id)
                            // Phân tích 1 lần, analyzer và xác định icon dùng chung (getVersionIconImage không resolve lặp lại nữa)
                            val resolved =
                                profile.repository.getResolvedPreservingPatchesVersion(version.id)
                            val libraries =
                                StringBuilder(game.orElse(context.getString(R.string.message_unknown)))
                            val analyzer = LibraryAnalyzer.analyze(resolved, game.orElse(null))
                            for (mark in analyzer) {
                                ensureActive()
                                val libraryId = mark.libraryId
                                val libraryVersion = mark.libraryVersion
                                if (libraryId == LibraryAnalyzer.LibraryType.MINECRAFT.patchId) continue
                                if (AndroidUtils.hasStringId(
                                        context,
                                        "install_installer_" + libraryId.replace("-", "_")
                                    )
                                ) {
                                    libraries.append(", ").append(
                                        AndroidUtils.getLocalizedText(
                                            context,
                                            "install_installer_" + libraryId.replace("-", "_")
                                        )
                                    )
                                    if (libraryVersion != null) libraries.append(": ").append(
                                        libraryVersion.replace(
                                            ("(?i)$libraryId").toRegex(),
                                            ""
                                        )
                                    )
                                }
                            }
                            var tag: String? = null
                            try {
                                val config: ModpackConfiguration<*>? =
                                    profile.repository.readModpackConfiguration<Any?>(
                                        version.id
                                    )
                                if (config != null) tag = config.version
                            } catch (e: IOException) {
                                Logging.LOG.log(
                                    Level.WARNING,
                                    "Failed to read modpack configuration from $version",
                                    e
                                )
                            } catch (e: JsonParseException) {
                                Logging.LOG.log(
                                    Level.WARNING,
                                    "Failed to read modpack configuration from $version",
                                    e
                                )
                            }
                            val icon = repository.getVersionIconImage(analyzer, version.id)
                            // Đếm số Mod thực hiện trong parallel stream ở luồng IO (tránh IO thư mục trên luồng chính lúc vuốt);
                            // use để đóng DirectoryStream, không thì rò rỉ file descriptor (CloseGuard báo tài nguyên chưa đóng)
                            val modCount = runCatching {
                                Files.list(repository.getModsDirectory(version.id)).use { stream ->
                                    stream.filter { it.isRegularFile() }.count().toInt()
                                }
                            }.getOrNull() ?: 0
                            return@map VersionListItem(
                                profile,
                                version.id,
                                libraries.toString(),
                                tag,
                                icon,
                                modCount
                            )
                        }
                        .collect(Collectors.toList())
                }
                // Trong lúc tải có thể đã đổi profile hoặc tải lại, bỏ kết quả quá hạn
                if (loadJob !== job) return@launch
                children = result
                if (profile == getSelectedProfile()) {
                    if (adapter == null) {
                        adapter = VersionListAdapter(
                            context,
                            children
                        )
                        binding.versionList.adapter = adapter
                        binding.versionList.layoutManager = LinearLayoutManager(context)
                    } else {
                        adapter!!.updateVersionList(children)
                    }
                    binding.refresh.isEnabled = true
                    if (children.isNotEmpty()) {
                        binding.layout.visibility = View.VISIBLE
                    }
                    binding.progress.visibility = View.GONE
                    binding.search.addTextChangedListener(textWatcher)
                    val selected = children.find { it.selectedProperty().get() }
                    if (selected != null) {
                        binding.versionList.scrollToPosition(children.indexOf(selected))
                    }
                }
                // Nổi bật version đang chọn: cập nhật khi lắng nghe version của profile đổi (thay cho fakefx bind cũ)
                versionHighlightListener?.let { highlightedProfile?.removeSelectedVersionListener(it) }
                val highlightListener = Runnable {
                    children.forEach { item ->
                        item.selectedProperty().set(profile.selectedVersion == item.version)
                    }
                }
                versionHighlightListener = highlightListener
                highlightedProfile = profile
                profile.addSelectedVersionListener(highlightListener)
                children.forEach { item ->
                    item.selectedProperty().set(profile.selectedVersion == item.version)
                }
            }
        }
        loadJob = job
    }

    override fun onClick(view: View?) {
        if (view === binding.refresh) {
            getSelectedProfile().repository.refreshVersionsAsync().start()
        }
        if (view === binding.newProfile) {
            val dialog = AddProfileDialog(context)
            dialog.show()
        }
    }
}
