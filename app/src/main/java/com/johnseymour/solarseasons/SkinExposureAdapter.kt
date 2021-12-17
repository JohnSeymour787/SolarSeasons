package com.johnseymour.solarseasons

import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_cell_skin_exposure.view.*
import kotlinx.android.synthetic.main.list_cell_sun_info.view.*
import java.time.ZonedDateTime

class SkinExposureAdapter(private val exposureTimes: List<Map.Entry<String, Int>>, private val textColorInt: Int): RecyclerView.Adapter<SkinExposureAdapter.SkinExposureViewHolder>()
{
    inner class SkinExposureViewHolder(view: SkinExposureViewCell): RecyclerView.ViewHolder(view)
    {
        internal fun bind(exposureTimeEntry: Map.Entry<String, Int>)
        {
            itemView.apply()
            {
                skinType.text = exposureTimeEntry.key
                exposureTime.text = exposureTimeEntry.value.toString()//preferredTimeString(context, sunTime.second)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkinExposureViewHolder
    {
        val cell = SkinExposureViewCell(parent.context).apply()
        {
            layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
          //  layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, resources.getDimensionPixelSize(R.dimen.cell_sun_info_height))
            skinType.setTextColor(resources.getColor(textColorInt, context.theme))
            exposureTime.setTextColor(resources.getColor(textColorInt, context.theme))
        }

        return SkinExposureViewHolder(cell)
    }

    override fun onBindViewHolder(holder: SkinExposureViewHolder, position: Int)
    {
        holder.bind(exposureTimes[position])
    }

    override fun getItemCount(): Int = exposureTimes.size
}