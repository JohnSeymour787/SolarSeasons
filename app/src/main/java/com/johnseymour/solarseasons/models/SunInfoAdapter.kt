package com.johnseymour.solarseasons.models

import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.johnseymour.solarseasons.R
import com.johnseymour.solarseasons.current_uv_screen.SunInfoViewCell
import com.johnseymour.solarseasons.preferredTimeString
import com.johnseymour.solarseasons.settings_screen.PreferenceScreenFragment

class SunInfoAdapter(private val sunTimes: List<SunInfo.SunTimeData>, private val textColorInt: Int, private val onClick: (SunInfo.SunTimeData) -> Unit, private val cellPixelWidth: Int): RecyclerView.Adapter<SunInfoAdapter.SunInfoViewHolder>()
{
    inner class SunInfoViewHolder(view: SunInfoViewCell): RecyclerView.ViewHolder(view)
    {
        private val viewBinding = view.binding

        internal fun bind(sunTime: SunInfo.SunTimeData)
        {
            itemView.apply()
            {
                viewBinding.infoTitle.text = resources.getString(sunTime.nameResourceInt)
                viewBinding.infoTime.text = preferredTimeString(context, sunTime.time)
                viewBinding.infoImage.setImageResource(sunTime.imageResourceInt)

                setOnClickListener { onClick(sunTime) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SunInfoViewHolder
    {
        val cell = SunInfoViewCell(parent.context).apply()
        {
            layoutParams = ConstraintLayout.LayoutParams(cellPixelWidth, resources.getDimensionPixelSize(R.dimen.cell_sun_info_height))

            if (PreferenceScreenFragment.useCustomTheme)
            {
                binding.infoTitle.setTextColor(resources.getColor(textColorInt, context.theme))
                binding.infoTime.setTextColor(resources.getColor(textColorInt, context.theme))
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