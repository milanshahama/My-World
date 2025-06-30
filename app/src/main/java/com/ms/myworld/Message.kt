package com.ms.myworld

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// This class is a simple blueprint to hold the information
// for a single chat message.
data class Message(
    val senderId: String = "",
    val text: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
)