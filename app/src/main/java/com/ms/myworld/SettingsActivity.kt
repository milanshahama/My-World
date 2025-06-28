package com.ms.myworld

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.ms.myworld.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var auth: FirebaseAuth

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

            // THIS IS THE NEW SMART LOGIC
            // Check what sign-in methods the user has.
            val providers = user.providerData.map { it.providerId }

            if (EmailAuthProvider.PROVIDER_ID in providers) {
                // If they have a password provider, it means they are an email user
                // or a Google user who has already set a password. Show the CHANGE dialog.
                showChangePasswordDialog()
            } else {
                // If they do not have a password provider, it means they are a Google user
                // who needs to set a password for the first time. Show the SET dialog.
                showSetPasswordDialog()
            }
        }
    }

    private fun updateUI() {
        val user = auth.currentUser ?: return
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

        binding.changeEmailButton.isEnabled = false // Remains disabled as requested
    }

    private fun showSetPasswordDialog() {
        val setPasswordSheet = SetPasswordBottomSheet()
        setPasswordSheet.setOnPasswordSetListener { passwordWasSet ->
            if (passwordWasSet) {
                updateUI() // Refresh the screen to hide the warning card and update button text
            }
        }
        setPasswordSheet.show(supportFragmentManager, "SettingsSetPasswordSheet")
    }

    private fun showChangePasswordDialog() {
        val changePasswordSheet = ChangePasswordBottomSheet()
        changePasswordSheet.setOnPasswordChangeListener { passwordWasChanged ->
            if (passwordWasChanged) {
                // No UI change is needed here, but we could add a success message
            }
        }
        changePasswordSheet.show(supportFragmentManager, "SettingsChangePasswordSheet")
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}