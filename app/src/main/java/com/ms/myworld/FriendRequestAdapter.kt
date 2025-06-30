package com.ms.myworld

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.ms.myworld.databinding.ItemFriendRequestBinding

class FriendRequestAdapter(
    private val listener: OnRequestActionListener
) : ListAdapter<FriendRequest, FriendRequestAdapter.FriendRequestViewHolder>(DiffCallback()) {

    interface OnRequestActionListener {
        fun onAccept(request: FriendRequest)
        fun onDecline(request: FriendRequest)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendRequestViewHolder {
        val binding = ItemFriendRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FriendRequestViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: FriendRequestViewHolder, position: Int) {
        val currentRequest = getItem(position)
        holder.bind(currentRequest)
    }

    inner class FriendRequestViewHolder(
        private val binding: ItemFriendRequestBinding,
        private val listener: OnRequestActionListener
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(request: FriendRequest) {
            binding.displayNameTextView.text = request.displayName ?: "Unknown User"

            // --- THE FIX IS HERE ---
            // The line for setting the shortIdTextView has been removed.

            binding.profileImageView.load(request.photoUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_person)
                error(R.drawable.ic_person)
            }

            binding.acceptButton.setOnClickListener {
                listener.onAccept(request)
            }
            binding.declineButton.setOnClickListener {
                listener.onDecline(request)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<FriendRequest>() {
        override fun areItemsTheSame(oldItem: FriendRequest, newItem: FriendRequest) =
            oldItem.uid == newItem.uid

        override fun areContentsTheSame(oldItem: FriendRequest, newItem: FriendRequest) =
            oldItem == newItem
    }
}