package com.johnseymour.solarseasons

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout

class SkinExposureViewCell(context: Context, attrs: AttributeSet? = null): ConstraintLayout(context, attrs)
{
    init { (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as? LayoutInflater)?.inflate(R.layout.list_cell_skin_exposure, this, true) }
}