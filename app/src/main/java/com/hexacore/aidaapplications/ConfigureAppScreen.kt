package com.hexacore.aidaapplications

import android.content.Context
import android.content.res.Resources
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

        val prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        // --- Initialize UI ---
        btnChangeAvatar = view.findViewById(R.id.btnChangeAvatar)
        btnChangeTheme = view.findViewById(R.id.btnChangeTheme)
        btnRingtones = view.findViewById(R.id.btnRingtones)

        avatarPanel = view.findViewById(R.id.avatarPanel)
        avatarRecyclerView = view.findViewById(R.id.avatarRecyclerView)
        avatarActionButtons = view.findViewById(R.id.avatarActionButtons)
        btnSaveAvatar = view.findViewById(R.id.btnSaveAvatar)
        btnCancelAvatar = view.findViewById(R.id.btnCancelAvatar)
        selectedAvatarPreview = view.findViewById(R.id.selectedAvatarPreview)

        themePanel = view.findViewById(R.id.themePanel)
        themeRadioGroup = view.findViewById(R.id.themeRadioGroup)
        btnSaveTheme = view.findViewById(R.id.btnSaveTheme)
        btnCancelTheme = view.findViewById(R.id.btnCancelTheme)

        ringtonePanel = view.findViewById(R.id.ringtonePanel)
        ringtoneRadioGroup = view.findViewById(R.id.ringtoneRadioGroup)
        btnSaveRingtone = view.findViewById(R.id.btnSaveRingtone)
        btnCancelRingtone = view.findViewById(R.id.btnCancelRingtone)

        // --- Safe Avatar Setup ---
        fun isDrawable(resId: Int): Boolean = try {
            resources.getResourceTypeName(resId) == "drawable"
        } catch (e: Resources.NotFoundException) {
            false
        }

        val savedAvatar = prefs.getInt("selected_avatar", R.drawable.favatar1)
        if (isDrawable(savedAvatar)) {
            selectedAvatarRes = savedAvatar
            selectedAvatarPreview.setImageResource(savedAvatar)
            selectedAvatarPreview.visibility = View.VISIBLE
        } else {
            selectedAvatarPreview.visibility = View.GONE
            prefs.edit().putInt("selected_avatar", R.drawable.favatar1).apply()
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
            if (isDrawable(selectedAvatar)) {
                selectedAvatarRes = selectedAvatar
                selectedAvatarPreview.setImageResource(selectedAvatar)
                selectedAvatarPreview.visibility = View.VISIBLE
            }
        }
        avatarRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        avatarRecyclerView.adapter = adapter

        btnChangeAvatar.setOnClickListener { togglePanel(avatarPanel, ::isAvatarPanelVisible) { isAvatarPanelVisible = it } }
        btnSaveAvatar.setOnClickListener {
            selectedAvatarRes?.let { avatar ->
                prefs.edit().putInt("selected_avatar", avatar).apply()
                closePanel(avatarPanel) { isAvatarPanelVisible = false }
                Toast.makeText(requireContext(), "Avatar saved!", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(requireContext(), "Please select an avatar", Toast.LENGTH_SHORT).show()
        }
        btnCancelAvatar.setOnClickListener { closePanel(avatarPanel) { isAvatarPanelVisible = false } }

        // --- Theme Setup ---
        val savedTheme = prefs.getString("selected_theme", "Light") ?: "Light"
        when (savedTheme) {
            "Light" -> themeRadioGroup.check(R.id.themeLight)
            "Dark" -> themeRadioGroup.check(R.id.themeDark)
            "Blue" -> themeRadioGroup.check(R.id.themeBlue)
            "Green" -> themeRadioGroup.check(R.id.themeGreen)
        }
        btnChangeTheme.setOnClickListener { togglePanel(themePanel, ::isThemePanelVisible) { isThemePanelVisible = it } }
        btnSaveTheme.setOnClickListener {
            val theme = when (themeRadioGroup.checkedRadioButtonId) {
                R.id.themeLight -> "Light"
                R.id.themeDark -> "Dark"
                R.id.themeBlue -> "Blue"
                R.id.themeGreen -> "Green"
                else -> "Light"
            }
            prefs.edit().putString("selected_theme", theme).apply()
            closePanel(themePanel) { isThemePanelVisible = false }
            Toast.makeText(requireContext(), "Theme saved: $theme", Toast.LENGTH_SHORT).show()
            requireActivity().recreate()
        }
        btnCancelTheme.setOnClickListener { closePanel(themePanel) { isThemePanelVisible = false } }

        // --- Ringtone Setup ---
        val savedRingtone = prefs.getInt("selected_ringtone", R.raw.ringtone1)
        fun checkRingtone(id: Int, radioId: Int) { if (savedRingtone == id) ringtoneRadioGroup.check(radioId) }
        checkRingtone(R.raw.ringtone1, R.id.ringtoneClassic)
        checkRingtone(R.raw.ringtone2, R.id.ringtoneBeep)
        checkRingtone(R.raw.ringtone3, R.id.ringtoneGentle)
        checkRingtone(R.raw.ringtone4, R.id.ringtoneWav)

        btnRingtones.setOnClickListener { togglePanel(ringtonePanel, ::isRingtonePanelVisible) { isRingtonePanelVisible = it } }
        btnSaveRingtone.setOnClickListener {
            val ringtone = when (ringtoneRadioGroup.checkedRadioButtonId) {
                R.id.ringtoneClassic -> R.raw.ringtone1
                R.id.ringtoneBeep -> R.raw.ringtone2
                R.id.ringtoneGentle -> R.raw.ringtone3
                R.id.ringtoneWav -> R.raw.ringtone4
                else -> R.raw.ringtone1
            }
            prefs.edit().putInt("selected_ringtone", ringtone).apply()
            closePanel(ringtonePanel) { isRingtonePanelVisible = false }
            Toast.makeText(requireContext(), "Ringtone saved!", Toast.LENGTH_SHORT).show()
        }
        btnCancelRingtone.setOnClickListener { closePanel(ringtonePanel) { isRingtonePanelVisible = false } }

        return view
    }

    // ---- Generic Panel Toggle ----
    private fun togglePanel(panel: LinearLayout, isVisibleFlag: () -> Boolean, setFlag: (Boolean) -> Unit) {
        if (!isVisibleFlag()) {
            panel.visibility = View.VISIBLE
            panel.translationX = panel.width.toFloat()
            panel.animate().translationX(0f).setDuration(300).start()
        } else {
            closePanel(panel, setFlag)
        }
        setFlag(!isVisibleFlag())
    }

    private fun closePanel(panel: LinearLayout, setFlag: (Boolean) -> Unit) {
        panel.animate().translationX(panel.width.toFloat()).setDuration(300)
            .withEndAction { panel.visibility = View.GONE }
            .start()
        setFlag(false)
    }
}
