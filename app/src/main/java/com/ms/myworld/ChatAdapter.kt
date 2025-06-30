package com.ms.myworld

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.ms.myworld.databinding.ItemChatPreviewBinding

class ChatAdapter(
    // A listener to tell our Activity when a friend in the list is clicked.
    private val onFriendClickListener: (Friend) -> Unit
) : ListAdapter<Friend, ChatAdapter.ChatViewHolder>(DiffCallback()) {

    // This creates the ViewHolder for each row in our list.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatPreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    // This connects the data from a specific friend to the views in a ViewHolder.
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val currentFriend = getItem(position)
        holder.bind(currentFriend)
    }

    // This ViewHolder holds the views for a single friend in the list.
    inner class ChatViewHolder(private val binding: ItemChatPreviewBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            // Set a click listener on the entire row.
            binding.root.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onFriendClickListener(getItem(adapterPosition))
                }
            }
        }

        fun bind(friend: Friend) {
            binding.displayNameTextView.text = friend.displayName ?: "Unknown User"
            binding.lastMessageTextView.text = friend.lastMessage ?: "Tap to start chatting..."

            // Load the user's profile picture using Coil
            binding.profileImageView.load(friend.photoUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_person)
                error(R.drawable.ic_person)
            }
        }
    }

    // This helps the adapter efficiently update the list.
    private class DiffCallback : DiffUtil.ItemCallback<Friend>() {
        override fun areItemsTheSame(oldItem: Friend, newItem: Friend) =
            oldItem.uid == newItem.uid

        override fun areContentsTheSame(oldItem: Friend, newItem: Friend) =
            oldItem == newItem
    }
}