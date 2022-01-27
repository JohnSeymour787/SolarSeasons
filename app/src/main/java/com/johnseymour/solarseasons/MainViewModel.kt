package com.johnseymour.solarseasons

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.work.WorkInfo

class MainViewModel: ViewModel()
{
    var lastObserving: LiveData<List<WorkInfo>>? = null
    var uvData: UVData? = null
    var latestError: ErrorStatus? = null

    val uvDataBackgroundBroadcastReceiver = object : BroadcastReceiver()
    {
        override fun onReceive(context: Context, intent: Intent)
        {
            if (intent.action == UVData.UV_DATA_UPDATED)
            {
                intent.getParcelableExtra<UVData>(UVData.UV_DATA_KEY)?.let()
                {
                    uvData = it
                }
            }
        }
    }

    val uvDataChangedIntentFilter = IntentFilter(UVData.UV_DATA_UPDATED)
}