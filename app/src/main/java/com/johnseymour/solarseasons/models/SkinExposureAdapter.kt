package com.johnseymour.solarseasons.models

import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.johnseymour.solarseasons.current_uv_screen.SkinExposureViewCell
import com.johnseymour.solarseasons.exposureDurationString
import com.johnseymour.solarseasons.settings_screen.PreferenceScreenFragment

class SkinExposureAdapter(private val exposureTimes: List<Map.Entry<String, Int>>, private val textColorInt: Int): RecyclerView.Adapter<SkinExposureAdapter.SkinExposureViewHolder>()
{
    inner class SkinExposureViewHolder(view: SkinExposureViewCell): RecyclerView.ViewHolder(view)
    {
        private val viewBinding = view.binding

        internal fun bind(exposureTimeEntry: Map.Entry<String, Int>)
        {
            itemView.apply()
            {
                viewBinding.skinIcon.drawable.setTint(resources.getColor(UVData.skinTypeColorInt(exposureTimeEntry.key), context.theme))
                viewBinding.skinType.text = resources.getString(UVData.skinTypeNameInt(exposureTimeEntry.key))
                viewBinding.exposureTime.text = exposureDurationString(resources, exposureTimeEntry.value)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkinExposureViewHolder
    {
        val cell = SkinExposureViewCell(parent.context).apply()
        {
            layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)

            if (PreferenceScreenFragment.useCustomTheme)
            {
                binding.skinType.setTextColor(resources.getColor(textColorInt, context.theme))
                binding.exposureTime.setTextColor(resources.getColor(textColorInt, context.theme))
            }
        }

        return SkinExposureViewHolder(cell)
    }

    override fun onBindViewHolder(holder: SkinExposureViewHolder, position: Int)
    {
        holder.bind(exposureTimes[position])
    }

    override fun getItemCount(): Int = exposureTimes.size
}