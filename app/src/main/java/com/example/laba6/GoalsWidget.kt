package com.example.laba6

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class GoalsWidget : AppWidgetProvider() {

    companion object {

        fun updateAppWidget(context: Context, manager: AppWidgetManager, id: Int) {

            val prefs = context.getSharedPreferences("GOALS_LIST", Context.MODE_PRIVATE)
            val donePrefs = context.getSharedPreferences("GOALS_DONE", Context.MODE_PRIVATE)

            val raw = prefs.getString("goals", "") ?: ""
            val goals = raw.split(";;").filter { it.isNotEmpty() }

            val views = RemoteViews(context.packageName, R.layout.goals_widget)

            views.removeAllViews(R.id.widgetGoalsContainer)

            goals.forEachIndexed { index, goal ->

                val item = RemoteViews(context.packageName, R.layout.widget_goal_item)

                item.setTextViewText(R.id.widgetGoalText, goal)

                val checked = donePrefs.getBoolean("d$index", false)
                item.setImageViewResource(
                    R.id.widgetGoalCheck,
                    if (checked) R.drawable.ic_checked else R.drawable.ic_unchecked
                )

                val intent = Intent(context, GoalsReceiver::class.java)
                intent.putExtra("index", index)

                val pending = PendingIntent.getBroadcast(
                    context,
                    index,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                item.setOnClickPendingIntent(R.id.widgetGoalCheck, pending)

                views.addView(R.id.widgetGoalsContainer, item)
            }

            manager.updateAppWidget(id, views)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateAppWidget(context, manager, it) }
    }
}
