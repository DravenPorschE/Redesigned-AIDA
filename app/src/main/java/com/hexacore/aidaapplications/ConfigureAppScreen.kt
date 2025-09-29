package com.hexacore.aidaapplications

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hexacore.aidaapplications.ui.theme.AvatarAdapter

class ConfigureAppScreen : Fragment() {

    private lateinit var btnChangeAvatar: View
    private lateinit var btnChangeTheme: View
    private lateinit var btnRingtones: View

    // Avatar
    private lateinit var avatarRecyclerView: RecyclerView
    private lateinit var avatarActionButtons: LinearLayout
    private lateinit var btnSaveAvatar: Button
    private lateinit var btnCancelAvatar: Button
    private lateinit var selectedAvatarPreview: ImageView
    private lateinit var avatarPanel: LinearLayout
    private lateinit var adapter: AvatarAdapter
    private var selectedAvatarRes: Int? = null
    private var isAvatarPanelVisible = false

    // Theme
    private lateinit var themePanel: LinearLayout
    private lateinit var themeRadioGroup: RadioGroup
    private lateinit var btnSaveTheme: Button
    private lateinit var btnCancelTheme: Button
    private var isThemePanelVisible = false

    // Ringtone
    private lateinit var ringtonePanel: LinearLayout
    private lateinit var ringtoneRadioGroup: RadioGroup
    private lateinit var btnSaveRingtone: Button
    private lateinit var btnCancelRingtone: Button
    private var isRingtonePanelVisible = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.configure_app_screen, container, false)

        // Init buttons
        btnChangeAvatar = view.findViewById(R.id.btnChangeAvatar)
        btnChangeTheme = view.findViewById(R.id.btnChangeTheme)
        btnRingtones = view.findViewById(R.id.btnRingtones)

        // Avatar panel
        avatarPanel = view.findViewById(R.id.avatarPanel)
        avatarRecyclerView = view.findViewById(R.id.avatarRecyclerView)
        avatarActionButtons = view.findViewById(R.id.avatarActionButtons)
        btnSaveAvatar = view.findViewById(R.id.btnSaveAvatar)
        btnCancelAvatar = view.findViewById(R.id.btnCancelAvatar)
        selectedAvatarPreview = view.findViewById(R.id.selectedAvatarPreview)

        // Theme panel
        themePanel = view.findViewById(R.id.themePanel)
        themeRadioGroup = view.findViewById(R.id.themeRadioGroup)
        btnSaveTheme = view.findViewById(R.id.btnSaveTheme)
        btnCancelTheme = view.findViewById(R.id.btnCancelTheme)

        // Ringtone panel
        ringtonePanel = view.findViewById(R.id.ringtonePanel)
        ringtoneRadioGroup = view.findViewById(R.id.ringtoneRadioGroup)
        btnSaveRingtone = view.findViewById(R.id.btnSaveRingtone)
        btnCancelRingtone = view.findViewById(R.id.btnCancelRingtone)

        // Preferences
        val prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        // --- Avatar Setup (unchanged) ---
        val savedAvatar = prefs.getInt("selected_avatar", -1)
        if (savedAvatar != -1) {
            selectedAvatarRes = savedAvatar
            selectedAvatarPreview.setImageResource(savedAvatar)
            selectedAvatarPreview.visibility = View.VISIBLE
        } else {
            selectedAvatarPreview.visibility = View.GONE
        }

        val avatarList = listOf(
            R.drawable.favatar1, R.drawable.favatar2, R.drawable.favatar3,
            R.drawable.favatar4, R.drawable.favatar5, R.drawable.favatar6,
            R.drawable.favatar7, R.drawable.favatar8, R.drawable.favatar9,
            R.drawable.mavatar1, R.drawable.mavatar2, R.drawable.mavatar3,
            R.drawable.mavatar4, R.drawable.mavatar5, R.drawable.mavatar6,
            R.drawable.mavatar7, R.drawable.mavatar8, R.drawable.mavatar9,
            R.drawable.mavatar10
        )

        adapter = AvatarAdapter(avatars = avatarList) { selectedAvatar ->
            selectedAvatarRes = selectedAvatar
            selectedAvatarPreview.setImageResource(selectedAvatar)
            selectedAvatarPreview.visibility = View.VISIBLE
        }
        avatarRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        avatarRecyclerView.adapter = adapter

        btnChangeAvatar.setOnClickListener { toggleAvatarPanel() }
        btnSaveAvatar.setOnClickListener {
            selectedAvatarRes?.let { avatar ->
                prefs.edit().putInt("selected_avatar", avatar).apply()
                closeAvatarPanel()
                Toast.makeText(requireContext(), "Avatar saved!", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(requireContext(), "Please select an avatar", Toast.LENGTH_SHORT).show()
        }
        btnCancelAvatar.setOnClickListener { closeAvatarPanel() }

        val btnAboutAida = view.findViewById<View>(R.id.btnAboutAida)
        btnAboutAida.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .replace((view.parent as ViewGroup).id, AboutAidaFragment())
                .addToBackStack(null)
                .commit()
        }

        // --- Theme Setup (Light/Dark/Blue/Green restored) ---
        val savedTheme = prefs.getString("selected_theme", "Light")
        when (savedTheme) {
            "Light" -> themeRadioGroup.check(R.id.themeLight)
            "Dark" -> themeRadioGroup.check(R.id.themeDark)
            "Blue" -> themeRadioGroup.check(R.id.themeBlue)
            "Green" -> themeRadioGroup.check(R.id.themeGreen)
        }

        btnChangeTheme.setOnClickListener { toggleThemePanel() }
        btnSaveTheme.setOnClickListener {
            val selectedId = themeRadioGroup.checkedRadioButtonId
            val theme = when (selectedId) {
                R.id.themeLight -> "Light"
                R.id.themeDark -> "Dark"
                R.id.themeBlue -> "Blue"
                R.id.themeGreen -> "Green"
                else -> "Light"
            }
            prefs.edit().putString("selected_theme", theme).apply()
            closeThemePanel()
            Toast.makeText(requireContext(), "Theme saved: $theme", Toast.LENGTH_SHORT).show()
            requireActivity().recreate()
        }
        btnCancelTheme.setOnClickListener { closeThemePanel() }

        // --- Ringtone Setup (4 choices restored) ---
        val savedRingtone = prefs.getInt("selected_ringtone", R.raw.ringtone1)
        when (savedRingtone) {
            R.raw.ringtone1 -> ringtoneRadioGroup.check(R.id.ringtoneClassic)
            R.raw.ringtone2 -> ringtoneRadioGroup.check(R.id.ringtoneBeep)
            R.raw.ringtone3 -> ringtoneRadioGroup.check(R.id.ringtoneGentle)
            R.raw.ringtone4 -> ringtoneRadioGroup.check(R.id.ringtoneWav)
        }

        btnRingtones.setOnClickListener { toggleRingtonePanel() }
        btnSaveRingtone.setOnClickListener {
            val selectedId = ringtoneRadioGroup.checkedRadioButtonId
            val ringtone = when (selectedId) {
                R.id.ringtoneClassic -> R.raw.ringtone1
                R.id.ringtoneBeep -> R.raw.ringtone2
                R.id.ringtoneGentle -> R.raw.ringtone3
                R.id.ringtoneWav -> R.raw.ringtone4
                else -> R.raw.ringtone1
            }
            prefs.edit().putInt("selected_ringtone", ringtone).apply()
            closeRingtonePanel()
            Toast.makeText(requireContext(), "Ringtone saved!", Toast.LENGTH_SHORT).show()
        }
        btnCancelRingtone.setOnClickListener { closeRingtonePanel() }

        return view
    }

    // ---- Avatar Panel Controls ----
    private fun toggleAvatarPanel() {
        if (!isAvatarPanelVisible) {
            avatarPanel.visibility = View.VISIBLE
            avatarPanel.translationX = avatarPanel.width.toFloat()
            avatarPanel.animate().translationX(0f).setDuration(300).start()
        } else {
            closeAvatarPanel()
        }
        isAvatarPanelVisible = !isAvatarPanelVisible
    }

    private fun closeAvatarPanel() {
        avatarPanel.animate().translationX(avatarPanel.width.toFloat()).setDuration(300)
            .withEndAction { avatarPanel.visibility = View.GONE }.start()
        isAvatarPanelVisible = false
    }

    // ---- Theme Panel Controls ----
    private fun toggleThemePanel() {
        if (!isThemePanelVisible) {
            themePanel.visibility = View.VISIBLE
            themePanel.translationX = themePanel.width.toFloat()
            themePanel.animate().translationX(0f).setDuration(300).start()
        } else {
            closeThemePanel()
        }
        isThemePanelVisible = !isThemePanelVisible
    }

    private fun closeThemePanel() {
        themePanel.animate().translationX(themePanel.width.toFloat()).setDuration(300)
            .withEndAction { themePanel.visibility = View.GONE }.start()
        isThemePanelVisible = false
    }

    // ---- Ringtone Panel Controls ----
    private fun toggleRingtonePanel() {
        if (!isRingtonePanelVisible) {
            ringtonePanel.visibility = View.VISIBLE
            ringtonePanel.translationX = ringtonePanel.width.toFloat()
            ringtonePanel.animate().translationX(0f).setDuration(300).start()
        } else {
            closeRingtonePanel()
        }
        isRingtonePanelVisible = !isRingtonePanelVisible
    }

    private fun closeRingtonePanel() {
        ringtonePanel.animate().translationX(ringtonePanel.width.toFloat()).setDuration(300)
            .withEndAction { ringtonePanel.visibility = View.GONE }.start()
        isRingtonePanelVisible = false
    }
}
