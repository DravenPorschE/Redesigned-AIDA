package com.hexacore.aidaapplications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AlarmAdapter(
    private val alarms: MutableList<AlarmItem>,
    private val onSwitchToggle: (AlarmItem, Boolean) -> Unit,
    private val onItemLongClick: (Int) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    inner class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeText: TextView = itemView.findViewById(R.id.alarm_time)
        val repeatText: TextView = itemView.findViewById(R.id.alarm_repeat)
        val switch: Switch = itemView.findViewById(R.id.alarm_switch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val alarm = alarms[position]

        holder.timeText.text = alarm.time
        holder.repeatText.text = if (alarm.repeatDays.isEmpty()) "Once" else alarm.repeatDays.joinToString(", ")

        // remove listener first to avoid recycled callbacks
        holder.switch.setOnCheckedChangeListener(null)
        holder.switch.isChecked = alarm.isEnabled
        holder.switch.setOnCheckedChangeListener { _, isChecked ->
            onSwitchToggle(alarm, isChecked)
        }

        // long press (position) => show options (Edit/Delete)
        holder.itemView.setOnLongClickListener {
            onItemLongClick(position)
            true
        }
    }

    override fun getItemCount(): Int = alarms.size
}
