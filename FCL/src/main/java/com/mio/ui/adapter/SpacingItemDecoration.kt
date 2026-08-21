package com.mio.ui.adapter

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Decorator khoảng cách dọc giữa các dòng list (thay cho đường phân cách thủ công), dòng cuối không thêm khoảng cách.
 */
class SpacingItemDecoration(private val spacingPx: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val itemCount = parent.adapter?.itemCount ?: return
        if (parent.getChildAdapterPosition(view) != itemCount - 1) {
            outRect.bottom = spacingPx
        }
    }
}
