package com.example.laba6

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var list: LinearLayout
    private lateinit var addBtn: Button
    private lateinit var saveBtn: Button

    private var updateReceiver: BroadcastReceiver? = null
    private var draggedView: View? = null
    private var startY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Laba6)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        list = findViewById(R.id.goalsList)
        addBtn = findViewById(R.id.addGoalBtn)
        saveBtn = findViewById(R.id.saveBtn)

        loadGoals()

        addBtn.setOnClickListener { addGoalField("") }

        saveBtn.setOnClickListener {
            saveGoals()
            updateWidget()
            Toast.makeText(this, "Збережено", Toast.LENGTH_SHORT).show()
        }

        updateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                list.removeAllViews()
                loadGoals()
            }
        }

        val filter = IntentFilter("GOALS_UPDATED")
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(updateReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateReceiver?.let { unregisterReceiver(it) }
    }

    private fun addGoalField(text: String, checked: Boolean = false) {
        val item = layoutInflater.inflate(R.layout.goal_item, list, false)
        val edit = item.findViewById<EditText>(R.id.goalEdit)
        val del = item.findViewById<ImageView>(R.id.deleteGoal)
        val check = item.findViewById<CheckBox>(R.id.goalCheck)
        val drag = item.findViewById<ImageView>(R.id.dragHandle)

        edit.setText(text)
        check.isChecked = checked

        del.setOnClickListener { list.removeView(item) }

        check.setOnCheckedChangeListener { _, isChecked ->
            val index = list.indexOfChild(item)
            if (index >= 0) {
                saveSingleCheck(index, isChecked)
                updateWidget()
            }
        }

        drag.setOnLongClickListener {
            draggedView = item
            startY = 0f
            item.alpha = 0.7f
            true
        }

        drag.setOnTouchListener { v, event ->
            if (draggedView != item) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    if (startY == 0f) startY = event.rawY
                    val dy = event.rawY - startY

                    val currentIndex = list.indexOfChild(item)
                    val targetIndex =
                        if (dy > item.height / 2) currentIndex + 1
                        else if (dy < -item.height / 2) currentIndex - 1
                        else currentIndex

                    if (targetIndex in 0 until list.childCount && targetIndex != currentIndex) {
                        list.removeView(item)
                        list.addView(item, targetIndex)
                        startY = event.rawY
                    }
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    item.alpha = 1f
                    draggedView = null
                    startY = 0f
                }
            }
            true
        }

        list.addView(item)
    }

    private fun loadGoals() {
        val prefs = getSharedPreferences("GOALS_LIST", Context.MODE_PRIVATE)
        val donePrefs = getSharedPreferences("GOALS_DONE", Context.MODE_PRIVATE)

        val raw = prefs.getString("goals", "") ?: ""
        val goals = raw.split(";;").filter { it.isNotEmpty() }

        goals.forEachIndexed { index, goal ->
            val checked = donePrefs.getBoolean("d$index", false)
            addGoalField(goal, checked)
        }
    }

    private fun saveSingleCheck(index: Int, checked: Boolean) {
        getSharedPreferences("GOALS_DONE", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("d$index", checked)
            .apply()
    }

    private fun saveGoals() {
        val goals = mutableListOf<String>()
        val donePrefs = getSharedPreferences("GOALS_DONE", Context.MODE_PRIVATE).edit()
        donePrefs.clear()

        for (i in 0 until list.childCount) {
            val item = list.getChildAt(i)
            val edit = item.findViewById<EditText>(R.id.goalEdit)
            val check = item.findViewById<CheckBox>(R.id.goalCheck)

            val text = edit.text.toString().trim()
            if (text.isNotEmpty()) {
                goals.add(text)
                donePrefs.putBoolean("d${goals.size - 1}", check.isChecked)
            }
        }

        donePrefs.apply()

        getSharedPreferences("GOALS_LIST", Context.MODE_PRIVATE)
            .edit()
            .putString("goals", goals.joinToString(";;"))
            .apply()
    }

    private fun updateWidget() {
        val manager = AppWidgetManager.getInstance(this)
        val widget = ComponentName(this, GoalsWidget::class.java)
        val ids = manager.getAppWidgetIds(widget)
        ids.forEach { GoalsWidget.updateAppWidget(this, manager, it) }
    }
}
