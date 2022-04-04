package com.johnseymour.solarseasons

import com.johnseymour.solarseasons.models.SunInfo
import com.johnseymour.solarseasons.models.UVData
import org.junit.Assert
import org.junit.Test
import java.time.ZonedDateTime

class UVDataUnitTests
{
    private val testInstance = UVData(
        uv = 0.0399F, uvTime = ZonedDateTime.parse("2021-09-25T00:00:30.826+10:00[Australia/Sydney]"),
        uvMax = 3.0005F, uvMaxTime = ZonedDateTime.parse("2021-09-25T21:53:36.274+10:00[Australia/Sydney]"),
        ozone = 332.5F, ozoneTime = ZonedDateTime.parse("2021-09-25T16:04:07.137+10:00[Australia/Sydney]"),
        safeExposure = mapOf("st1" to 4180, "st2" to 5016, "st3" to 6688, "st4" to 8360, "st5" to 13376, "st6" to 25079),
        sunInfo = SunInfo(
            solarNoon = ZonedDateTime.parse("2021-09-25T21:53:36.274+10:00[Australia/Sydney]"), nadir = ZonedDateTime.parse("2021-09-25T09:53:36.274+10:00[Australia/Sydney]"),
            sunrise = ZonedDateTime.parse("2021-09-25T15:52:48.317+10:00[Australia/Sydney]"),
            sunset = ZonedDateTime.parse("2021-09-26T03:54:24.230+10:00[Australia/Sydney]"),
            sunriseEnd = ZonedDateTime.parse("2021-09-25T15:56:13.870+10:00[Australia/Sydney]"),
            sunsetStart = ZonedDateTime.parse("2021-09-26T03:50:58.677+10:00[Australia/Sydney]"),
            dawn = ZonedDateTime.parse("2021-09-25T15:19:32.279+10:00[Australia/Sydney]"),
            dusk = ZonedDateTime.parse("2021-09-26T04:27:40.269+10:00[Australia/Sydney]"),
            nauticalDawn = ZonedDateTime.parse("2021-09-25T14:40:20.870+10:00[Australia/Sydney]"),
            nauticalDusk = ZonedDateTime.parse("2021-09-26T05:06:51.678+10:00[Australia/Sydney]"),
            nightEnd = ZonedDateTime.parse("2021-09-25T13:59:43.486+10:00[Australia/Sydney]"),
            night = ZonedDateTime.parse("2021-09-26T05:47:29.061+10:00[Australia/Sydney]"),
            goldenHourEnd = ZonedDateTime.parse("2021-09-25T16:36:54.771+10:00[Australia/Sydney]"),
            goldenHour = ZonedDateTime.parse("2021-09-26T03:10:17.776+10:00[Australia/Sydney]"),
            azimuth = -1.48815118586359, altitude = 0.04749226792696052
        )
    )

    @Test
    fun `uv background colour is correct`()
    {
        testInstance.uv = 10.99999F
        Assert.assertEquals(R.color.uv_very_high, testInstance.backgroundColorInt)

        testInstance.uv = 11.000001F
        Assert.assertEquals(R.color.uv_extreme, testInstance.backgroundColorInt)
    }

    @Test
    fun `test UVData sunInSky()`()
    {
        Assert.assertFalse(testInstance.sunInSky())

        val sunInSky = UVData(
            uv = 0.0399F, uvTime = ZonedDateTime.parse("2021-09-25T12:00:30.826+10:00[Australia/Sydney]"),
            uvMax = 3.0005F, uvMaxTime = ZonedDateTime.parse("2021-09-25T12:53:36.274+10:00[Australia/Sydney]"),
            ozone = 332.5F, ozoneTime = ZonedDateTime.parse("2021-09-25T16:04:07.137+10:00[Australia/Sydney]"),
            safeExposure = mapOf("st1" to 4180, "st2" to 5016, "st3" to 6688, "st4" to 8360, "st5" to 13376, "st6" to 25079),
            sunInfo = SunInfo(
                solarNoon = ZonedDateTime.parse("2021-09-25T21:53:36.274+10:00[Australia/Sydney]"), nadir = ZonedDateTime.parse("2021-09-25T09:53:36.274+10:00[Australia/Sydney]"),
                sunrise = ZonedDateTime.parse("2021-09-25T07:52:48.317+10:00[Australia/Sydney]"),
                sunset = ZonedDateTime.parse("2021-09-26T18:54:24.230+10:00[Australia/Sydney]"),
                sunriseEnd = ZonedDateTime.parse("2021-09-25T07:56:13.870+10:00[Australia/Sydney]"),
                sunsetStart = ZonedDateTime.parse("2021-09-26T18:50:58.677+10:00[Australia/Sydney]"),
                dawn = ZonedDateTime.parse("2021-09-25T07:19:32.279+10:00[Australia/Sydney]"),
                dusk = ZonedDateTime.parse("2021-09-26T19:27:40.269+10:00[Australia/Sydney]"),
                nauticalDawn = ZonedDateTime.parse("2021-09-25T06:40:20.870+10:00[Australia/Sydney]"),
                nauticalDusk = ZonedDateTime.parse("2021-09-26T20:06:51.678+10:00[Australia/Sydney]"),
                nightEnd = ZonedDateTime.parse("2021-09-25T04:59:43.486+10:00[Australia/Sydney]"),
                night = ZonedDateTime.parse("2021-09-26T22:47:29.061+10:00[Australia/Sydney]"),
                goldenHourEnd = ZonedDateTime.parse("2021-09-25T20:36:54.771+10:00[Australia/Sydney]"),
                goldenHour = ZonedDateTime.parse("2021-09-26T06:10:17.776+10:00[Australia/Sydney]"),
                azimuth = -1.48815118586359, altitude = 0.04749226792696052
            )
        )

        Assert.assertTrue(sunInSky.sunInSky())

        val sunJustBeforeSunset = UVData(
            uv = 0.0399F, uvTime = ZonedDateTime.parse("2021-09-25T18:30:30.826+10:00[Australia/Sydney]"),
            uvMax = 3.0005F, uvMaxTime = ZonedDateTime.parse("2021-09-25T12:53:36.274+10:00[Australia/Sydney]"),
            ozone = 332.5F, ozoneTime = ZonedDateTime.parse("2021-09-25T16:04:07.137+10:00[Australia/Sydney]"),
            safeExposure = mapOf("st1" to 4180, "st2" to 5016, "st3" to 6688, "st4" to 8360, "st5" to 13376, "st6" to 25079),
            sunInfo = SunInfo(
                solarNoon = ZonedDateTime.parse("2021-09-25T21:53:36.274+10:00[Australia/Sydney]"), nadir = ZonedDateTime.parse("2021-09-25T09:53:36.274+10:00[Australia/Sydney]"),
                sunrise = ZonedDateTime.parse("2021-09-25T07:52:48.317+10:00[Australia/Sydney]"),
                sunset = ZonedDateTime.parse("2021-09-26T18:54:24.230+10:00[Australia/Sydney]"),
                sunriseEnd = ZonedDateTime.parse("2021-09-25T07:56:13.870+10:00[Australia/Sydney]"),
                sunsetStart = ZonedDateTime.parse("2021-09-26T18:50:58.677+10:00[Australia/Sydney]"),
                dawn = ZonedDateTime.parse("2021-09-25T07:19:32.279+10:00[Australia/Sydney]"),
                dusk = ZonedDateTime.parse("2021-09-26T19:27:40.269+10:00[Australia/Sydney]"),
                nauticalDawn = ZonedDateTime.parse("2021-09-25T06:40:20.870+10:00[Australia/Sydney]"),
                nauticalDusk = ZonedDateTime.parse("2021-09-26T20:06:51.678+10:00[Australia/Sydney]"),
                nightEnd = ZonedDateTime.parse("2021-09-25T04:59:43.486+10:00[Australia/Sydney]"),
                night = ZonedDateTime.parse("2021-09-26T22:47:29.061+10:00[Australia/Sydney]"),
                goldenHourEnd = ZonedDateTime.parse("2021-09-25T20:36:54.771+10:00[Australia/Sydney]"),
                goldenHour = ZonedDateTime.parse("2021-09-26T06:10:17.776+10:00[Australia/Sydney]"),
                azimuth = -1.48815118586359, altitude = 0.04749226792696052
            )
        )

        Assert.assertTrue(sunJustBeforeSunset.sunInSky())

        val sunJustAfterSunset = UVData(
            uv = 0.0399F, uvTime = ZonedDateTime.parse("2021-09-25T19:00:30.826+10:00[Australia/Sydney]"),
            uvMax = 3.0005F, uvMaxTime = ZonedDateTime.parse("2021-09-25T12:53:36.274+10:00[Australia/Sydney]"),
            ozone = 332.5F, ozoneTime = ZonedDateTime.parse("2021-09-25T16:04:07.137+10:00[Australia/Sydney]"),
            safeExposure = mapOf("st1" to 4180, "st2" to 5016, "st3" to 6688, "st4" to 8360, "st5" to 13376, "st6" to 25079),
            sunInfo = SunInfo(
                solarNoon = ZonedDateTime.parse("2021-09-25T21:53:36.274+10:00[Australia/Sydney]"), nadir = ZonedDateTime.parse("2021-09-25T09:53:36.274+10:00[Australia/Sydney]"),
                sunrise = ZonedDateTime.parse("2021-09-25T07:52:48.317+10:00[Australia/Sydney]"),
                sunset = ZonedDateTime.parse("2021-09-26T18:54:24.230+10:00[Australia/Sydney]"),
                sunriseEnd = ZonedDateTime.parse("2021-09-25T07:56:13.870+10:00[Australia/Sydney]"),
                sunsetStart = ZonedDateTime.parse("2021-09-26T18:50:58.677+10:00[Australia/Sydney]"),
                dawn = ZonedDateTime.parse("2021-09-25T07:19:32.279+10:00[Australia/Sydney]"),
                dusk = ZonedDateTime.parse("2021-09-26T19:27:40.269+10:00[Australia/Sydney]"),
                nauticalDawn = ZonedDateTime.parse("2021-09-25T06:40:20.870+10:00[Australia/Sydney]"),
                nauticalDusk = ZonedDateTime.parse("2021-09-26T20:06:51.678+10:00[Australia/Sydney]"),
                nightEnd = ZonedDateTime.parse("2021-09-25T04:59:43.486+10:00[Australia/Sydney]"),
                night = ZonedDateTime.parse("2021-09-26T22:47:29.061+10:00[Australia/Sydney]"),
                goldenHourEnd = ZonedDateTime.parse("2021-09-25T20:36:54.771+10:00[Australia/Sydney]"),
                goldenHour = ZonedDateTime.parse("2021-09-26T06:10:17.776+10:00[Australia/Sydney]"),
                azimuth = -1.48815118586359, altitude = 0.04749226792696052
            )
        )

        Assert.assertFalse(sunJustAfterSunset.sunInSky())

        val sunAtNight = UVData(
            uv = 0.0399F, uvTime = ZonedDateTime.parse("2021-09-25T23:00:30.826+10:00[Australia/Sydney]"),
            uvMax = 3.0005F, uvMaxTime = ZonedDateTime.parse("2021-09-25T12:53:36.274+10:00[Australia/Sydney]"),
            ozone = 332.5F, ozoneTime = ZonedDateTime.parse("2021-09-25T16:04:07.137+10:00[Australia/Sydney]"),
            safeExposure = mapOf("st1" to 4180, "st2" to 5016, "st3" to 6688, "st4" to 8360, "st5" to 13376, "st6" to 25079),
            sunInfo = SunInfo(
                solarNoon = ZonedDateTime.parse("2021-09-25T21:53:36.274+10:00[Australia/Sydney]"), nadir = ZonedDateTime.parse("2021-09-25T09:53:36.274+10:00[Australia/Sydney]"),
                sunrise = ZonedDateTime.parse("2021-09-25T07:52:48.317+10:00[Australia/Sydney]"),
                sunset = ZonedDateTime.parse("2021-09-26T18:54:24.230+10:00[Australia/Sydney]"),
                sunriseEnd = ZonedDateTime.parse("2021-09-25T07:56:13.870+10:00[Australia/Sydney]"),
                sunsetStart = ZonedDateTime.parse("2021-09-26T18:50:58.677+10:00[Australia/Sydney]"),
                dawn = ZonedDateTime.parse("2021-09-25T07:19:32.279+10:00[Australia/Sydney]"),
                dusk = ZonedDateTime.parse("2021-09-26T19:27:40.269+10:00[Australia/Sydney]"),
                nauticalDawn = ZonedDateTime.parse("2021-09-25T06:40:20.870+10:00[Australia/Sydney]"),
                nauticalDusk = ZonedDateTime.parse("2021-09-26T20:06:51.678+10:00[Australia/Sydney]"),
                nightEnd = ZonedDateTime.parse("2021-09-25T04:59:43.486+10:00[Australia/Sydney]"),
                night = ZonedDateTime.parse("2021-09-26T22:47:29.061+10:00[Australia/Sydney]"),
                goldenHourEnd = ZonedDateTime.parse("2021-09-25T20:36:54.771+10:00[Australia/Sydney]"),
                goldenHour = ZonedDateTime.parse("2021-09-26T06:10:17.776+10:00[Australia/Sydney]"),
                azimuth = -1.48815118586359, altitude = 0.04749226792696052
            )
        )

        Assert.assertFalse(sunAtNight.sunInSky())
    }
}