package com.ms.myworld

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ms.myworld.databinding.ActivityUserProfileBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private var profileUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        profileUserId = intent.getStringExtra("USER_UID")

        if (profileUserId == null) {
            Toast.makeText(this, "User not found.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        loadUserProfile(profileUserId!!)

        binding.addFriendButton.setOnClickListener {
            // The button's function will change based on the friendship status
            // For now, we connect it to the send request function.
            profileUserId?.let {
                sendFriendRequest(it)
            }
        }
    }

    private fun loadUserProfile(uid: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            finish()
            return
        }

        // Hide button if viewing own profile
        if (uid == currentUser.uid) {
            binding.addFriendButton.visibility = View.GONE
        } else {
            // Check the current relationship status to set the button correctly
            checkFriendshipStatus(currentUser.uid, uid)
        }

        // --- This part remains the same, it just loads the user's info ---
        val userDocRef = db.collection("users").document(uid)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val document = userDocRef.get().await()
                if (document != null && document.exists()) {
                    val displayName = document.getString("displayName") ?: "No Name"
                    val shortId = document.getString("shortId") ?: "N/A"
                    val email = document.getString("email") ?: "No Email"
                    val photoUrl = document.getString("photoUrl")

                    withContext(Dispatchers.Main) {
                        binding.displayNameTextView.text = displayName
                        binding.shortIdTextView.text = shortId
                        binding.emailTextView.text = email
                        binding.profileImageView.load(photoUrl) {
                            crossfade(true)
                            placeholder(R.drawable.ic_person)
                            error(R.drawable.ic_person)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PROFILE_ERROR", "Error loading user profile", e)
            }
        }
    }

    // --- NEW: This function checks the relationship and updates the button ---
    private fun checkFriendshipStatus(currentUid: String, profileUid: String) {
        // Path to the document representing the profile user in the current user's friend list
        val friendDocRef = db.collection("users").document(currentUid)
            .collection("friendships").document(profileUid)

        friendDocRef.get().addOnSuccessListener { document ->
            binding.addFriendButton.visibility = View.VISIBLE // Make button visible
            if (document != null && document.exists()) {
                when (document.getString("status")) {
                    "friends" -> {
                        binding.addFriendButton.text = "Friends"
                        binding.addFriendButton.isEnabled = false
                    }
                    "sent" -> {
                        binding.addFriendButton.text = "Request Sent"
                        binding.addFriendButton.isEnabled = false
                    }
                    "received" -> {
                        binding.addFriendButton.text = "Accept Request"
                        binding.addFriendButton.isEnabled = true
                        // In the future, clicking this would accept the request.
                        // For now, it will still send a request back.
                    }
                    else -> {
                        binding.addFriendButton.text = "Add Friend"
                        binding.addFriendButton.isEnabled = true
                    }
                }
            } else {
                // No document exists, so they are not friends and no request has been sent
                binding.addFriendButton.text = "Add Friend"
                binding.addFriendButton.isEnabled = true
            }
        }.addOnFailureListener {
            // In case of error, just show the default button
            binding.addFriendButton.text = "Add Friend"
            binding.addFriendButton.isEnabled = true
        }
    }

    // --- NEW: This function sends a friend request ---
    private fun sendFriendRequest(friendUid: String) {
        val currentUser = auth.currentUser ?: return

        binding.addFriendButton.isEnabled = false // Disable button to prevent multiple clicks
        Toast.makeText(this, "Sending request...", Toast.LENGTH_SHORT).show()

        // Reference for the request in the current user's list
        val myFriendRequestRef = db.collection("users").document(currentUser.uid)
            .collection("friendships").document(friendUid)

        // Reference for the request in the other user's list
        val theirFriendRequestRef = db.collection("users").document(friendUid)
            .collection("friendships").document(currentUser.uid)

        // The data for the "sent" request
        val sentRequestData = hashMapOf(
            "status" to "sent",
            "timestamp" to FieldValue.serverTimestamp()
        )

        // The data for the "received" request
        val receivedRequestData = hashMapOf(
            "status" to "received",
            "timestamp" to FieldValue.serverTimestamp()
        )

        // Use a batch write to perform both operations at once
        db.batch().apply {
            set(myFriendRequestRef, sentRequestData)
            set(theirFriendRequestRef, receivedRequestData)
        }.commit().addOnSuccessListener {
            Toast.makeText(this, "Friend Request Sent!", Toast.LENGTH_LONG).show()
            binding.addFriendButton.text = "Request Sent"
        }.addOnFailureListener { e ->
            Log.e("FRIEND_REQUEST_ERROR", "Error sending friend request", e)
            Toast.makeText(this, "Failed to send request.", Toast.LENGTH_SHORT).show()
            binding.addFriendButton.isEnabled = true // Re-enable button on failure
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}