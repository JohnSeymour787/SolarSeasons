package com.johnseymour.solarseasons

import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel()
{
    var locationInitialised = false
    lateinit var uvData: UVData
}