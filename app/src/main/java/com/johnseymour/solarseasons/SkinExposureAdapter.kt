package com.johnseymour.solarseasons

import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.johnseymour.solarseasons.settings_screen.PreferenceScreenFragment
import kotlinx.android.synthetic.main.list_cell_skin_exposure.view.*

class SkinExposureAdapter(private val exposureTimes: List<Map.Entry<String, Int>>, private val textColorInt: Int): RecyclerView.Adapter<SkinExposureAdapter.SkinExposureViewHolder>()
{
    inner class SkinExposureViewHolder(view: SkinExposureViewCell): RecyclerView.ViewHolder(view)
    {
        internal fun bind(exposureTimeEntry: Map.Entry<String, Int>)
        {
            itemView.apply()
            {
                skinIcon.drawable.setTint(resources.getColor(UVData.skinTypeColorInt(exposureTimeEntry.key), context.theme))
                skinType.text = resources.getString(UVData.skinTypeNameInt(exposureTimeEntry.key))
                exposureTime.text = exposureDurationString(resources, exposureTimeEntry.value)
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
                skinType.setTextColor(resources.getColor(textColorInt, context.theme))
                exposureTime.setTextColor(resources.getColor(textColorInt, context.theme))
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