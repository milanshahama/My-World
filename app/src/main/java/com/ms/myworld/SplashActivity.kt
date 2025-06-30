package com.ms.myworld

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ms.myworld.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val navigationDelay: Long = 4000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playCinematicSequence()

        Handler(Looper.getMainLooper()).postDelayed({
            navigateToLogin()
        }, navigationDelay)
    }

    private fun playCinematicSequence() {
        // Entrance animations for the logo and text
        binding.logoImageView.alpha = 0f
        binding.logoImageView.scaleX = 0.8f
        binding.logoImageView.scaleY = 0.8f
        binding.sloganTextView.alpha = 0f
        binding.productionTextView.alpha = 0f

        val logoScaleX = ObjectAnimator.ofFloat(binding.logoImageView, View.SCALE_X, 1f)
        val logoScaleY = ObjectAnimator.ofFloat(binding.logoImageView, View.SCALE_Y, 1f)
        val logoFadeIn = ObjectAnimator.ofFloat(binding.logoImageView, View.ALPHA, 1f)

        val logoAnimatorSet = AnimatorSet().apply {
            playTogether(logoScaleX, logoScaleY, logoFadeIn)
            duration = 1500
            interpolator = DecelerateInterpolator()
        }

        val sloganFadeIn = ObjectAnimator.ofFloat(binding.sloganTextView, View.ALPHA, 1f).apply {
            duration = 1000
            startDelay = 500
        }
        val productionFadeIn = ObjectAnimator.ofFloat(binding.productionTextView, View.ALPHA, 1f).apply {
            duration = 1000
            startDelay = 800
        }

        // Start the initial fade-in animations
        logoAnimatorSet.start()
        sloganFadeIn.start()
        productionFadeIn.start()

        // --- THIS IS THE NEW PART ---
        // We will apply the shine animation after the text has faded in.
        val shineDelay = 2000L
        binding.root.postDelayed({
            applyShineAnimation(binding.sloganTextView)
            applyShineAnimation(binding.productionTextView)
        }, shineDelay)
    }

    // --- THIS IS THE NEW, IMPROVED SHINE ANIMATION FUNCTION ---
    private fun applyShineAnimation(textView: TextView) {
        // Ensure the view has been measured before getting its width
        if (textView.width == 0) {
            textView.post { applyShineAnimation(textView) }
            return
        }

        val textViewWidth = textView.paint.measureText(textView.text.toString())
        val currentTextColor = textView.currentTextColor

        // A brighter, more visible shine color
        val highlightColor = Color.argb(255, 240, 240, 240)

        val animator = ValueAnimator.ofFloat(0f, 2 * textViewWidth)
        animator.duration = 1200 // A slightly longer, more elegant wipe
        animator.interpolator = DecelerateInterpolator()

        animator.addUpdateListener { valueAnimator ->
            val shimmerPosition = valueAnimator.animatedValue as Float

            // This new gradient is sharper and more visible on all backgrounds
            val gradient = LinearGradient(
                shimmerPosition - textViewWidth, 0f,
                shimmerPosition, 0f,
                intArrayOf(currentTextColor, highlightColor, currentTextColor),
                floatArrayOf(0.4f, 0.5f, 0.6f), // Creates a sharp "gleam"
                Shader.TileMode.CLAMP
            )
            textView.paint.shader = gradient
            textView.invalidate()
        }

        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                // Reset the shader to null to remove the gradient and restore the original text color
                textView.paint.shader = null
                textView.setTextColor(currentTextColor)
                textView.invalidate()
            }
        })

        animator.start()
    }

    private fun navigateToLogin() {
        if (!isFinishing) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}