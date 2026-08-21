package com.tungsten.fcl.ui.download.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mio.ui.adapter.ViewHolder
import com.mio.util.AnimUtil.Companion.playTranslationX
import com.mio.util.format
import com.tungsten.fcl.R
import com.tungsten.fcl.activity.MainActivity
import com.tungsten.fcl.databinding.ItemRemoteModBinding
import com.tungsten.fcl.ui.download.DownloadUI
import com.tungsten.fcl.util.ModTranslations
import com.tungsten.fclcore.mod.LocalModFile
import com.tungsten.fclcore.mod.RemoteMod
import com.tungsten.fclcore.util.Logging
import com.tungsten.fclcore.util.StringUtils
import com.tungsten.fcllibrary.component.theme.ThemeEngine
import com.tungsten.fcllibrary.util.LocaleUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.logging.Level
import java.util.stream.Collectors

class RemoteModListAdapter(
    private val context: Context,
    private val downloadPage: DownloadPage,
    private val list: ArrayList<RemoteMod>,
    private val callback: Callback
) : RecyclerView.Adapter<ViewHolder>() {
    private val modIdList: MutableList<String?> = ArrayList()

    init {
        MainActivity.getInstance().lifecycleScope.launch(Dispatchers.Default) {
            // Làm nóng dữ liệu dịch Mod ở nền, tránh lần bind đầu phân tích file lớn ở luồng chính gây giật
            ModTranslations.getTranslationsByRepositoryType(downloadPage.repository.getType())
                .preload()
            if (downloadPage.pageId == DownloadUI.PAGE_ID_DOWNLOAD_MOD) {
                val modManager = downloadPage.modManager
                val modFiles = runCatching {
                    modManager.getMods().parallelStream().collect(Collectors.toList())
                }.getOrNull() ?: emptyList<LocalModFile>()
                for (localModFile in modFiles) {
                    try {
                        val size = localModFile.file.toFile().length()
                        if (size > 104857600) continue
                        val remoteVersionOptional = downloadPage.getRepository()
                            .getRemoteVersionByLocalFile(localModFile, localModFile.file)
                        remoteVersionOptional.ifPresent {
                            localModFile.remoteVersion = it
                        }
                        localModFile.remoteVersion?.let {
                            modIdList.add(it.modid)
                        }
                    } catch (e: Throwable) {
                        System.gc()
                        Logging.LOG.log(Level.SEVERE, e.toString())
                    }
                }
            }
        }
    }

    interface Callback {
        fun onItemSelect(mod: RemoteMod?)
    }

    companion object {
        /** Cache bitmap placeholder (nội dung chỉ đọc, chia sẻ an toàn giữa nhiều view), tránh mỗi lần bind phải cấp phát và vẽ lại */
        private var placeholderBitmap: Bitmap? = null
    }

    /** Ảnh placeholder kích thước cố định 90×90 (khớp kích thước ảnh sau khi override, tránh khi tải xong
     *  kích thước nội tại drawable đổi kích hoạt requestLayout khiến list sắp xếp lại) */
    private fun fixedIconPlaceholder(): Drawable {
        var bitmap = placeholderBitmap
        if (bitmap == null) {
            bitmap = createBitmap(90, 90)
            val base = ContextCompat.getDrawable(context, R.drawable.ic_cube)!!.mutate()
            base.setBounds(0, 0, 90, 90)
            base.draw(Canvas(bitmap))
            placeholderBitmap = bitmap
        }
        return bitmap.toDrawable(context.resources)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            ItemRemoteModBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            ).root
        )
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val binding = ItemRemoteModBinding.bind(holder.itemView)
        val remoteMod = list[position]
        binding.parent.setOnClickListener {
            callback.onItemSelect(
                remoteMod
            )
        }
        // Placeholder cố định 90×90 (khớp kích thước nội tại ảnh sau khi override): khi ảnh tải xong thay thế
        // kích thước nội tại drawable không đổi, không kích hoạt requestLayout, tránh việc list sắp xếp lại toàn bộ khiến
        // text marquee của item khác bị reset
        binding.icon.setImageDrawable(fixedIconPlaceholder())
        Glide.with(binding.icon)
            .load(remoteMod.iconUrl)
            .placeholder(fixedIconPlaceholder())
            .override(90, 90)
            .error(fixedIconPlaceholder())
            .into(binding.icon)
        val mod =
            ModTranslations.getTranslationsByRepositoryType(downloadPage.repository.getType())
                .getModByCurseForgeId(remoteMod.slug)
        binding.title.text =
            if (mod != null && LocaleUtils.isChinese(context)) mod.getDisplayName() else remoteMod.title
        val categories = remoteMod.categories.stream()
            .map { downloadPage.getLocalizedCategory(it) }
            .collect(
                Collectors.toList()
            ).joinToString("   ")
        val tag = StringUtils.removeSuffix(categories, "   ")
        binding.tag.text = tag
        binding.description.text = remoteMod.description
        binding.downloadCount.text = remoteMod.downloadCount.format(context)
        playTranslationX(
            binding.root,
            ThemeEngine.getInstance().getTheme().animationSpeed * 30L,
            -100f,
            0f
        ).start()
        if (downloadPage.pageId == DownloadUI.PAGE_ID_DOWNLOAD_MOD) {
            if (modIdList.isNotEmpty() && modIdList.contains(remoteMod.modID)) {
                val text = binding.title.getText().toString()
                if (!text.startsWith(context.getString(R.string.installed))) {
                    binding.title.text = String.format(
                        "[%s] %s",
                        context.getString(R.string.installed),
                        text
                    )
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}
