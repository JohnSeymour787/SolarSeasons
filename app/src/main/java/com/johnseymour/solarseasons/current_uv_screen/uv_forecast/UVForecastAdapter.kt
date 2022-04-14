package com.johnseymour.solarseasons.current_uv_screen.uv_forecast

import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.johnseymour.solarseasons.R
import com.johnseymour.solarseasons.models.UVForecastData
import com.johnseymour.solarseasons.preferredTimeString
import kotlinx.android.synthetic.main.list_cell_uv_forecast.view.*

class UVForecastAdapter(private val forecastTimes: List<UVForecastData>?= null, val cake: List<Float>, private val textColorInt: Int): RecyclerView.Adapter<UVForecastAdapter.UVForecastViewHolder>()
{
    inner class UVForecastViewHolder(view: UVForecastViewCell): RecyclerView.ViewHolder(view)
    {
        internal fun bind(forecastData: UVForecastData, previousUV: Float?, nextUV: Float?)
        {
            itemView.apply()
            {
                forecastUV.text = resources.getString(R.string.uv_value, forecastData.uv)

                forecastDotView.let()
                {
                    it.yValue = forecastData.uv
                    it.previousDotYValue = previousUV ?: 0F
                    it.nextDotYValue = nextUV ?: 0F
                }

                forecastTime.text = preferredTimeString(context, forecastData.time)
            }
        }

        internal fun bind2(uvForecast: Float, previousUV: Float?, nextUV: Float?)
        {
            itemView.apply()
            {
                forecastUV.text = resources.getString(R.string.uv_value, uvForecast)
                forecastDotView.let()
                {
                    it.yValue = uvForecast
                    it.previousDotYValue = previousUV ?: 0F
                    it.nextDotYValue = nextUV ?: 0F
                }
                forecastTime.text = "Time"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UVForecastViewHolder
    {
        val cell = UVForecastViewCell(parent.context).apply()
        {
            layoutParams = ConstraintLayout.LayoutParams(resources.getDimensionPixelSize(R.dimen.uv_forecast_cell_width), resources.getDimensionPixelSize(R.dimen.uv_forecast_cell_height))

//            if (PreferenceScreenFragment.useCustomTheme)
//            {
//                forecastUV.setTextColor(resources.getColor(textColorInt, context.theme))
//                forecastTime.setTextColor(resources.getColor(textColorInt, context.theme))
                //TODO() might want to set the colour of the dots as well, but only if they are not the same as the background colour
//            }

            cake.maxOrNull()?.let { forecastDotView.maxYValue = it }
        }

        return UVForecastViewHolder(cell)
    }

    override fun onBindViewHolder(holder: UVForecastViewHolder, position: Int)
    {
//        val previousUV = forecastTimes.getOrNull(position - 1)?.forecastUV
//        val nextUV = forecastTimes.getOrNull(position + 1)?.forecastUV
//
//        holder.bind(forecastTimes[position], previousUV, nextUV)

        val previousUV = cake.getOrNull(position - 1)
        val nextUV = cake.getOrNull(position + 1)

        holder.bind2(cake[position], previousUV, nextUV)
    }

    override fun getItemCount(): Int = cake.size
}