package com.tungsten.fcl.setting

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.JsonParser
import com.mio.data.Renderer
import com.tungsten.fclauncher.utils.FCLPath
import com.tungsten.fclcore.util.gson.JsonUtils
import com.tungsten.fclcore.util.platform.MemoryUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

/**
 * Kiểm chứng việc tái cấu trúc VersionSetting (fakefx property → field kiểu thường + thông báo qua callback):
 * Giá trị mặc định, thông báo của setter, tính toàn vẹn khi clone, serialize/deserialize JSON.
 */
@RunWith(AndroidJUnit4::class)
class VersionSettingTest {

    @Before
    fun setup() {
        FCLPath.loadPaths(ApplicationProvider.getApplicationContext<Context>())
    }

    private fun defaultMemory() = MemoryUtils.findBestRAMAllocation(FCLPath.CONTEXT)

    @Test
    fun defaultValues() {
        val vs = VersionSetting()
        assertTrue(vs.isUsesGlobal)
        assertEquals("Auto", vs.java)
        assertEquals("", vs.uuid)
        assertEquals(defaultMemory(), vs.maxMemory)
        assertNull(vs.minMemory)
        assertTrue(vs.isAutoMemory)
        assertEquals("", vs.javaArgs)
        assertEquals("", vs.minecraftArgs)
        assertFalse(vs.isNotCheckJVM)
        assertFalse(vs.isNotCheckGame)
        assertEquals("", vs.serverIp)
        assertTrue(vs.isIsolateGameDir)
        assertEquals("default", vs.graphicsBackend)
        assertFalse(vs.isVKDriverSystem)
        assertEquals("00000000", vs.controller)
        assertEquals(Renderer.ID_NGGL4ES, vs.renderer)
        assertEquals("Turnip", vs.driver)
        assertFalse(vs.isPojavBigCore)
        assertFalse(vs.isNotCheckMod)
        assertFalse(vs.isDebugLog)
        assertFalse(vs.isForceResolution)
    }

    /** Setter của mọi field có thể ghi đều kích hoạt 1 lần thông báo listener (khi giá trị đổi) */
    @Test
    fun settersNotifyOnChangeListener() {
        val vs = VersionSetting()
        val notified = AtomicInteger(0)
        val listener = Runnable { notified.incrementAndGet() }
        vs.addOnChangeListener(listener)
        try {
            vs.isUsesGlobal = false
            vs.java = "Java 17"
            vs.maxMemory = defaultMemory() + 1
            vs.minMemory = 1024
            vs.isAutoMemory = false
            vs.javaArgs = "-Xmx2G"
            vs.minecraftArgs = "--server localhost"
            vs.isNotCheckJVM = true
            vs.isNotCheckGame = true
            vs.serverIp = "127.0.0.1:25565"
            vs.isIsolateGameDir = false
            vs.graphicsBackend = "vulkan"
            vs.isVKDriverSystem = true
            vs.controller = "12345678"
            vs.renderer = "gl4es"
            vs.driver = "Mesa"
            vs.isPojavBigCore = true
            vs.uuid = "uuid-1"
            vs.isNotCheckMod = true
            vs.isDebugLog = true
            vs.isForceResolution = true
            assertEquals(21, notified.get())
        } finally {
            vs.removeOnChangeListener(listener)
        }
    }

    /** Ghi giá trị giống nhau không kích hoạt thông báo (tránh gán idempotent ở đường đọc kích hoạt lưu cấu hình lặp lại) */
    @Test
    fun sameValueSetDoesNotNotify() {
        val vs = VersionSetting()
        val notified = AtomicInteger(0)
        val listener = Runnable { notified.incrementAndGet() }
        vs.addOnChangeListener(listener)
        try {
            vs.isUsesGlobal = true
            vs.java = "Auto"
            vs.controller = "00000000"
            vs.driver = "Turnip"
            vs.maxMemory = vs.maxMemory
            assertEquals(0, notified.get())
            vs.maxMemory = vs.maxMemory + 1
            assertEquals(1, notified.get())
        } finally {
            vs.removeOnChangeListener(listener)
        }
    }

    @Test
    fun removeOnChangeListenerStopsNotification() {
        val vs = VersionSetting()
        val notified = AtomicInteger(0)
        val listener = Runnable { notified.incrementAndGet() }
        vs.addOnChangeListener(listener)
        vs.removeOnChangeListener(listener)
        vs.java = "Java 17"
        assertEquals(0, notified.get())
    }

    /** Tự xóa listener của mình trong callback không crash (sao chép list trước khi duyệt) */
    @Test
    fun listenerCanRemoveItselfDuringNotification() {
        val vs = VersionSetting()
        val otherCount = AtomicInteger(0)
        val selfRemoving = object : Runnable {
            override fun run() {
                vs.removeOnChangeListener(this)
            }
        }
        val other = Runnable { otherCount.incrementAndGet() }
        vs.addOnChangeListener(selfRemoving)
        vs.addOnChangeListener(other)
        vs.java = "Java 17"
        // selfRemoving bị xóa trong lúc duyệt, không ảnh hưởng các listener chạy sau
        assertEquals(1, otherCount.get())
        // Đổi lần nữa không kích hoạt thông báo nào (selfRemoving đã bị xóa, other vẫn còn)
        vs.java = "Java 21"
        assertEquals(2, otherCount.get())
    }

    /** Sau khi đặt hết field, clone sao chép đầy đủ và độc lập với nhau */
    @Test
    fun cloneCopiesAllFieldsAndIsIndependent() {
        val vs = VersionSetting().apply {
            isUsesGlobal = false
            java = "Java 17"
            maxMemory = 2048
            minMemory = 1024
            isAutoMemory = false
            javaArgs = "-Xmx2G"
            minecraftArgs = "--server localhost"
            isNotCheckJVM = true
            isNotCheckGame = true
            serverIp = "127.0.0.1:25565"
            isIsolateGameDir = false
            graphicsBackend = "vulkan"
            isVKDriverSystem = true
            controller = "12345678"
            renderer = "gl4es"
            driver = "Mesa"
            isPojavBigCore = true
            uuid = "uuid-1"
            isNotCheckMod = true
            isDebugLog = true
            isForceResolution = true
        }
        val copy = vs.clone()
        assertNotSame(vs, copy)
        assertEquals(vs.isUsesGlobal, copy.isUsesGlobal)
        assertEquals(vs.java, copy.java)
        assertEquals(vs.maxMemory, copy.maxMemory)
        assertEquals(vs.minMemory, copy.minMemory)
        assertEquals(vs.isAutoMemory, copy.isAutoMemory)
        assertEquals(vs.javaArgs, copy.javaArgs)
        assertEquals(vs.minecraftArgs, copy.minecraftArgs)
        assertEquals(vs.isNotCheckJVM, copy.isNotCheckJVM)
        assertEquals(vs.isNotCheckGame, copy.isNotCheckGame)
        assertEquals(vs.serverIp, copy.serverIp)
        assertEquals(vs.isIsolateGameDir, copy.isIsolateGameDir)
        assertEquals(vs.graphicsBackend, copy.graphicsBackend)
        assertEquals(vs.isVKDriverSystem, copy.isVKDriverSystem)
        assertEquals(vs.controller, copy.controller)
        assertEquals(vs.renderer, copy.renderer)
        assertEquals(vs.driver, copy.driver)
        assertEquals(vs.isPojavBigCore, copy.isPojavBigCore)
        assertEquals(vs.uuid, copy.uuid)
        assertEquals(vs.isNotCheckMod, copy.isNotCheckMod)
        assertEquals(vs.isDebugLog, copy.isDebugLog)
        assertEquals(vs.isForceResolution, copy.isForceResolution)
        // Sửa bản sao không ảnh hưởng object gốc
        copy.java = "Changed"
        copy.maxMemory = 1
        assertEquals("Java 17", vs.java)
        assertEquals(2048, vs.maxMemory)
    }

    /** Serialize/deserialize khứ hồi toàn bộ field giữ nhất quán (tên java không nhận diện được thì về Auto, có test case riêng bao phủ) */
    @Test
    fun serializerRoundTripPreservesFields() {
        val vs = VersionSetting().apply {
            isUsesGlobal = false
            java = "Auto"
            maxMemory = 2048
            minMemory = 512
            isAutoMemory = false
            javaArgs = "-Xmx2G"
            minecraftArgs = "--server localhost"
            isNotCheckJVM = true
            isNotCheckGame = true
            serverIp = "127.0.0.1:25565"
            isIsolateGameDir = false
            graphicsBackend = "vulkan"
            isVKDriverSystem = true
            controller = "12345678"
            renderer = "gl4es"
            driver = "Mesa"
            isPojavBigCore = true
            uuid = "uuid-1"
            isNotCheckMod = true
            isDebugLog = true
            isForceResolution = true
        }
        val restored = JsonUtils.GSON.fromJson<VersionSetting>(JsonUtils.GSON.toJson(vs), VersionSetting::class.java)
        assertFalse(restored.isUsesGlobal)
        assertEquals("Auto", restored.java)
        assertEquals(2048, restored.maxMemory)
        assertEquals(512, restored.minMemory)
        assertFalse(restored.isAutoMemory)
        assertEquals("-Xmx2G", restored.javaArgs)
        assertEquals("--server localhost", restored.minecraftArgs)
        assertTrue(restored.isNotCheckJVM)
        assertTrue(restored.isNotCheckGame)
        assertEquals("127.0.0.1:25565", restored.serverIp)
        assertFalse(restored.isIsolateGameDir)
        assertEquals("vulkan", restored.graphicsBackend)
        assertTrue(restored.isVKDriverSystem)
        assertEquals("12345678", restored.controller)
        assertEquals("gl4es", restored.renderer)
        assertEquals("Mesa", restored.driver)
        assertTrue(restored.isPojavBigCore)
        assertEquals("uuid-1", restored.uuid)
        assertTrue(restored.isNotCheckMod)
        assertTrue(restored.isDebugLog)
        assertTrue(restored.isForceResolution)
    }

    /** Deserialize object rỗng: field thiếu dùng giá trị mặc định khi deserialize */
    @Test
    fun deserializeEmptyObjectUsesDefaults() {
        val restored = JsonUtils.GSON.fromJson<VersionSetting>("{}", VersionSetting::class.java)
        // Giá trị mặc định khi deserialize (khác giá trị mặc định của field ở chỗ: usesGlobal/isolateGameDir thiếu thì là false)
        assertFalse(restored.isUsesGlobal)
        assertFalse(restored.isIsolateGameDir)
        assertTrue(restored.isAutoMemory)
        assertEquals(defaultMemory(), restored.maxMemory)
        assertNull(restored.minMemory)
        assertEquals("Auto", restored.java)
        assertEquals("00000000", restored.controller)
        assertEquals(Renderer.ID_NGGL4ES, restored.renderer)
        assertEquals("Turnip", restored.driver)
        assertEquals("default", restored.graphicsBackend)
        assertEquals("", restored.javaArgs)
        assertEquals("", restored.minecraftArgs)
    }

    /** Khi có 1 phần field, các field còn lại dùng giá trị mặc định */
    @Test
    fun deserializePartialJsonFillsDefaults() {
        val restored = JsonUtils.GSON.fromJson<VersionSetting>(
            """{"maxMemory":1024,"autoMemory":false,"serverIp":"1.2.3.4"}""",
            VersionSetting::class.java
        )
        assertEquals(1024, restored.maxMemory)
        assertFalse(restored.isAutoMemory)
        assertEquals("1.2.3.4", restored.serverIp)
        assertNull(restored.minMemory)
        assertEquals("Auto", restored.java)
    }

    /** Khi maxMemory <= 0, cả serialize và deserialize đều về mức bộ nhớ khuyến nghị */
    @Test
    fun nonPositiveMaxMemoryFallsBack() {
        val vs = VersionSetting().apply { maxMemory = 0 }
        val json = JsonParser.parseString(JsonUtils.GSON.toJson(vs)).asJsonObject
        assertEquals(defaultMemory(), json.get("maxMemory").asInt)
        val restored = JsonUtils.GSON.fromJson<VersionSetting>("""{"maxMemory":0}""", VersionSetting::class.java)
        assertEquals(defaultMemory(), restored.maxMemory)
    }

    /** Vẫn phân tích được khi maxMemory trong JSON là số dạng chuỗi */
    @Test
    fun deserializeMaxMemoryAsStringNumber() {
        val restored = JsonUtils.GSON.fromJson<VersionSetting>("""{"maxMemory":"2048"}""", VersionSetting::class.java)
        assertEquals(2048, restored.maxMemory)
    }

    /** Tên java không rõ sẽ về Auto */
    @Test
    fun deserializeUnknownJavaFallsBackToAuto() {
        val restored = JsonUtils.GSON.fromJson<VersionSetting>("""{"java":"Not Installed JDK"}""", VersionSetting::class.java)
        assertEquals("Auto", restored.java)
    }
}
