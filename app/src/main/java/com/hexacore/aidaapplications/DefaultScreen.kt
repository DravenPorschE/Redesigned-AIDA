package com.hexacore.aidaapplications

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment

class DefaultScreen : Fragment() {

    private lateinit var avatarAidaImage: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.default_screen, container, false)

        avatarAidaImage = view.findViewById(R.id.aida_avatar)

        // Load saved avatar from SharedPreferences
        val prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val savedAvatar = prefs.getInt("selected_avatar", R.drawable.avatar_aida) // fallback avatar
        if (resources.getResourceTypeName(savedAvatar) == "drawable") {
            avatarAidaImage.setImageResource(savedAvatar)
        } else {
            avatarAidaImage.setImageResource(R.drawable.avatar_aida) // fallback
        }


        return view
    }
}
