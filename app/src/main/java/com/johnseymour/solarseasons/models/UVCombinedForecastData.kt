package com.johnseymour.solarseasons.models

data class UVCombinedForecastData(val uvData: UVData, val forecast: List<UVForecastData>?, val protectionTime: UVProtectionTimeData?)