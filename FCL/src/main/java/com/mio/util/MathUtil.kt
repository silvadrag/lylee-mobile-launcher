package com.mio.util

import android.content.Context
import com.tungsten.fcllibrary.util.LocaleUtils

fun Long.format(context: Context): String {
    val isChinese = LocaleUtils.isChinese(context)
    val absNum = kotlin.math.abs(this)
    val sign = if (this < 0) "-" else ""

    return when {
        // Định dạng tiếng Trung
        isChinese && absNum >= 100_000_000 -> {
            val value = absNum / 100_000_000.0
            sign + formatNumber(value) + "ức"
        }

        isChinese && absNum >= 10_000 -> {
            val value = absNum / 10_000.0
            sign + formatNumber(value) + "vạn"
        }
        // Định dạng tiếng Anh
        !isChinese && absNum >= 1_000_000_000 -> {
            val value = absNum / 1_000_000_000.0
            sign + formatNumber(value) + "B"
        }

        !isChinese && absNum >= 1_000_000 -> {
            val value = absNum / 1_000_000.0
            sign + formatNumber(value) + "M"
        }

        !isChinese && absNum >= 1_000 -> {
            val value = absNum / 1_000.0
            sign + formatNumber(value) + "K"
        }

        else -> this.toString()
    }
}

fun Int.format(context: Context): String =
    toLong().format(context)

/** Giữ 2 chữ số thập phân, bỏ số 0 và dấu chấm vô nghĩa ở cuối */
private fun formatNumber(value: Double): String {
    val formatted = "%.2f".format(value)
    return formatted.trimEnd('0').trimEnd('.')
}