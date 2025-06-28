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
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.ms.myworld.databinding.ActivityLoginBinding
import java.lang.Exception

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

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
                    if (isNewUser) {
                        showSetPasswordDialog()
                    } else {
                        startPostLoginActivity(R.raw.ani_google)
                    }
                } else {
                    handleAuthFailure(task.exception)
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