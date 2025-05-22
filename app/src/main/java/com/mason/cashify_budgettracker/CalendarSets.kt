package com.mason.cashify_budgettracker

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.icu.text.CaseMap.Title
import android.os.Build
import android.os.Bundle
import android.provider.Telephony.Mms.Intents
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.listeners.OnDayClickListener
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.mason.cashify_budgettracker.databinding.ActivityCalendarSetsBinding
import com.mason.cashify_budgettracker.databinding.ActivityGoalsBinding
import java.util.Calendar
import java.util.Date

class CalendarSets : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarSetsBinding

    private lateinit var calendarView: CalendarView
    private lateinit var titleInput: TextInputEditText
    private lateinit var amountInput: TextInputEditText
    private lateinit var categoryInput: TextInputEditText
    private lateinit var notesInput: TextInputEditText
    private lateinit var dateRangeText: TextView
    private lateinit var btnSave: MaterialButton

    private lateinit var DeadStore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calendar_sets)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        DeadStore = FirebaseFirestore.getInstance()

        binding = ActivityCalendarSetsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        calendarView = findViewById(R.id.calendarView)
        titleInput = findViewById(R.id.titleInput)
        amountInput = findViewById(R.id. amountInput)
        categoryInput = findViewById(R.id.categoryInput)
        notesInput = findViewById(R.id.notesInput)
        dateRangeText = findViewById(R.id.dateRangeText)
        btnSave = findViewById(R.id.btnSaveDeadline)

        createNotificationChannel()

        val timeInput: TextInputEditText = findViewById(R.id.timeInput)

        timeInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                timeInput.setText(formattedTime)

                // Save time and schedule notification
                val selectedTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, selectedHour)
                    set(Calendar.MINUTE, selectedMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                //scheduleNotification(selectedTime.timeInMillis)

            }, hour, minute, true).show()
        }

        var startDate: Calendar? = null
        var endDate: Calendar? = null

        calendarView.setOnDayClickListener(object : OnDayClickListener {
            override fun onDayClick(eventDay: EventDay) {
                val clickedDate = eventDay.calendar

                if (startDate == null || endDate != null) {
                    // Start a new range
                    startDate = clickedDate
                    endDate = null

                    calendarView.setDate(clickedDate) // Optional: highlight start
                    dateRangeText.text = "Start Date: ${clickedDate.time}"
                } else {
                    // Finish the range
                    if (clickedDate.before(startDate)) {
                        endDate = startDate
                        startDate = clickedDate
                    } else {
                        endDate = clickedDate
                    }

                    val range = getDatesBetween(startDate!!, endDate!!).map {
                        EventDay(it, R.drawable.circle_range) // Use your custom drawable
                    }

                    calendarView.setEvents(range)
                    dateRangeText.text = "Selected Range: ${startDate!!.time} - ${endDate!!.time}"
                }
            }
        })

        btnSave.setOnClickListener{
            saveDeadline()
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Log.d("CalendarSets", "Navigating to MainActivity")
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_categories -> {
                    Log.d("CalendarSets", "Navigating to CategoriesActivity")
                    startActivity(Intent(this, CategoriesActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_goals -> {
                    Log.d("CalendarSets", "Navigating to GoalsActivity")
                    startActivity(Intent(this, GoalsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_calendar -> {
                    Log.d("CalendarSets", "Calendar tab Selected")
                    true
                }
                else -> false
            }
        }
        binding.bottomNav.menu.findItem(R.id.nav_calendar)?.isChecked = true
    }

    private fun scheduleNotification(triggerTime: Long) {
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("title", "Budget Deadline Reminder")
            putExtra("message", "You have a budget deadline today!")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)

        Toast.makeText(this, "Reminder set!", Toast.LENGTH_SHORT).show()
    }

    private fun getDatesBetween(startDate: Calendar, endDate: Calendar): List<Calendar> {
        val dates = mutableListOf<Calendar>()
        val current = startDate.clone() as Calendar

        while (!current.after(endDate)) {
            dates.add(current.clone() as Calendar)
            current.add(Calendar.DATE, 1)
        }

        return dates
    }



    private fun saveDeadline(){
        val title = titleInput.text.toString().trim()
        val amount = amountInput.text.toString().trim()
        val category = categoryInput.text.toString().trim()
        val notes = notesInput.text.toString().trim()
        val selectedDates = calendarView.selectedDates

        if(title.isEmpty() || amount.isEmpty() || category.isEmpty() || selectedDates.isEmpty()){
            Toast.makeText(this, "Please fill tin all the important Fields", Toast.LENGTH_SHORT).show()
            return
        }

        val startDate = selectedDates.first().time
        val endDate = selectedDates.last().time

        val deadline = hashMapOf(
            "title" to title,
            "amount" to amount,
            "category" to category,
            "notes" to notes,
            "startDate" to startDate,
            "endDate" to endDate
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this, "Exact alarms are disabled for this app. Please enable them in system settings.", Toast.LENGTH_LONG).show()
                // You can optionally redirect the user to settings:
                // val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                // startActivity(intent)
                return
            }
        }

        DeadStore.collection("deadlines")
            .add(deadline)
            .addOnSuccessListener {
                Toast.makeText(this, "Deadline Saved!", Toast.LENGTH_SHORT).show()
                val timeParts = binding.timeInput.text.toString().split(":")
                if (timeParts.size == 2) {
                    val hour = timeParts[0].toInt()
                    val minute = timeParts[1].toInt()

                    val notifyCalendar = Calendar.getInstance().apply {
                        time = endDate // 🔔 Set to end of the deadline range
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    scheduleNotification(title, notifyCalendar.time)
                }

                clearFields()
            }
            . addOnFailureListener{ exception ->
                Toast.makeText(this, "Error saving deadline", Toast.LENGTH_SHORT).show()
                exception.printStackTrace()
            }
    }

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    private fun  scheduleNotification(title: String, date: Date){
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("title", "Budget Reminder")
            putExtra("message", "Deadline: $title is starting today.")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            date.time.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            date.time,
            pendingIntent
        )
    }

    private fun createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(
                "budget_reminders",
                "Budget Reminder",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun clearFields() {
        titleInput.setText("")
        amountInput.setText("")
        categoryInput.setText("")
        notesInput.setText("")

        // Clear selected dates using reflection (Applandeo workaround)
        try {
            val field = CalendarView::class.java.getDeclaredField("mCalendarProperties")
            field.isAccessible = true
            val properties = field.get(calendarView)

            val selectedDaysField = properties.javaClass.getDeclaredField("mSelectedDays")
            selectedDaysField.isAccessible = true
            val selectedDays = selectedDaysField.get(properties) as MutableList<*>
            selectedDays.clear()
            calendarView.invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        dateRangeText.text = "Selected Range: Not Set"
    }
}