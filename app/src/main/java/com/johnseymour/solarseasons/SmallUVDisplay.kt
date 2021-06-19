package com.johnseymour.solarseasons

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

/**
 * Implementation of App Widget functionality.
 */
class SmallUVDisplay : AppWidgetProvider()
{
    companion object
    {
        var uvData: UVData? = null
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray)
    {
        //Pending intent to launch the MainActivity when a Widget is selected
        val intent: PendingIntent = Intent(context, MainActivity::class.java).apply { putExtra(UVData.UV_DATA_KEY, uvData) }.let { PendingIntent.getActivity(context, 0, it, 0) }

    //    AlarmManager().set(0, 23000, intent)

        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds)
        {
            updateAppWidget(context, appWidgetManager, appWidgetId, intent)
        }
    }

    override fun onEnabled(context: Context)
    {
        // Enter relevant functionality for when the first widget is created
        val uvDataRequest = OneTimeWorkRequestBuilder<UVDataWorker>().addTag("sadwadsaw").setInitialDelay(10, TimeUnit.SECONDS).build()

        val workManager = WorkManager.getInstance(context)
        workManager.enqueue(uvDataRequest)

      //  context.registerReceiver(this, IntentFilter().apply { addAction(UVData.UV_DATA_CHANGED) })
    }

    override fun onDisabled(context: Context)
    {
        // Enter relevant functionality for when the last widget is disabled
    }

    private fun updateUVData(context: Context, uvData: UVData)
    {
        //TODO() Might need to make sure on main thread here
   //     Handler(Looper.getMainLooper()).post()
 //       {
            val uv: Float = uvData.uv

             WeakReference(RemoteViews(context.packageName, R.layout.small_u_v_display)).get()?.let()
            {
                it.setTextViewText(R.id.uvValue, uv.toString())
                it.setInt(R.id.layout, "setBackgroundColor", context.resources.getColor(uvData.colorInt, context.theme))

                val appWidgetManager = AppWidgetManager.getInstance(context)

                appWidgetManager.updateAppWidget(ComponentName(context, SmallUVDisplay::class.java), it)
            }



   //     }

    }

    override fun onReceive(context: Context?, intent: Intent?)
    {

        val cake = 2

        if (intent?.action == UVData.UV_DATA_CHANGED)
        {
            context ?: return
            val data = 2
            intent.getParcelableExtra<UVData>(UVData.UV_DATA_KEY)?.let()
            {
                //updateUVData(context, it)
                uvData = it

                val uvDataRequest = OneTimeWorkRequestBuilder<UVDataWorker>().addTag("sadwadsaw").setInitialDelay(10, TimeUnit.SECONDS).build()

                val workManager = WorkManager.getInstance(context)
                workManager.enqueue(uvDataRequest)
            }

            //
        }
        else
        {
            super.onReceive(context, intent)
        }
    }



    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, intent: PendingIntent)
    {
        // Construct the RemoteViews object
        val views = RemoteViews(context.packageName, R.layout.small_u_v_display)
        uvData?.let()
        {
            val widgetText = context.getString(R.string.widget_uv_value, it.uv)
            views.setTextViewText(R.id.uvValue, widgetText)
            views.setInt(R.id.layout, "setBackgroundColor", context.resources.getColor(it.colorInt, context.theme))
        } ?: run { views.setTextViewText(R.id.uvValue, "Data unavailable") }

     //   val myconfig = Configuration.Builder().

      //  val constraints = Constraints.Builder().setTriggerContentMaxDelay(1, TimeUnit.MINUTES).
      //  val cake = Handler(Looper.myLooper()!!)
     //   cake.post(UVDataRunnable(context, ::updateUVData))
      //  Thread(UVDataRunnable(context, ::updateUVData)).start()
        //TODO DO this in the onEnabled() method
   //     val uvDataRequest = OneTimeWorkRequestBuilder<UVDataWorker>().addTag("").setInitialDelay(3, TimeUnit.SECONDS).build()

   //     val workManager = WorkManager.getInstance(context)
    //    workManager.enqueue(uvDataRequest)

                // workManager.cancelAllWorkByTag("")

        //TODO() seems to call the onReceive method too often, with UPDATE action



        views.setOnClickPendingIntent(R.id.layout, intent)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

