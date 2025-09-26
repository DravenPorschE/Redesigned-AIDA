package com.hexacore.aidaapplications

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class NoteAppScreen : Fragment() {

    private val notes = mutableListOf<Note>()
    private lateinit var noteAdapter: NoteAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.note_app, container, false)

        val recycler = root.findViewById<RecyclerView>(R.id.note_recycler)
        val addButton = root.findViewById<FloatingActionButton>(R.id.add_note_button)

        noteAdapter = NoteAdapter(notes) { pos ->
            notes.removeAt(pos)
            noteAdapter.notifyItemRemoved(pos)
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = noteAdapter

        addButton.setOnClickListener { showAddNoteDialog() }

        return root
    }

    private fun showAddNoteDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_note, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.input_title)
        val infoInput = dialogView.findViewById<EditText>(R.id.input_info)

        AlertDialog.Builder(requireContext())
            .setTitle("New Note")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = titleInput.text.toString().ifBlank { "Untitled" }
                val info = infoInput.text.toString()
                notes.add(Note(title, info))
                noteAdapter.notifyItemInserted(notes.size - 1)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
