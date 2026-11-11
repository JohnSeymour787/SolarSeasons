package com.johnseymour.solarseasons.ui.currentuv

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.johnseymour.solarseasons.R
import com.johnseymour.solarseasons.exposureDurationString
import com.johnseymour.solarseasons.models.SunInfo
import com.johnseymour.solarseasons.models.UVData
import com.johnseymour.solarseasons.models.UVForecastData
import com.johnseymour.solarseasons.preferredTimeString
import com.johnseymour.solarseasons.ui.theme.LocalSolarColors
import com.johnseymour.solarseasons.ui.theme.Quicksand
import kotlin.math.ceil
import kotlin.math.min

@Composable
fun SectionContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
)
{
    val colors = LocalSolarColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(colors.sectionBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier)
{
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 8.dp),
        color = LocalSolarColors.current.primaryText,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
fun SectionSubLabel(text: String, modifier: Modifier = Modifier)
{
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 8.dp),
        color = LocalSolarColors.current.primaryText,
        fontSize = 12.sp,
    )
}

/**
 * Compose port of ScaledVerticalAxisDotView - a single dot positioned according to its UV value,
 * connected to the previous/next dots, with the value drawn above it.
 */
@Composable
fun ScaledVerticalAxisDotGraph(
    text: String,
    yValue: Float,
    previousDotYValue: Float?,
    nextDotYValue: Float?,
    maxYValue: Float,
    dotColor: Color,
    lineColor: Color,
    textColor: Color,
    markerColor: Color?,
    useDashedMarker: Boolean,
    modifier: Modifier = Modifier,
)
{
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val dotRadius = with(density) { 5.dp.toPx() }
    val lineThickness = with(density) { 3.dp.toPx() }

    val textStyle = TextStyle(
        fontFamily = Quicksand,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
    )

    val annotatedText = AnnotatedString(text)
    val measured = textMeasurer.measure(annotatedText, textStyle)

    Canvas(modifier = modifier)
    {
        val contentWidth = size.width
        val contentHeight = size.height - ceil(dotRadius).toInt()

        val gradient = -(contentHeight - ceil(dotRadius).toInt() - measured.size.height - 15F) /
                (maxYValue - 0F)

        val dotHeight = (gradient * yValue) + contentHeight
        val contentCenter = contentWidth / 2F

        // Draws a line between two dot centres, stopping at the edge of each dot so that the line
        // is never painted over a dot
        fun drawConnectedDotLine(from: Offset, to: Offset)
        {
            val delta = to - from
            val distance = delta.getDistance()

            if (distance == 0F) { return }

            val trim = min(dotRadius, distance / 2F)
            val direction = delta / distance

            drawLine(
                color = lineColor,
                start = from + (direction * trim),
                end = to - (direction * trim),
                strokeWidth = lineThickness,
            )
        }

        previousDotYValue?.let()
        {
            val previousHeight = (gradient * it) + contentHeight
            drawConnectedDotLine(
                from = Offset(contentCenter, dotHeight),
                to = Offset(-contentCenter, previousHeight),
            )
        }

        nextDotYValue?.let()
        {
            val nextHeight = (gradient * it) + contentHeight
            drawConnectedDotLine(
                from = Offset(contentCenter, dotHeight),
                to = Offset(size.width + contentCenter, nextHeight),
            )
        }

        if (text.isNotEmpty())
        {
            drawText(
                textMeasurer = textMeasurer,
                text = annotatedText,
                topLeft = Offset(
                    contentCenter - (measured.size.width / 2F),
                    dotHeight - dotRadius - 15F - measured.size.height
                ),
                style = textStyle.copy(color = textColor),
            )
        }

        markerColor?.let()
        {
            val pathEffect = if (useDashedMarker && yValue > 2F)
            {
                PathEffect.dashPathEffect(floatArrayOf(10F, 12F), 0F)
            }
            else
            {
                null
            }

            drawLine(
                color = it,
                start = Offset(contentCenter, size.height),
                end = Offset(contentCenter, dotHeight),
                strokeWidth = lineThickness,
                pathEffect = pathEffect,
            )
        }

        drawCircle(
            color = dotColor,
            radius = dotRadius,
            center = Offset(contentCenter, dotHeight),
        )
    }
}

@Composable
fun UvForecastCell(
    forecast: UVForecastData,
    previousUv: Float,
    nextUv: Float,
    maxYValue: Float,
    cellWidth: Dp,
    textColor: Color,
)
{
    val context = LocalContext.current
    val uvColor = colorResource(UVData.uvColourInt(forecast.uv))

    val markerColor = when
    {
        forecast.isTimeNow -> textColor
        forecast.isProtectionTimeBoundary -> uvColor
        else -> null
    }

    Column(
        modifier = Modifier
            .width(cellWidth)
            .height(120.dp)
    )
    {
        ScaledVerticalAxisDotGraph(
            text = stringResource(R.string.uv_value, forecast.uv),
            yValue = forecast.uv,
            previousDotYValue = previousUv,
            nextDotYValue = nextUv,
            maxYValue = maxYValue,
            dotColor = textColor,
            lineColor = uvColor,
            textColor = textColor,
            markerColor = markerColor,
            useDashedMarker = forecast.isTimeNow,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F),
        )

        Text(
            text = if (forecast.isTimeNow)
            {
                stringResource(R.string.uv_forecast_now_time_label)
            }
            else
            {
                preferredTimeString(context, forecast.time)
            },
            modifier = Modifier.fillMaxWidth(),
            color = textColor,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
fun SunInfoCell(
    sunTime: SunInfo.SunTimeData,
    cellWidth: Dp,
    titleColor: Color,
    timeColor: Color,
    onClick: () -> Unit,
)
{
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .width(cellWidth)
            .height(110.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    )
    {
        Text(
            text = stringResource(sunTime.nameResourceInt),
            color = titleColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )

        Text(
            text = preferredTimeString(context, sunTime.time),
            color = timeColor,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )

        Spacer(modifier = Modifier.weight(1F))

        Image(
            painter = painterResource(sunTime.imageResourceInt),
            contentDescription = stringResource(R.string.sun_info_cell_image_description),
            modifier = Modifier.size(35.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
fun SkinExposureCell(
    exposureEntry: Map.Entry<String, Int>,
    titleColor: Color,
    secondaryColor: Color,
)
{
    val resources = androidx.compose.ui.platform.LocalResources.current
    val skinColor = colorResource(UVData.skinTypeColorInt(exposureEntry.key))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    )
    {
        Box(
            modifier = Modifier
                .size(25.dp)
                .background(skinColor, CircleShape)
        )

        Text(
            text = stringResource(UVData.skinTypeNameInt(exposureEntry.key)),
            modifier = Modifier
                .weight(1F)
                .padding(start = 6.dp),
            color = titleColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )

        Text(
            text = exposureDurationString(resources, exposureEntry.value),
            modifier = Modifier.weight(1F),
            color = secondaryColor,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
