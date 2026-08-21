package com.tungsten.fcllibrary.component.view

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import com.tungsten.fcl.R
import com.tungsten.fcllibrary.component.theme.ThemeEngine

class FCLConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    var autoTint = false

    init {
        context.withStyledAttributes(attrs, R.styleable.FCLConstraintLayout) {
            autoTint = getBoolean(R.styleable.FCLConstraintLayout_auto_tint, false)
        }
        ThemeEngine.getInstance().registerEvent(this) {
            if (autoTint) {
                // Màu card tối (đồng bộ launcher PC) thay vì màu accent nhạt —
                // vùng lớn không nên phủ đặc màu nhấn.
                setBackgroundTintList(
                    ColorStateList(
                        arrayOf<IntArray?>(intArrayOf()),
                        intArrayOf(resources.getColor(R.color.card_bg, null))
                    )
                )
            }
        }
    }
}
