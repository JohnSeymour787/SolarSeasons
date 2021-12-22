package com.johnseymour.solarseasons

import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_cell_sun_info.view.*

class SunInfoAdapter(private val sunTimes: List<SunInfo.SunTimeData>, private val textColorInt: Int): RecyclerView.Adapter<SunInfoAdapter.SunInfoViewHolder>()
{
    inner class SunInfoViewHolder(view: SunInfoViewCell): RecyclerView.ViewHolder(view)
    {
        internal fun bind(sunTime: SunInfo.SunTimeData)
        {
            itemView.apply()
            {
                infoTitle.text = resources.getString(sunTime.nameResourceInt)
                infoTime.text = preferredTimeString(context, sunTime.time)
                infoImage.setImageResource(sunTime.imageResourceInt)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SunInfoViewHolder
    {
        val cell = SunInfoViewCell(parent.context).apply()
        {
            layoutParams = ConstraintLayout.LayoutParams(resources.getDimensionPixelSize(R.dimen.cell_sun_info_width), resources.getDimensionPixelSize(R.dimen.cell_sun_info_height))
            infoTitle.setTextColor(resources.getColor(textColorInt, context.theme))
            infoTime.setTextColor(resources.getColor(textColorInt, context.theme))
        }

        return SunInfoViewHolder(cell)
    }

    override fun onBindViewHolder(holder: SunInfoViewHolder, position: Int)
    {
        holder.bind(sunTimes[position])
    }

    override fun getItemCount(): Int = sunTimes.size
}