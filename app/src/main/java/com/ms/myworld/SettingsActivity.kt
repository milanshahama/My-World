package com.ms.myworld

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ms.myworld.databinding.ActivitySettingsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var auth: FirebaseAuth
    // --- NEW: Add a reference to the Firestore database ---
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        auth = Firebase.auth
        updateUI()

        binding.passwordButton.setOnClickListener {
            val user = auth.currentUser ?: return@setOnClickListener

            val providers = user.providerData.map { it.providerId }

            if (EmailAuthProvider.PROVIDER_ID in providers) {
                showChangePasswordDialog()
            } else {
                showSetPasswordDialog()
            }
        }
    }

    private fun updateUI() {
        val user = auth.currentUser ?: return

        // --- THIS IS THE NEW LOGIC TO GET THE SHORT ID ---
        // We will fetch the shortId from the Firestore database.
        binding.userIdTextView.text = "Loading..." // Show a loading message
        val userDocRef = db.collection("users").document(user.uid)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val document = userDocRef.get().await()
                val shortId = if (document != null && document.exists()) {
                    // If the document exists, get the shortId
                    document.getString("shortId") ?: user.uid // Fallback to long ID if field is missing
                } else {
                    // If document doesn't exist (for older users), show the long ID as a fallback
                    user.uid
                }

                // Update the UI on the main thread
                withContext(Dispatchers.Main) {
                    binding.userIdTextView.text = shortId
                }
            } catch (e: Exception) {
                Log.e("SETTINGS_ERROR", "Error fetching user document", e)
                withContext(Dispatchers.Main) {
                    // If there's an error, show the long ID
                    binding.userIdTextView.text = user.uid
                }
            }
        }
        // --- END OF NEW LOGIC ---


        binding.userInfoTextView.text = user.email

        val providers = user.providerData.map { it.providerId }
        val hasPassword = EmailAuthProvider.PROVIDER_ID in providers
        val isGoogleUser = GoogleAuthProvider.PROVIDER_ID in providers

        if (isGoogleUser && !hasPassword) {
            binding.passwordWarningCard.visibility = View.VISIBLE
            binding.passwordButton.text = "Set a Password"
        } else {
            binding.passwordWarningCard.visibility = View.GONE
            binding.passwordButton.text = "Change Password"
        }

        binding.changeEmailButton.isEnabled = false
    }

    private fun showSetPasswordDialog() {
        val setPasswordSheet = SetPasswordBottomSheet()
        setPasswordSheet.setOnPasswordSetListener { passwordWasSet ->
            if (passwordWasSet) {
                updateUI()
            }
        }
        setPasswordSheet.show(supportFragmentManager, "SettingsSetPasswordSheet")
    }

    private fun showChangePasswordDialog() {
        val changePasswordSheet = ChangePasswordBottomSheet()
        changePasswordSheet.setOnPasswordChangeListener { passwordWasChanged ->
            // No action needed here after password change
        }
        changePasswordSheet.show(supportFragmentManager, "SettingsChangePasswordSheet")
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
