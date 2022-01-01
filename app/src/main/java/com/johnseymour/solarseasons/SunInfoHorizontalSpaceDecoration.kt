package com.johnseymour.solarseasons

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SunInfoHorizontalSpaceDecoration(private val spacingGap: Int) : RecyclerView.ItemDecoration()
{
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State)
    {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.right = spacingGap
        outRect.left = spacingGap
    }
}