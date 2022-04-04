package com.johnseymour.solarseasons.models

import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.johnseymour.solarseasons.R
import com.johnseymour.solarseasons.current_uv_screen.SunInfoViewCell
import com.johnseymour.solarseasons.preferredTimeString
import com.johnseymour.solarseasons.settings_screen.PreferenceScreenFragment
import kotlinx.android.synthetic.main.list_cell_sun_info.view.*

class SunInfoAdapter(private val sunTimes: List<SunInfo.SunTimeData>, private val textColorInt: Int, private val onClick: (SunInfo.SunTimeData) -> Unit): RecyclerView.Adapter<SunInfoAdapter.SunInfoViewHolder>()
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

                setOnClickListener { onClick(sunTime) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SunInfoViewHolder
    {
        val cell = SunInfoViewCell(parent.context).apply()
        {
            layoutParams = ConstraintLayout.LayoutParams(resources.getDimensionPixelSize(R.dimen.cell_sun_info_width), resources.getDimensionPixelSize(R.dimen.cell_sun_info_height))

            if (PreferenceScreenFragment.useCustomTheme)
            {
                infoTitle.setTextColor(resources.getColor(textColorInt, context.theme))
                infoTime.setTextColor(resources.getColor(textColorInt, context.theme))
            }
        }

        return SunInfoViewHolder(cell)
    }

    override fun onBindViewHolder(holder: SunInfoViewHolder, position: Int)
    {
        holder.bind(sunTimes[position])
    }

    override fun getItemCount(): Int = sunTimes.size
}