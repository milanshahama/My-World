package com.ms.myworld

import android.content.Intent
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import coil.load
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ms.myworld.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), ProfileMenuBottomSheet.BottomSheetListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth

        binding.profileImageContainer.setOnClickListener {
            val profileMenu = ProfileMenuBottomSheet()
            profileMenu.show(supportFragmentManager, "ProfileMenuBottomSheet")
        }

        binding.chatCard.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        binding.searchIcon.setOnClickListener {
            Toast.makeText(this, "This will be a new feature soon!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        updateUI()
    }

    override fun onOptionSelected(option: String) {
        when (option) {
            "edit_profile" -> startActivity(Intent(this, EditProfileActivity::class.java)) // <-- NEW
            "settings" -> startActivity(Intent(this, SettingsActivity::class.java))
            "logout" -> signOut()
        }
    }

    private fun checkAndCreateUserProfile(user: FirebaseUser) {
        val userDocRef = db.collection("users").document(user.uid)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val document = userDocRef.get().await()
                if (!document.exists()) {
                    Log.d("PROFILE_FIX", "User profile for ${user.email} not found. Creating one now.")
                    val baseId = "ms" + user.uid.take(4).toLowerCase(Locale.ROOT)
                    var shortId = baseId
                    var isUnique = false
                    var attempt = 0
                    while (!isUnique) {
                        val query = db.collection("users").whereEqualTo("shortId", shortId).get().await()
                        if (query.isEmpty) { isUnique = true }
                        else { attempt++; shortId = "$baseId$attempt" }
                    }
                    val userProfile = hashMapOf(
                        "uid" to user.uid, "email" to user.email,
                        "email_lowercase" to user.email?.toLowerCase(Locale.ROOT),
                        "displayName" to user.displayName, "photoUrl" to user.photoUrl.toString(),
                        "shortId" to shortId, "createdAt" to System.currentTimeMillis()
                    )
                    userDocRef.set(userProfile).await()
                    Log.d("PROFILE_FIX", "Successfully created profile for ${user.email} with shortId: $shortId")
                }
            } catch (e: Exception) {
                Log.e("PROFILE_FIX_ERROR", "Error checking/creating user profile", e)
            }
        }
    }

    private fun updateUI() {
        val user = auth.currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        checkAndCreateUserProfile(user)

        val welcomeText = if (user.displayName.isNullOrEmpty()) {
            val nameFromEmail = user.email?.split("@")?.get(0)?.replaceFirstChar { it.uppercase() } ?: "User"
            "Hello, $nameFromEmail"
        } else { "Hello, ${user.displayName}" }
        binding.welcomeTextView.text = welcomeText
        applyTextGradient(binding.welcomeTextView)

        if (user.photoUrl != null) {
            binding.profileImageView.load(user.photoUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_person)
                error(R.drawable.ic_person)
            }
        } else { binding.profileImageView.load(R.drawable.ic_person) }

        val providers = user.providerData.map { it.providerId }
        val hasPassword = EmailAuthProvider.PROVIDER_ID in providers
        val isGoogleUser = GoogleAuthProvider.PROVIDER_ID in providers
        val needsPasswordReminder = isGoogleUser && !hasPassword

        binding.redDotIndicator.visibility = if (needsPasswordReminder) View.VISIBLE else View.GONE
        binding.greenDotIndicator.visibility = if (hasPassword) View.VISIBLE else View.GONE

        if (needsPasswordReminder && ReminderManager.shouldShowReminder(this)) {
            showPasswordReminderAlert()
        }

        binding.dateTextView.text = SimpleDateFormat("'Today' dd MMMM", Locale.getDefault()).format(Date())
    }

    private fun showPasswordReminderAlert() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_password_reminder, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()
        val setPasswordBtn = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.dialogSetPasswordButton)
        val notNowBtn = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.dialogNotNowButton)
        setPasswordBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            dialog.dismiss()
        }
        notNowBtn.setOnClickListener {
            ReminderManager.recordReminderDismissal(this)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun applyTextGradient(textView: TextView) {
        val paint = textView.paint
        val width = paint.measureText(textView.text.toString())
        val color1 = ContextCompat.getColor(this, com.google.android.material.R.color.design_default_color_primary)
        val color2 = ContextCompat.getColor(this, com.google.android.material.R.color.design_default_color_secondary)
        val textShader: Shader = LinearGradient(0f, 0f, width, textView.textSize,
            intArrayOf(color1, color2), null, Shader.TileMode.CLAMP)
        textView.paint.shader = textShader
    }

    private fun signOut() {
        Toast.makeText(this, "Signing out...", Toast.LENGTH_SHORT).show()
        auth.signOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}