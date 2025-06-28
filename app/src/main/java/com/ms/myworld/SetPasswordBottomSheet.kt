package com.ms.myworld

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.ms.myworld.databinding.FragmentSetPasswordBinding

class SetPasswordBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentSetPasswordBinding? = null
    private val binding get() = _binding!!
    private var listener: ((Boolean) -> Unit)? = null

    fun setOnPasswordSetListener(listener: (Boolean) -> Unit) {
        this.listener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSetPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.maybeLaterButton.setOnClickListener {
            listener?.invoke(false)
            dismiss()
        }
        binding.setPasswordButton.setOnClickListener {
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()
            if (password.length < 6) {
                binding.passwordLayout.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                binding.confirmPasswordLayout.error = "Passwords do not match"
                return@setOnClickListener
            }
            binding.passwordLayout.error = null
            binding.confirmPasswordLayout.error = null
            Firebase.auth.currentUser?.updatePassword(password)?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Password set successfully!", Toast.LENGTH_SHORT).show()
                    listener?.invoke(true)
                } else {
                    Toast.makeText(context, "Failed to set password.", Toast.LENGTH_SHORT).show()
                    listener?.invoke(false)
                }
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}