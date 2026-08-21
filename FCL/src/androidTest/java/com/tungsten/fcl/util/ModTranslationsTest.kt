package com.tungsten.fcl.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tungsten.fclcore.mod.RemoteModRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Kiểm chứng việc preload nền của ModTranslations: tính idempotent và nhất quán dữ liệu.
 */
@RunWith(AndroidJUnit4::class)
class ModTranslationsTest {

    @Test
    fun preloadIsIdempotent() {
        val before = ModTranslations.MOD.getMods()
        // Preload và preload lặp lại không ném exception
        ModTranslations.MOD.preload()
        ModTranslations.MOD.preload()
        // Instance dữ liệu nhất quán sau nhiều lần gọi (tái dùng cache nội bộ)
        assertSame(before, ModTranslations.MOD.getMods())
    }

    @Test
    fun getTranslationsByRepositoryType() {
        assertEquals(
            ModTranslations.MOD,
            ModTranslations.getTranslationsByRepositoryType(RemoteModRepository.Type.MOD)
        )
        assertEquals(
            ModTranslations.MODPACK,
            ModTranslations.getTranslationsByRepositoryType(RemoteModRepository.Type.MODPACK)
        )
        // Loại khác dùng bản dịch rỗng
        assertEquals(
            ModTranslations.EMPTY,
            ModTranslations.getTranslationsByRepositoryType(RemoteModRepository.Type.RESOURCE_PACK)
        )
        assertEquals(
            ModTranslations.EMPTY,
            ModTranslations.getTranslationsByRepositoryType(RemoteModRepository.Type.SHADER_PACK)
        )
    }

    @Test
    fun emptyTranslationsReturnsEmptyMods() {
        assertEquals(0, ModTranslations.EMPTY.getMods().size)
    }
}
