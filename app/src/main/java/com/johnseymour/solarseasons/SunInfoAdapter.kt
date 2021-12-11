package com.johnseymour.solarseasons

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.ZonedDateTime

class SunInfoAdapter(private val sunTimes: List<Pair<String, ZonedDateTime>>): RecyclerView.Adapter<SunInfoAdapter.SunInfoViewHolder>()
{
    inner class SunInfoViewHolder(val view: TextView): RecyclerView.ViewHolder(view)
    {
        internal fun bind(sunTime: Pair<String, ZonedDateTime>)
        {
            view.text = sunTime.first + " " + preferredTimeString(itemView.context, sunTime.second)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SunInfoViewHolder
    {
        val cell = TextView(parent.context)

        return SunInfoViewHolder(cell)
    }

    override fun onBindViewHolder(holder: SunInfoViewHolder, position: Int)
    {
        holder.bind(sunTimes[position])
    }

    override fun getItemCount(): Int = sunTimes.size
}