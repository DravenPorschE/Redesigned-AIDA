package com.hexacore.aidaapplications

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class CalendarEventAdapter(
    private var events: MutableList<CalendarEvent>
) : RecyclerView.Adapter<CalendarEventAdapter.EventViewHolder>() {

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.txtEventDate)
        val noteText: TextView = view.findViewById(R.id.txtEventNote)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]

        // Format date (from yyyy-MM-dd → MMM dd, yyyy)
        val formattedDate = try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val parsedDate = parser.parse(event.date)
            formatter.format(parsedDate ?: event.date)
        } catch (e: Exception) {
            event.date // fallback
        }

        // ✅ Bright Teal for date
        holder.dateText.text = formattedDate
        holder.dateText.setTextColor(Color.parseColor("#00E7A2"))

        // ✅ White for note
        holder.noteText.text = event.note
        holder.noteText.setTextColor(Color.WHITE)
    }

    override fun getItemCount(): Int = events.size

    fun updateData(newEvents: List<CalendarEvent>) {
        events.clear()
        events.addAll(newEvents.sortedBy { it.date }) // sort by date for readability
        notifyDataSetChanged()
    }
}
