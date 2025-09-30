package com.hexacore.aidaapplications

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Button
import java.text.SimpleDateFormat
import java.util.*

class CalendarAppScreen : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CalendarEventAdapter
    private lateinit var calendarView: CalendarView
    private lateinit var monthLabel: TextView
    private lateinit var btnMonthEvents: Button

    private val allEvents = mutableMapOf<String, MutableList<CalendarEvent>>() // month -> events
    private var currentMonth: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.calendar_app, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewEvents)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.visibility = View.GONE // hidden initially

        calendarView = view.findViewById(R.id.calendarView)
        monthLabel = view.findViewById(R.id.txtMonthLabel)
        btnMonthEvents = view.findViewById(R.id.btnMonthEvents)

        // Load saved events
        allEvents.putAll(CalendarStorage.loadEvents(requireContext()))

        // Adapter setup
        adapter = CalendarEventAdapter(mutableListOf())
        recyclerView.adapter = adapter

        // Default: current month
        val calendar = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        currentMonth = monthFormat.format(calendar.time)
        monthLabel.text = "Events in $currentMonth"
        btnMonthEvents.text = "Show Events in $currentMonth"

        // Listen to day clicks → updates current month & hides list
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val shownCalendar = Calendar.getInstance()
            shownCalendar.set(year, month, dayOfMonth)
            currentMonth = monthFormat.format(shownCalendar.time)

            // update texts
            monthLabel.text = "Events in $currentMonth"
            btnMonthEvents.text = "Show Events in $currentMonth"

            // hide list when month changes
            recyclerView.visibility = View.GONE

            // prepare clicked date for add-event dialog
            val clickedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            showAddEventDialog(clickedDate)
        }

        // Button → show events for the month
        btnMonthEvents.setOnClickListener {
            val eventsForMonth = allEvents[currentMonth] ?: mutableListOf()
            if (eventsForMonth.isEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("No Events")
                    .setMessage("There’s no event in $currentMonth")
                    .setPositiveButton("OK", null)
                    .show()
                recyclerView.visibility = View.GONE
            } else {
                adapter.updateData(eventsForMonth)
                recyclerView.visibility = View.VISIBLE
            }
        }

        return view
    }

    private fun showAddEventDialog(date: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_event, null)
        val noteInput = dialogView.findViewById<EditText>(R.id.eventNoteInput)
        val dateLabel = dialogView.findViewById<TextView>(R.id.eventDateLabel)

        // Show fixed date (not editable)
        val displayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedDate = parser.parse(date)
        dateLabel.text = displayFormat.format(parsedDate ?: date)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Event")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val note = noteInput.text.toString().trim()
                if (note.isNotEmpty()) {
                    val newEvent = CalendarEvent(date, note)

                    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    val monthKey = monthFormat.format(parsedDate ?: Date())

                    val eventsForMonth = allEvents.getOrPut(monthKey) { mutableListOf() }
                    eventsForMonth.add(newEvent)

                    // Save immediately
                    CalendarStorage.saveEvents(requireContext(), allEvents)
                } else {
                    Toast.makeText(requireContext(), "Please enter a note", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
