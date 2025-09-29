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
    private val fileName = "notes.txt"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.note_app, container, false)

        val recycler = root.findViewById<RecyclerView>(R.id.note_recycler)
        val addButton = root.findViewById<FloatingActionButton>(R.id.add_note_button)

        loadNotes() // ✅ Load notes from file

        noteAdapter = NoteAdapter(notes) { pos ->
            notes.removeAt(pos)
            noteAdapter.notifyItemRemoved(pos)
            saveNotes() // ✅ Save after delete
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
                saveNotes() // ✅ Save after adding
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveNotes() {
        val file = requireContext().getFileStreamPath(fileName)
        file.printWriter().use { out ->
            for (note in notes) {
                // Store as "title||info" per line
                out.println("${note.title}||${note.info}")
            }
        }
    }

    private fun loadNotes() {
        val file = requireContext().getFileStreamPath(fileName)
        if (file.exists()) {
            file.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split("||")
                    if (parts.size == 2) {
                        notes.add(Note(parts[0], parts[1]))
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        saveNotes() // ✅ Auto-save when app goes to background
    }
}