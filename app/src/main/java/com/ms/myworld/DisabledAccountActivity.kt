package com.ms.myworld

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ms.myworld.databinding.ActivityDisabledAccountBinding

class DisabledAccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDisabledAccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDisabledAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val message = intent.getStringExtra("EXTRA_MESSAGE")
        binding.warningMessage.text = message ?: "Your account has an issue. Please contact an admin."

        binding.okButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}