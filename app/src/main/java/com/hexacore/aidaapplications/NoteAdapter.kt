package com.hexacore.aidaapplications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView Adapter for displaying a list of notes.
 * Handles binding data and delete actions.
 */
class NoteAdapter(
    private val notes: MutableList<Note>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.note_title)
        val infoText: TextView = itemView.findViewById(R.id.note_info)
        val deleteButton: Button = itemView.findViewById(R.id.delete_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.note_item, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.titleText.text = note.title
        holder.infoText.text = note.info

        holder.deleteButton.setOnClickListener {
            onDelete(position)
        }
    }

    override fun getItemCount(): Int = notes.size

    fun addNote(note: Note) {
        notes.add(note)
        notifyItemInserted(notes.size - 1)
    }

    fun deleteNoteAt(position: Int) {
        if (position in notes.indices) {
            notes.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}