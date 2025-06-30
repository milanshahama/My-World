package com.ms.myworld

// This class is a simple blueprint to hold the information
// for each accepted friend that will be shown in the chat list.
data class Friend(
    val uid: String = "",
    val displayName: String? = null,
    val shortId: String? = null,
    val photoUrl: String? = null,
    // We add this field to show a placeholder message in the chat list
    val lastMessage: String? = "Tap to start chatting..."
)