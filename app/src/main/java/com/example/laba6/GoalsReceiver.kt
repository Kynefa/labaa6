package com.example.laba6

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class GoalsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val index = intent.getIntExtra("index", -1)
        if (index == -1) return

        val prefs = context.getSharedPreferences("GOALS_DONE", Context.MODE_PRIVATE)
        val current = prefs.getBoolean("d$index", false)
        prefs.edit().putBoolean("d$index", !current).apply()

        val manager = AppWidgetManager.getInstance(context)
        val widget = ComponentName(context, GoalsWidget::class.java)
        val ids = manager.getAppWidgetIds(widget)
        ids.forEach { GoalsWidget.updateAppWidget(context, manager, it) }

        context.sendBroadcast(Intent("GOALS_UPDATED"))
    }
}
