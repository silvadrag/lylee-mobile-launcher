package com.tungsten.fcl.setting

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tungsten.fclauncher.utils.FCLPath
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * Kiểm chứng chuyên sâu việc chuyển Profiles.selectedProfile sang Repository đơn lẻ + StateFlow:
 * Cập nhật giá trị StateFlow, thông báo listener, kiểm tra và về mặc định mục đang chọn, hành vi collect.
 */
@RunWith(AndroidJUnit4::class)
class ProfilesTest {

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        FCLPath.loadPaths(context)
        if (!ConfigHolder.isInit()) {
            ConfigHolder.init()
        }
        Profiles.init()
    }

    @Test
    fun initSelectsConfiguredProfile() {
        val selected = Profiles.getSelectedProfile()
        assertNotNull(selected)
        // Giá trị hiện tại của StateFlow khớp với getSelectedProfile
        assertEquals(selected, Profiles.selectedProfile.value)
        // Mục đang chọn phải nằm trong list profiles
        assertTrue(Profiles.profiles.contains(selected))
    }

    @Test
    fun setSelectedProfileUpdatesStateFlowAndNotifiesListeners() {
        // Chọn profile khác giá trị hiện tại (StateFlow không phát lại nếu giá trị giống nhau)
        val current = Profiles.selectedProfile.value
        val target = Profiles.profiles.first { it != current }
        val notified = AtomicInteger(0)
        val listener = Runnable { notified.incrementAndGet() }
        Profiles.addSelectedProfileListener(listener)
        try {
            Profiles.setSelectedProfile(target)
            // StateFlow cập nhật đồng bộ với API đọc
            assertEquals(target, Profiles.selectedProfile.value)
            assertEquals(target, Profiles.getSelectedProfile())
            // Listener nhận 1 lần thông báo (callback đồng bộ)
            assertEquals(1, notified.get())
        } finally {
            Profiles.removeSelectedProfileListener(listener)
        }
    }

    @Test
    fun setSelectedProfileSameValueDoesNotNotify() {
        val current = Profiles.getSelectedProfile()
        var notified = 0
        val listener = Runnable { notified++ }
        Profiles.addSelectedProfileListener(listener)
        try {
            Profiles.setSelectedProfile(current)
            // StateFlow không phát lại giá trị giống nhau, listener không thông báo lặp lại
            assertEquals(0, notified)
        } finally {
            Profiles.removeSelectedProfileListener(listener)
        }
    }

    @Test
    fun setSelectedProfileFallsBackToFirstWhenNotInList() {
        val ghost = Profile("ghost", File("/sdcard/ghost_dir"))
        Profiles.setSelectedProfile(ghost)
        // Profile không có trong list sẽ về mặc định là cái đầu tiên
        assertEquals(Profiles.profiles[0], Profiles.getSelectedProfile())
        assertEquals(Profiles.profiles[0], Profiles.selectedProfile.value)
    }

    @Test
    fun removingSelectedProfileFallsBack() {
        val first = Profiles.profiles[0]
        Profiles.removeProfile(first)
        try {
            // Kiểm tra khi list đổi: mục đang chọn không có trong list thì về cái đầu tiên
            assertEquals(Profiles.profiles[0], Profiles.getSelectedProfile())
        } finally {
            Profiles.profiles.add(0, first)
        }
    }

    @Test
    fun selectedProfileCanBeCollected() {
        // Chọn profile khác giá trị hiện tại (StateFlow không phát lại nếu giá trị giống nhau)
        val current = Profiles.selectedProfile.value
        val target = Profiles.profiles.first { it != current }
        val values = mutableListOf<Profile?>()
        runBlocking {
            val job = launch {
                Profiles.selectedProfile.collect { values.add(it) }
            }
            // Để coroutine collect khởi động và nhận giá trị ban đầu trước, rồi mới cập nhật
            yield()
            Profiles.setSelectedProfile(target)
            delay(100)
            job.cancel()
        }
        // Lần thu thập đầu phát ngay giá trị hiện tại, cập nhật xong phát thêm lần nữa
        assertEquals(2, values.size)
        assertEquals(target, values.last())
    }

    @Test
    fun selectedVersionFollowsSelectedProfile() {
        val current = Profiles.selectedProfile.value
        val target = Profiles.profiles.first { it != current }
        // bind phụ thuộc repository đã tải (isLoaded), làm mới đồng bộ trước
        target.repository.refreshVersions()
        assertTrue("repository chưa được tải", target.repository.isLoaded)
        Profiles.setSelectedProfile(target)
        // selectedVersion sau khi bind bằng version đang chọn của profile đích
        assertEquals(target.selectedVersion, Profiles.getSelectedVersion())
        assertEquals(target.selectedVersion, Profiles.selectedVersion.value)
        // Sự kiện làm mới repository kích hoạt lại bind, giữ nhất quán
        target.repository.refreshVersions()
        assertEquals(target.selectedVersion, Profiles.getSelectedVersion())
    }

    @Test
    fun addProfileAppendsToList() {
        val before = Profiles.profiles.size
        val newProfile = Profile("NewProfile_${System.nanoTime()}", File(Profiles.getSelectedProfile().gameDir, "new_dir"))
        Profiles.addProfile(newProfile)
        try {
            assertTrue(Profiles.profiles.contains(newProfile))
            assertEquals(before + 1, Profiles.profiles.size)
        } finally {
            Profiles.removeProfile(newProfile)
        }
    }

    @Test
    fun addProfileDoesNotChangeSelection() {
        val selected = Profiles.getSelectedProfile()
        val newProfile = Profile("NewProfile_${System.nanoTime()}", File(selected.gameDir, "new_dir"))
        Profiles.addProfile(newProfile)
        try {
            // Thêm profile mới không ảnh hưởng mục đang chọn hiện tại
            assertEquals(selected, Profiles.getSelectedProfile())
            assertEquals(selected, Profiles.selectedProfile.value)
        } finally {
            Profiles.removeProfile(newProfile)
        }
    }

    @Test
    fun removingSelectedProfileFallsBackInStateFlow() {
        val first = Profiles.profiles[0]
        Profiles.removeProfile(first)
        try {
            // Sau khi mục đang chọn bị xóa, StateFlow và API đọc đồng bộ về cái đầu tiên
            assertEquals(Profiles.profiles[0], Profiles.getSelectedProfile())
            assertEquals(Profiles.profiles[0], Profiles.selectedProfile.value)
        } finally {
            Profiles.profiles.add(0, first)
            Profiles.setSelectedProfile(first)
        }
    }

    @Test
    fun versionSettingFallsBackToGlobalWhenNoSelectedVersion() {
        val profile = Profiles.getSelectedProfile()
        profile.selectedVersion = null
        profile.repository.refreshVersions()
        // Trả về cài đặt toàn cục khi không có version đang chọn
        assertEquals(profile.globalVersionSetting, profile.versionSetting)
        assertEquals(profile.globalVersionSetting, profile.getVersionSetting(null))
    }
}
