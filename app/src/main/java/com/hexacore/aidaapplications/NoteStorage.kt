package com.hexacore.aidaapplications

import android.content.Context
import android.util.Log

object NoteStorage {
    private const val fileName = "notes.txt"

    fun saveNotes(context: Context, newNotes: List<Note>) {
        val existing = loadNotes(context).toMutableList()
        existing.clear()
        existing.addAll(newNotes)

        val file = context.getFileStreamPath(fileName)
        file.printWriter().use { out ->
            for (note in existing) {
                out.println("${note.title}||${note.info}")
            }
        }

        Log.d("AIDA_DEBUG", "Saved ${existing.size} notes to storage")
    }


    fun loadNotes(context: Context): MutableList<Note> {
        val notes = mutableListOf<Note>()
        val file = context.getFileStreamPath(fileName)
        if (file.exists()) {
            file.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split("||")
                    if (parts.size == 2) {
                        notes.add(Note(parts[0], parts[1]))
                        Log.d("AIDA_DEBUG", "Loaded note → Title: '${parts[0]}', Content: '${parts[1].take(50)}...'")
                    }
                }
            }
        }
        Log.d("AIDA_DEBUG", "Total notes loaded: ${notes.size}")
        return notes
    }
}
