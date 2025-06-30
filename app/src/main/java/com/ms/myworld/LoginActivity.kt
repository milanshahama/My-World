package com.ms.myworld

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ms.myworld.databinding.ActivityLoginBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.util.Locale

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val db = Firebase.firestore

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                val errorCode = e.statusCode
                Log.e("LOGIN_ERROR", "Google Sign-In failed with error code: $errorCode", e)
                Toast.makeText(this, "Google Login Failed. Error Code: $errorCode", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.googleSignInCustomButton.setOnClickListener { signInWithGoogle() }
        binding.loginButton.setOnClickListener { handleManualLogin() }
        binding.forgotPasswordText.setOnClickListener { handleForgotPassword() }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val isNewUser = task.result?.additionalUserInfo?.isNewUser ?: false
                    val user = task.result?.user
                    if (isNewUser && user != null) {
                        createUserProfileInFirestore(user) {
                            showSetPasswordDialog()
                        }
                    } else {
                        // For existing users, check if their profile has the lowercase email field.
                        // If not, we should probably add it for consistency, but for now we'll just log in.
                        user?.let { checkAndFixUserProfile(it) }
                        startPostLoginActivity(R.raw.ani_google)
                    }
                } else {
                    handleAuthFailure(task.exception)
                }
            }
    }

    // --- NEW FUNCTION TO FIX OLD PROFILES ---
    // This function will check if an existing user's profile is missing the new field
    // and add it if necessary.
    private fun checkAndFixUserProfile(user: FirebaseUser) {
        val userDocRef = db.collection("users").document(user.uid)
        userDocRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                if (!document.contains("email_lowercase")) {
                    // The field is missing, so we add it.
                    val email = document.getString("email")
                    if (email != null) {
                        userDocRef.update("email_lowercase", email.toLowerCase(Locale.ROOT))
                            .addOnSuccessListener { Log.d("PROFILE_FIX", "Successfully added lowercase email to existing user.") }
                            .addOnFailureListener { e -> Log.e("PROFILE_FIX_ERROR", "Error updating existing user profile.", e) }
                    }
                }
            }
        }
    }

    private fun showSetPasswordDialog() {
        val setPasswordSheet = SetPasswordBottomSheet()
        setPasswordSheet.setOnPasswordSetListener { _ ->
            startPostLoginActivity(R.raw.ani_google)
        }
        setPasswordSheet.isCancelable = false
        setPasswordSheet.show(supportFragmentManager, "SetPasswordBottomSheet")
    }

    private fun handleManualLogin() {
        val email = binding.usernameEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        if (email.isEmpty()) { binding.usernameLayout.error = "Email is required"; return }
        if (password.isEmpty()) { binding.passwordLayout.error = "Password is required"; return }
        binding.usernameLayout.error = null; binding.passwordLayout.error = null

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    startPostLoginActivity(R.raw.success_anim)
                } else {
                    handleAuthFailure(task.exception)
                }
            }
    }

    private fun createUserProfileInFirestore(user: FirebaseUser, onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val baseId = "ms" + user.uid.take(4).toLowerCase(Locale.ROOT)
                var shortId = baseId
                var isUnique = false
                var attempt = 0

                while (!isUnique) {
                    val query = db.collection("users").whereEqualTo("shortId", shortId).get().await()
                    if (query.isEmpty) {
                        isUnique = true
                    } else {
                        attempt++
                        shortId = "$baseId$attempt"
                    }
                }

                // --- THIS IS THE FIX ---
                // We add a new field, email_lowercase, to save the email in all lowercase.
                val userProfile = hashMapOf(
                    "uid" to user.uid,
                    "email" to user.email,
                    "email_lowercase" to user.email?.toLowerCase(Locale.ROOT), // Save lowercase version for searching
                    "displayName" to user.displayName,
                    "photoUrl" to user.photoUrl.toString(),
                    "shortId" to shortId,
                    "createdAt" to System.currentTimeMillis()
                )
                // --- END OF FIX ---

                db.collection("users").document(user.uid).set(userProfile).await()

                withContext(Dispatchers.Main) {
                    onComplete()
                }

            } catch (e: Exception) {
                Log.e("FIRESTORE_ERROR", "Error creating user profile", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Error setting up profile.", Toast.LENGTH_LONG).show()
                    onComplete()
                }
            }
        }
    }

    private fun handleAuthFailure(exception: Exception?) {
        if (exception is FirebaseAuthInvalidUserException && exception.errorCode == "ERROR_USER_DISABLED") {
            showDisabledAccountScreen()
        } else {
            vibratePhone()
            val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
            binding.usernameLayout.startAnimation(shake); binding.passwordLayout.startAnimation(shake)
            Toast.makeText(this, "Authentication failed. Check your credentials.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showDisabledAccountScreen() {
        val intent = Intent(this, DisabledAccountActivity::class.java).apply {
            putExtra("EXTRA_MESSAGE", "This account has been disabled. Please contact an administrator.")
        }
        startActivity(intent)
    }

    private fun vibratePhone() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        if (vibrator.hasVibrator()) { vibrator.vibrate(200) }
    }

    private fun handleForgotPassword() {
        val email = binding.usernameEditText.text.toString().trim()
        if (email.isEmpty()) { Toast.makeText(this, "Please enter your email to reset password.", Toast.LENGTH_SHORT).show(); return }
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) { Toast.makeText(this, "Password reset email sent.", Toast.LENGTH_LONG).show() }
                else { Toast.makeText(this, "Failed to send reset email.", Toast.LENGTH_SHORT).show() }
            }
    }

    private fun startPostLoginActivity(animationResId: Int) {
        val intent = Intent(this, PostLoginActivity::class.java).apply {
            putExtra("ANIMATION_RESOURCE_ID", animationResId)
        }
        startActivity(intent)
        finish()
    }
}