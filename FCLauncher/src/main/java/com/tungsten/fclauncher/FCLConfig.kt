package com.tungsten.fclauncher

import android.content.Context
import com.mio.data.Renderer
import java.io.Serializable

/**
 * Cấu hình khởi chạy FCL, do launcher xây dựng rồi truyền cho FCLauncher
 */
class FCLConfig(
    val context: Context,
    val logDir: String,
    val javaPath: String,
    val workingDir: String,
    val renderer: Renderer,
    val args: Array<String>
) : Serializable {

    var useVKDriverSystem = false

    var pojavBigCore = false

    var installedModLoaders: InstalledModLoaders? = null

    var lwjglVersion: String = "3.3.3"

    /**
     * Thông tin mod loader đã cài
     */
    data class InstalledModLoaders(
        val installForge: Boolean,
        val installCleanroom: Boolean,
        val installNeoForge: Boolean,
        val installOptiFine: Boolean,
        val installLiteLoader: Boolean,
        val installFabric: Boolean,
        val installQuilt: Boolean
    )
}
