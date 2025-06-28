package com.ms.myworld

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ms.myworld.databinding.ActivityPostLoginBinding

class PostLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val animationResId = intent.getIntExtra("ANIMATION_RESOURCE_ID", 0)

        if (animationResId != 0) {
            binding.postLoginAnimationView.setAnimation(animationResId)
            binding.postLoginAnimationView.playAnimation()
        }

        binding.postLoginAnimationView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator) {
                navigateToMain()
            }
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
    }

    private fun navigateToMain() {
        if (!isFinishing) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}