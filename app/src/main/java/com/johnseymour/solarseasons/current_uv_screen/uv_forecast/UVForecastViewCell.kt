package com.johnseymour.solarseasons.current_uv_screen.uv_forecast

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.johnseymour.solarseasons.databinding.ListCellUvForecastBinding

class UVForecastViewCell(context: Context, attrs: AttributeSet? = null): ConstraintLayout(context, attrs)
{
    private var _binding: ListCellUvForecastBinding? = null
    val binding get() = _binding!!

    init
    {
        (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as? LayoutInflater)?.let()
        {
            _binding = ListCellUvForecastBinding.inflate(it, this)
        }
    }

    override fun onDetachedFromWindow()
    {
        super.onDetachedFromWindow()
        _binding = null
    }
}