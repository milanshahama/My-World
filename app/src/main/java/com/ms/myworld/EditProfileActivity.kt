package com.ms.myworld

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ms.myworld.databinding.ActivityEditProfileBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        auth = Firebase.auth
        loadUserProfile()

        binding.saveButton.setOnClickListener {
            saveProfileChanges()
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "You must be logged in to edit your profile.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.displayNameEditText.setText(user.displayName)
        binding.profileImageView.load(user.photoUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_person)
            error(R.drawable.ic_person)
        }
    }

    private fun saveProfileChanges() {
        val user = auth.currentUser ?: return
        val newDisplayName = binding.displayNameEditText.text.toString().trim()

        if (newDisplayName.isEmpty()) {
            binding.displayNameLayout.error = "Display name cannot be empty."
            return
        }

        binding.displayNameLayout.error = null // Clear any previous errors
        val savingToast = Toast.makeText(this, "Saving...", Toast.LENGTH_LONG)
        savingToast.show()

        // Step 1: Update the profile in Firebase Authentication
        val profileUpdates = userProfileChangeRequest {
            displayName = newDisplayName
            // In the future, you can update the photo URI here as well
        }

        user.updateProfile(profileUpdates).addOnCompleteListener { authTask ->
            if (authTask.isSuccessful) {
                // Step 2: Update the profile in Firestore using a Coroutine
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Update the main user document
                        val userDocRef = db.collection("users").document(user.uid)
                        userDocRef.update("displayName", newDisplayName).await()

                        // Update the displayName in all friendships for this user
                        updateFriendshipDocuments(user.uid, newDisplayName)

                        withContext(Dispatchers.Main) {
                            savingToast.cancel()
                            Toast.makeText(this@EditProfileActivity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                            finish() // Go back to the previous screen
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            savingToast.cancel()
                            Toast.makeText(this@EditProfileActivity, "Failed to update profile in database.", Toast.LENGTH_SHORT).show()
                            Log.e("EDIT_PROFILE_ERROR", "Error updating Firestore", e)
                        }
                    }
                }
            } else {
                savingToast.cancel()
                Toast.makeText(this, "Failed to update profile.", Toast.LENGTH_SHORT).show()
                Log.e("EDIT_PROFILE_ERROR", "Error updating Firebase Auth profile", authTask.exception)
            }
        }
    }

    // This function finds all friends of the current user and updates the user's name in their friend list.
    private suspend fun updateFriendshipDocuments(myUid: String, newDisplayName: String) {
        // Path to the current user's own friendship list
        val myFriendshipsRef = db.collection("users").document(myUid).collection("friendships")
            .whereEqualTo("status", "friends")

        try {
            val snapshot = myFriendshipsRef.get().await()
            val friendIds = snapshot.documents.map { it.id }

            // Create a batch write to update all documents efficiently
            val batch = db.batch()
            for (friendId in friendIds) {
                // Reference to the doc in the friend's list that represents the current user
                val friendSideDocRef = db.collection("users")
                    .document(friendId)
                    .collection("friendships")
                    .document(myUid)

                batch.update(friendSideDocRef, "displayName", newDisplayName)
            }
            batch.commit().await()
            Log.d("EDIT_PROFILE", "Successfully updated displayName for all friends.")
        } catch (e: Exception) {
            Log.e("EDIT_PROFILE_ERROR", "Error updating friendship documents", e)
            // We can choose to notify the user or just log the error
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}