package com.ms.myworld

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.ms.myworld.databinding.FragmentChangePasswordBinding

class ChangePasswordBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!
    private var listener: ((Boolean) -> Unit)? = null

    fun setOnPasswordChangeListener(listener: (Boolean) -> Unit) {
        this.listener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.updatePasswordButton.setOnClickListener {
            val oldPassword = binding.oldPasswordEditText.text.toString()
            val newPassword = binding.newPasswordEditText.text.toString()

            if (oldPassword.isEmpty() || newPassword.isEmpty()) {
                Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPassword.length < 6) {
                binding.newPasswordLayout.error = "New password must be at least 6 characters"
                return@setOnClickListener
            }

            val user = Firebase.auth.currentUser
            if (user?.email == null) return@setOnClickListener

            // Re-authenticate the user with their old password for security
            val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)
            user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    // If re-authentication is successful, update to the new password
                    user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) {
                            Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                            listener?.invoke(true)
                            dismiss()
                        } else {
                            Toast.makeText(context, "Error updating password.", Toast.LENGTH_SHORT).show()
                            listener?.invoke(false)
                        }
                    }
                } else {
                    binding.oldPasswordLayout.error = "Old password is not correct"
                    listener?.invoke(false)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}