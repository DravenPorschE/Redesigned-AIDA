package com.hexacore.aidaapplications

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class ThemeSetting : Fragment() {

    private lateinit var themeListView: ListView
    private lateinit var previewBox: View
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private var selectedTheme: String? = null

    private val themes = listOf("Light", "Dark", "Blue", "Green")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.theme_settings, container, false)

        themeListView = view.findViewById(R.id.themeListView)
        previewBox = view.findViewById(R.id.previewBox)
        btnSave = view.findViewById(R.id.btnSaveTheme)
        btnCancel = view.findViewById(R.id.btnCancelTheme)

        // Load saved theme
        val prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val savedTheme = prefs.getString("selected_theme", "Light")
        selectedTheme = savedTheme

        // Setup list
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_single_choice,
            themes
        )
        themeListView.adapter = adapter
        themeListView.choiceMode = ListView.CHOICE_MODE_SINGLE
        val savedIndex = themes.indexOf(savedTheme)
        if (savedIndex != -1) themeListView.setItemChecked(savedIndex, true)

        updatePreview(savedTheme ?: "Light")

        themeListView.setOnItemClickListener { _, _, position, _ ->
            selectedTheme = themes[position]
            updatePreview(selectedTheme!!)
        }

        btnSave.setOnClickListener {
            selectedTheme?.let {
                prefs.edit().putString("selected_theme", it).apply()
                Toast.makeText(requireContext(), "Theme saved: $it", Toast.LENGTH_SHORT).show()

                // 🔥 Apply theme immediately
                requireActivity().recreate()
            }
            parentFragmentManager.popBackStack() // Go back
        }

        btnCancel.setOnClickListener {
            Toast.makeText(requireContext(), "Theme change cancelled", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack() // Go back
        }

        return view
    }

    private fun updatePreview(theme: String) {
        val color = when (theme) {
            "Light" -> android.R.color.white
            "Dark" -> android.R.color.black
            "Blue" -> android.R.color.holo_blue_light
            "Green" -> android.R.color.holo_green_light
            else -> android.R.color.darker_gray
        }
        previewBox.setBackgroundResource(color)
    }
}
