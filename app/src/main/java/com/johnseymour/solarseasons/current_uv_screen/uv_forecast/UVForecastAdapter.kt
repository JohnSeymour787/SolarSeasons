package com.johnseymour.solarseasons.current_uv_screen.uv_forecast

import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.johnseymour.solarseasons.R
import com.johnseymour.solarseasons.models.UVData
import com.johnseymour.solarseasons.models.UVForecastData
import com.johnseymour.solarseasons.preferredTimeString
import com.johnseymour.solarseasons.resolveColourAttr
import com.johnseymour.solarseasons.settings_screen.PreferenceScreenFragment
import kotlinx.android.synthetic.main.list_cell_uv_forecast.view.*

class UVForecastAdapter(private val forecastTimes: List<UVForecastData>, private val textColorInt: Int, private val cellPixelWidth: Int): RecyclerView.Adapter<UVForecastAdapter.UVForecastViewHolder>()
{
    inner class UVForecastViewHolder(view: UVForecastViewCell): RecyclerView.ViewHolder(view)
    {
        internal fun bind(forecastData: UVForecastData, previousUV: Float, nextUV: Float)
        {
            itemView.apply()
            {
                forecastDotView.let()
                {
                    it.yValue = forecastData.uv
                    it.previousDotYValue = previousUV
                    it.nextDotYValue = nextUV

                    it.text = resources.getString(R.string.uv_value, forecastData.uv)

                    it.lineColour = resources.getColor(UVData.uvColourInt(forecastData.uv), context.theme)

                    when
                    {
                        forecastData.isTimeNow ->
                        {
                            it.drawVerticalMarkerLine = true
                            it.useDashedVerticalMarkerLine = true
                            it.verticalMarkerLineColour = if (PreferenceScreenFragment.useCustomTheme) { resources.getColor(textColorInt, context.theme) } else { context.resolveColourAttr(android.R.attr.textColorPrimary) }
                        }

                        forecastData.isProtectionTimeBoundary ->
                        {
                            it.drawVerticalMarkerLine = true
                            it.useDashedVerticalMarkerLine = false
                            it.verticalMarkerLineColour = resources.getColor(UVData.uvColourInt(forecastData.uv), context.theme)
                        }

                        else ->
                        {
                            it.drawVerticalMarkerLine = false
                        }
                    }
                }

                if (forecastData.isTimeNow)
                {
                    forecastTime.setText(R.string.uv_forecast_now_time_label)
                }
                else
                {
                    forecastTime.text = preferredTimeString(context, forecastData.time)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UVForecastViewHolder
    {
        val cell = UVForecastViewCell(parent.context).apply()
        {
            layoutParams = ConstraintLayout.LayoutParams(cellPixelWidth, resources.getDimensionPixelSize(R.dimen.uv_forecast_cell_height))

            if (PreferenceScreenFragment.useCustomTheme)
            {
                val textColor = resources.getColor(textColorInt, context.theme)
                forecastDotView.textColour = textColor
                forecastDotView.dotColour = textColor
                forecastDotView.lineColour = textColor
                forecastTime.setTextColor(textColor)
            }

            val maxUV = forecastTimes.maxOfOrNull(UVForecastData::uv) ?: 0F

            if (maxUV > forecastDotView.maxYValue)
            {
                forecastDotView.maxYValue = maxUV
            }
        }

        return UVForecastViewHolder(cell)
    }

    override fun onBindViewHolder(holder: UVForecastViewHolder, position: Int)
    {
        val previousUV = forecastTimes.getOrNull(position - 1)?.uv ?: 0F
        val nextUV = forecastTimes.getOrNull(position + 1)?.uv ?: 0F

        holder.bind(forecastTimes[position], previousUV, nextUV)
    }

    override fun getItemCount(): Int = forecastTimes.size
}