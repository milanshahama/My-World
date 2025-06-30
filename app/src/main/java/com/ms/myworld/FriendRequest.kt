package com.ms.myworld

// This class is a simple blueprint to hold the information
// for each user who has sent a friend request.
data class FriendRequest(
    val uid: String = "",
    val displayName: String? = null,
    val shortId: String? = null,
    val photoUrl: String? = null
)