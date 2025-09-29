package com.hexacore.aidaapplications.ui.theme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.hexacore.aidaapplications.R

class AvatarAdapter(
    private val avatars: List<Int>,
    private val onAvatarSelected: (Int) -> Unit
) : RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    inner class AvatarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatarImage: ImageView = itemView.findViewById(R.id.avatarImage)
        val cardContainer: MaterialCardView = itemView.findViewById(R.id.avatarCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvatarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_avatar, parent, false)
        return AvatarViewHolder(view)
    }

    override fun onBindViewHolder(holder: AvatarViewHolder, position: Int) {
        val avatarRes = avatars[position]
        holder.avatarImage.setImageResource(avatarRes)

        // Highlight selection with border
        if (position == selectedPosition) {
            holder.cardContainer.strokeColor =
                ContextCompat.getColor(holder.itemView.context, R.color.teal_200)
            holder.cardContainer.strokeWidth = 6
        } else {
            holder.cardContainer.strokeWidth = 0
        }

        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition

            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)

            onAvatarSelected(avatarRes) // callback
        }
    }

    override fun getItemCount(): Int = avatars.size
}
