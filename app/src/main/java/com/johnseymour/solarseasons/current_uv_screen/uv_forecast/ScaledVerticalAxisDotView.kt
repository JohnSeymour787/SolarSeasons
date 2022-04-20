package com.johnseymour.solarseasons.current_uv_screen.uv_forecast

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import com.johnseymour.solarseasons.Constants
import com.johnseymour.solarseasons.R
import kotlin.math.ceil

/**
 * View with a single circle (dot) centered horizontally with a vertical position set depending on the
 *  @yValue property set, within the range of @maxYValue and @minYValue. @maxYValue positions the dot
 *  at the top of the view. Text immediately above the dot can also be set
 */
class ScaledVerticalAxisDotView(context: Context, attrs: AttributeSet? = null) : View(context, attrs)
{
    private val textBottomMargin: Float = 10F

    private val dotPaint = Paint().apply { isAntiAlias = true }

    var maxYValue = Constants.GENERAL_MAXIMUM_UV
    var minYValue = 0F
    var yValue = 0F

    @ColorInt
    var dotColour = resources.getColor(R.color.black, context.theme)
        set(value)
        {
            dotPaint.color = value
            field = value
        }

    var dotRadius = resources.getDimension(R.dimen.uv_forecast_uv_dot_radius)

    @ColorInt
    var lineColour = dotColour
        set(value)
        {
            linePaint.color = value
            field = value
        }

    var lineThickness = resources.getDimension(R.dimen.uv_forecast_uv_line_thickness)
        set(value)
        {
            linePaint.strokeWidth = value
            field = value
        }

    private val linePaint = Paint().apply()
    {
        strokeWidth = lineThickness
        isAntiAlias = true
    }

    private var textStyle: Int = Typeface.NORMAL
        set(value)
        {
            textPaint.typeface = Typeface.create(textPaint.typeface, value)
            field = value
        }

    private var textFont: Typeface = Typeface.DEFAULT
        set(value)
        {
            textPaint.typeface = Typeface.create(value, textPaint.typeface.style)
            field = value
        }

    private var textSize: Float = resources.getDimension(R.dimen.text_default)
        set(value)
        {
            textPaint.textSize = value
            field = value
        }

    @ColorInt
    var textColour = dotColour
        set(value)
        {
            textPaint.color = value
            field = value
        }

    private val textPaint = Paint().apply()
    {
        isAntiAlias = true
        textSize = this@ScaledVerticalAxisDotView.textSize
        typeface = this@ScaledVerticalAxisDotView.textFont
        textAlign = Paint.Align.CENTER
    }

    var text: String = ""
    var previousDotYValue: Float? = null
    var nextDotYValue: Float? = null

    init
    {
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.ScaledVerticalAxisDotView, 0, 0
        )

        if (a.hasValue(R.styleable.ScaledVerticalAxisDotView_maxYValue))
        {
            maxYValue = a.getFloat(R.styleable.ScaledVerticalAxisDotView_maxYValue, maxYValue)
        }

        if (a.hasValue(R.styleable.ScaledVerticalAxisDotView_minYValue))
        {
            minYValue = a.getFloat(R.styleable.ScaledVerticalAxisDotView_minYValue, minYValue)
        }

        if (a.hasValue(R.styleable.ScaledVerticalAxisDotView_yValue))
        {
            yValue = a.getFloat(R.styleable.ScaledVerticalAxisDotView_yValue, yValue)
        }

        if (a.hasValue(R.styleable.ScaledVerticalAxisDotView_dotColour))
        {
            dotColour = a.getColor(R.styleable.ScaledVerticalAxisDotView_dotColour, dotColour)
        }

        if (a.hasValue(R.styleable.ScaledVerticalAxisDotView_dotRadius))
        {
            dotRadius = a.getDimension(R.styleable.ScaledVerticalAxisDotView_dotRadius, dotRadius)
        }

        if (a.hasValue(R.styleable.ScaledVerticalAxisDotView_lineColour))
        {
            lineColour = a.getColor(R.styleable.ScaledVerticalAxisDotView_lineColour, lineColour)
        }

        if (a.hasValue(R.styleable.ScaledVerticalAxisDotView_lineThickness))
        {
            lineThickness = a.getDimension(R.styleable.ScaledVerticalAxisDotView_lineThickness, lineThickness)
        }

        if (a.hasValue(R.styleable.ScaledVerticalAxisDotView_android_fontFamily))
        {
            textFont = a.getFont(R.styleable.ScaledVerticalAxisDotView_android_fontFamily) ?: textFont
        }

        if (a.hasValue(R.styleable.ScaledVerticalAxisDotView_android_textStyle))
        {
            textStyle = a.getInt(R.styleable.ScaledVerticalAxisDotView_android_textStyle, textStyle)
        }

        if (a.hasValue(R.styleable.ScaledVerticalAxisDotView_textColour))
        {
            textColour = a.getColor(R.styleable.ScaledVerticalAxisDotView_textColour, textColour)
        }

        if (a.hasValue(R.styleable.ScaledVerticalAxisDotView_text))
        {
            text = a.getString(R.styleable.ScaledVerticalAxisDotView_text) ?: text
        }

        if (a.hasValue(R.styleable.ScaledVerticalAxisDotView_android_textSize))
        {
            textSize = a.getDimension(R.styleable.ScaledVerticalAxisDotView_android_textSize, textSize)
        }

        a.recycle()
    }

    private var contentWidth: Int = 0
    private var contentHeight: Int = 0
    private var gradient: Float = 0F

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int)
    {
        super.onSizeChanged(w, h, oldw, oldh)

        if (w == 0 || h == 0) { return }

        contentWidth = width - paddingLeft - paddingRight
        contentHeight = height - paddingTop - paddingBottom - ceil(dotRadius).toInt()
        // Gradient of equation to work out dot height based on yValue
        gradient = (-contentHeight + ceil(dotRadius).toInt()) / (maxYValue - minYValue)
    }

    override fun onDraw(canvas: Canvas)
    {
        super.onDraw(canvas)

        val dotHeight = (gradient * yValue) + contentHeight
        val contentCenter = contentWidth / 2F

        // If adjacent dots with known values, draw a line to them
        previousDotYValue?.let()
        {
            val prevCircleHeight = (gradient * it) + contentHeight
            canvas.drawLine(contentCenter, dotHeight, 0 - (contentCenter), prevCircleHeight, linePaint)
        }

        nextDotYValue?.let()
        {
            val nextCircleHeight = (gradient * it) + contentHeight
            canvas.drawLine(contentCenter, dotHeight, width.toFloat() + (contentCenter), nextCircleHeight, linePaint)
        }

        if (text.isNotEmpty())
        {
            canvas.drawText(text, contentCenter, dotHeight - dotRadius - textBottomMargin, textPaint)
        }

        canvas.drawCircle(contentCenter, dotHeight, dotRadius, dotPaint)
    }
}