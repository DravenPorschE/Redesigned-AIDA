package com.hexacore.aidaapplications

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.util.*
import java.text.SimpleDateFormat

class AlarmAppScreen : Fragment() {

    private lateinit var alarmRecycler: RecyclerView
    private lateinit var addAlarmButton: Button
    private lateinit var alarmAdapter: AlarmAdapter
    private lateinit var headerCountdown: TextView
    private lateinit var headerDate: TextView
    private var alarms = mutableListOf<AlarmItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        val view = inflater.inflate(R.layout.alarm_app_screen, container, false)

        // Load saved alarms
        alarms = AlarmStorage.loadAlarms(requireContext())

        headerCountdown = view.findViewById(R.id.alarm_header_countdown)
        headerDate = view.findViewById(R.id.alarm_header_date)
        alarmRecycler = view.findViewById(R.id.alarm_recycler)
        addAlarmButton = view.findViewById(R.id.add_alarm_button)

        alarmAdapter = AlarmAdapter(
            alarms,
            onSwitchToggle = { alarm, isChecked ->
                alarm.isEnabled = isChecked
                if (isChecked) scheduleAlarm(alarm) else cancelAlarm(alarm)
                saveAlarms()
                updateHeader()
            },
            onItemLongClick = { position -> showOptionsDialog(position) }
        )
        val toolbar = view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        alarmRecycler.layoutManager = LinearLayoutManager(requireContext())
        alarmRecycler.adapter = alarmAdapter

        addAlarmButton.setOnClickListener { showAddOrEditDialog(null) }

        updateHeader() // initial update

        return view
    }

    private fun showAddOrEditDialog(editIndex: Int?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.add_alarm_dialog, null)
        val timeButton = dialogView.findViewById<Button>(R.id.btnPickTime)
        val chipGroup = dialogView.findViewById<ChipGroup>(R.id.dayChipGroup)
        val saveBtn = dialogView.findViewById<Button>(R.id.btnSaveAlarm)

        var pickedTime = ""
        if (editIndex != null) {
            val alarm = alarms[editIndex]
            pickedTime = alarm.time
            timeButton.text = pickedTime
            for (i in 0 until chipGroup.childCount) {
                val chip = chipGroup.getChildAt(i) as Chip
                chip.isChecked = alarm.repeatDays.contains(chip.text.toString())
            }
        }

        timeButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
                val amPm = if (selectedHour >= 12) "PM" else "AM"
                val displayHour = if (selectedHour % 12 == 0) 12 else selectedHour % 12
                pickedTime = String.format(Locale.getDefault(), "%d:%02d %s", displayHour, selectedMinute, amPm)
                timeButton.text = pickedTime
            }, hour, minute, false).show()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        saveBtn.setOnClickListener {
            val selectedDays = mutableListOf<String>()
            for (i in 0 until chipGroup.childCount) {
                val chip = chipGroup.getChildAt(i) as Chip
                if (chip.isChecked) selectedDays.add(chip.text.toString())
            }

            if (pickedTime.isEmpty()) {
                Toast.makeText(requireContext(), "Please pick a time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedDays.isEmpty()) {
                Toast.makeText(requireContext(), "Please select at least one day", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (editIndex == null) {
                val newAlarm = AlarmItem(pickedTime, selectedDays.toMutableList(), true)
                alarms.add(newAlarm)
                alarmAdapter.notifyItemInserted(alarms.size - 1)
                scheduleAlarm(newAlarm)
                Toast.makeText(requireContext(), "Alarm added", Toast.LENGTH_SHORT).show()
            } else {
                val alarm = alarms[editIndex]
                cancelAlarm(alarm)
                alarm.time = pickedTime
                alarm.repeatDays = selectedDays.toMutableList()
                alarm.isEnabled = true
                alarmAdapter.notifyItemChanged(editIndex)
                scheduleAlarm(alarm)
                Toast.makeText(requireContext(), "Alarm updated", Toast.LENGTH_SHORT).show()
            }

            saveAlarms()
            updateHeader()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showOptionsDialog(position: Int) {
        val options = arrayOf("Edit", "Delete")
        AlertDialog.Builder(requireContext())
            .setTitle("Alarm Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddOrEditDialog(position)
                    1 -> confirmDelete(position)
                }
            }
            .show()
    }

    private fun confirmDelete(position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Alarm")
            .setMessage("Delete alarm at ${alarms[position].time}?")
            .setPositiveButton("Yes") { _, _ -> deleteAlarm(position) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteAlarm(position: Int) {
        val alarm = alarms[position]
        cancelAlarm(alarm)
        alarms.removeAt(position)
        alarmAdapter.notifyItemRemoved(position)
        saveAlarms()
        updateHeader()
        Toast.makeText(requireContext(), "Alarm deleted", Toast.LENGTH_SHORT).show()
    }

    private fun saveAlarms() {
        AlarmStorage.saveAlarms(requireContext(), alarms)
        updateHeader()
    }

    private fun updateHeader() {
        val activeAlarms = alarms.filter { it.isEnabled }
        if (activeAlarms.isEmpty()) {
            headerCountdown.text = "No alarms set"
            headerDate.text = ""
        } else {
            val now = Calendar.getInstance()
            var nextAlarmTime: Calendar? = null

            for (alarm in activeAlarms) {
                for (day in alarm.repeatDays) {
                    val cal = Calendar.getInstance().apply {
                        val parts = alarm.time.split(" ")
                        val hm = parts[0].split(":")
                        var hour = hm[0].toInt()
                        val minute = hm[1].toInt()
                        val amPm = parts[1]
                        if (amPm == "PM" && hour != 12) hour += 12
                        if (amPm == "AM" && hour == 12) hour = 0

                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        set(Calendar.DAY_OF_WEEK, dayConst(day))

                        if (before(now)) add(Calendar.WEEK_OF_YEAR, 1)
                    }
                    if (nextAlarmTime == null || cal.before(nextAlarmTime)) {
                        nextAlarmTime = cal
                    }
                }
            }

            nextAlarmTime?.let {
                val diffMillis = it.timeInMillis - now.timeInMillis
                val hours = diffMillis / (1000 * 60 * 60)
                val minutes = (diffMillis / (1000 * 60)) % 60

                headerCountdown.text = "Alarm in ${hours} hr ${minutes} min"

                val dayFormat = SimpleDateFormat("EEE, MMM d, h:mm a", Locale.getDefault())
                headerDate.text = dayFormat.format(it.time)
            }
        }
    }

    private fun dayConst(label: String): Int = when (label) {
        "Sun" -> Calendar.SUNDAY
        "Mon" -> Calendar.MONDAY
        "Tue" -> Calendar.TUESDAY
        "Wed" -> Calendar.WEDNESDAY
        "Thu" -> Calendar.THURSDAY
        "Fri" -> Calendar.FRIDAY
        "Sat" -> Calendar.SATURDAY
        else -> Calendar.MONDAY
    }

    private fun scheduleAlarm(alarm: AlarmItem) {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(requireContext(), "Exact alarms are not allowed. Please enable in system settings.", Toast.LENGTH_LONG).show()
                return
            }
        }

        val parts = alarm.time.split(" ")
        val hm = parts[0].split(":")
        var hour = hm[0].toInt()
        val minute = hm[1].toInt()
        val amPm = parts[1]
        if (amPm == "PM" && hour != 12) hour += 12
        if (amPm == "AM" && hour == 12) hour = 0

        for (day in alarm.repeatDays) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.DAY_OF_WEEK, dayConst(day))
                if (before(Calendar.getInstance())) add(Calendar.WEEK_OF_YEAR, 1)
            }

            val intent = Intent(requireContext(), AlarmReceiver::class.java).apply {
                putExtra("alarm_time", alarm.time)
                putExtra("alarm_days", alarm.repeatDays.toTypedArray())
            }

            val requestCode = (alarm.time + day).hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                Toast.makeText(requireContext(), "Exact alarm scheduling requires permission.", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun cancelAlarm(alarm: AlarmItem) {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (day in alarm.repeatDays) {
            val requestCode = (alarm.time + day).hashCode()
            val intent = Intent(requireContext(), AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
