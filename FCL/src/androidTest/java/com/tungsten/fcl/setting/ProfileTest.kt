package com.tungsten.fcl.setting

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tungsten.fclauncher.utils.FCLPath
import com.tungsten.fclcore.util.gson.JsonUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * Kiểm chứng việc tái cấu trúc Profile (fakefx property → field kiểu thường + callback lắng nghe):
 * Serialize/deserialize, kiểm tra và thông báo selectedVersion, chuyển đổi thư mục game.
 */
@RunWith(AndroidJUnit4::class)
class ProfileTest {

    private lateinit var tempDir: File

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        FCLPath.loadPaths(context)
        tempDir = File(context.cacheDir, "profile_test_${System.nanoTime()}")
        tempDir.mkdirs()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun writeVersion(id: String, parent: File = tempDir, mainClass: String? = null) {
        val dir = File(parent, "versions/$id")
        dir.mkdirs()
        val json = buildString {
            append("{\"id\":\"$id\"")
            if (mainClass != null) append(",\"mainClass\":\"$mainClass\"")
            append("}")
        }
        File(dir, "$id.json").writeText(json)
    }

    @Test
    fun serializerRoundTripPreservesFields() {
        val global = VersionSetting().apply {
            maxMemory = 2048
            serverIp = "1.2.3.4"
            isUsesGlobal = false
        }
        val profile = Profile("TestProfile", File(tempDir, "game"), global, "1.0")
        val restored = JsonUtils.GSON.fromJson<Profile>(JsonUtils.GSON.toJson(profile), Profile::class.java)
        assertEquals(File(tempDir, "game"), restored.gameDir)
        assertEquals("1.0", restored.selectedVersion)
        assertEquals(2048, restored.globalVersionSetting.maxMemory)
        assertEquals("1.2.3.4", restored.globalVersionSetting.serverIp)
        assertFalse(restored.globalVersionSetting.isUsesGlobal)
        // Tên cố định là "Default" khi deserialize (Serializer không lưu bền name)
        assertEquals("Default", restored.name)
    }

    @Test
    fun selectedVersionSetterNotifiesAndSameValueSkips() {
        val profile = Profile("Test", tempDir)
        val notified = AtomicInteger(0)
        val listener = Runnable { notified.incrementAndGet() }
        profile.addSelectedVersionListener(listener)
        try {
            profile.selectedVersion = "1.0"
            assertEquals(1, notified.get())
            // Giá trị giống nhau thì không thông báo
            profile.selectedVersion = "1.0"
            assertEquals(1, notified.get())
            profile.selectedVersion = "2.0"
            assertEquals(2, notified.get())
        } finally {
            profile.removeSelectedVersionListener(listener)
        }
        profile.selectedVersion = "3.0"
        assertEquals(2, notified.get())
    }

    /** Thêm/xóa listener trong callback không crash (sao chép list trước khi duyệt) */
    @Test
    fun listenerCanRemoveItselfDuringNotification() {
        val profile = Profile("Test", tempDir)
        val otherCount = AtomicInteger(0)
        val selfRemoving = object : Runnable {
            override fun run() {
                profile.removeSelectedVersionListener(this)
            }
        }
        val other = Runnable { otherCount.incrementAndGet() }
        profile.addSelectedVersionListener(selfRemoving)
        profile.addSelectedVersionListener(other)
        profile.selectedVersion = "1.0"
        assertEquals(1, otherCount.get())
    }

    /** Không kiểm tra selectedVersion khi repository chưa tải */
    @Test
    fun selectedVersionKeptWhenRepositoryNotLoaded() {
        val profile = Profile("Test", tempDir)
        profile.selectedVersion = "ghost"
        assertEquals("ghost", profile.selectedVersion)
    }

    /** Sau khi tải, chọn version không tồn tại sẽ về version đầu tiên */
    @Test
    fun selectedVersionInvalidFallsBackToFirst() {
        writeVersion("1.0")
        val profile = Profile("Test", tempDir)
        profile.repository.refreshVersions()
        assertTrue(profile.repository.isLoaded())
        profile.selectedVersion = "ghost"
        assertEquals("1.0", profile.selectedVersion)
    }

    /** Sau khi tải mà không có version nào, chọn version không tồn tại sẽ bị xóa về null */
    @Test
    fun selectedVersionInvalidClearedWhenNoVersions() {
        val profile = Profile("Test", tempDir)
        profile.repository.refreshVersions()
        assertTrue(profile.repository.isLoaded())
        profile.selectedVersion = "ghost"
        assertNull(profile.selectedVersion)
    }

    /** Version hợp lệ không bị ảnh hưởng bởi việc kiểm tra */
    @Test
    fun selectedVersionValidKept() {
        writeVersion("1.0")
        val profile = Profile("Test", tempDir)
        profile.repository.refreshVersions()
        profile.selectedVersion = "1.0"
        assertEquals("1.0", profile.selectedVersion)
    }

    /** Sau khi đổi thư mục game, repository trỏ tới thư mục mới */
    @Test
    fun gameDirSwitchChangesRepositoryDirectory() {
        val dir1 = File(tempDir, "dir1")
        val dir2 = File(tempDir, "dir2")
        dir1.mkdirs()
        dir2.mkdirs()
        val profile = Profile("Test", dir1)
        profile.repository.refreshVersions()
        assertFalse(profile.repository.hasVersion("1.0"))
        writeVersion("1.0", parent = dir2)
        profile.gameDir = dir2
        profile.repository.refreshVersions()
        assertTrue(profile.repository.hasVersion("1.0"))
    }
}