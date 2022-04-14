package com.johnseymour.solarseasons.current_uv_screen.uv_forecast

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.johnseymour.solarseasons.R

class UVForecastViewCell(context: Context, attrs: AttributeSet? = null): ConstraintLayout(context, attrs)
{
    init { (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as? LayoutInflater)?.inflate(R.layout.list_cell_uv_forecast, this, true) }
}