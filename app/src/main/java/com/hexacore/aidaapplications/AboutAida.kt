package com.hexacore.aidaapplications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.TextView
import android.widget.Button

class AboutAidaFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.about_aida, container, false)

        val aboutText = view.findViewById<TextView>(R.id.tvAboutText)
        aboutText.text = """
            A🤖 What AIDA Can Do:
            
            📓 Notes
            • Manually add and edit notes.
            • Voice: "Create a new note with a title New Note and the content is Agenda for today is to order food."
            • Voice: "Delete a note with a title <note title>."
    
            📅 Calendar
            • Manually view and navigate months.
            • Voice: "Create a calendar event on <month> <day> <year> about Company outing."
    
            📝 Meetings
            • Voice: "Start live meeting" → begins transcription.
            • Voice: "Summarize the meeting for me" → generates a summary.
    
            🌐 Search
            • Voice: "Search what/when/where/who/how is ..." → performs an online search.
        """.trimIndent()

        // Close button → go back to ConfigureAppScreen
        val btnClose = view.findViewById<Button>(R.id.btnCloseAbout)
        btnClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }
}
