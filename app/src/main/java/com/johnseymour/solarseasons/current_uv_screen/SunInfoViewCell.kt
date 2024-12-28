package com.johnseymour.solarseasons.current_uv_screen

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.johnseymour.solarseasons.databinding.ListCellSunInfoBinding

class SunInfoViewCell(context: Context, attrs: AttributeSet? = null): ConstraintLayout(context, attrs)
{
    private var _binding: ListCellSunInfoBinding? = null
    val binding get() = _binding!!

    init
    {
        (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as? LayoutInflater)?.let()
        {
            _binding = ListCellSunInfoBinding.inflate(it, this)
        }
    }

    override fun onDetachedFromWindow()
    {
        super.onDetachedFromWindow()
        _binding = null
    }
}