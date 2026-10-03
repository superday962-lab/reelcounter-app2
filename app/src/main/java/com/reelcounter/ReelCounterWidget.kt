package com.reelcounter

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class ReelCounterWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(ReelAccessibilityService.PREFS, Context.MODE_PRIVATE)
        val count = prefs.getInt(ReelAccessibilityService.KEY_COUNT, 0)

        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_reel_counter)
            views.setTextViewText(R.id.widget_count, "🎬 $count")

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
