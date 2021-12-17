package com.johnseymour.solarseasons

import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_cell_sun_info.view.*
import java.time.ZonedDateTime

class SunInfoAdapter(private val sunTimes: List<Pair<Int, ZonedDateTime>>): RecyclerView.Adapter<SunInfoAdapter.SunInfoViewHolder>()
{
    inner class SunInfoViewHolder(view: SunInfoViewCell): RecyclerView.ViewHolder(view)
    {
        internal fun bind(sunTime: Pair<Int, ZonedDateTime>)
        {
            itemView.apply()
            {
                infoTitle.text = resources.getString(sunTime.first)
                infoTime.text = preferredTimeString(context, sunTime.second)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SunInfoViewHolder
    {
        val cell = SunInfoViewCell(parent.context).apply()
        {
            layoutParams = ConstraintLayout.LayoutParams(resources.getDimensionPixelSize(R.dimen.cell_sun_info_width), resources.getDimensionPixelSize(R.dimen.cell_sun_info_height))
        }

        return SunInfoViewHolder(cell)
    }

    override fun onBindViewHolder(holder: SunInfoViewHolder, position: Int)
    {
        holder.bind(sunTimes[position])
    }

    override fun getItemCount(): Int = sunTimes.size
}