package com.ms.myworld

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ms.myworld.databinding.FragmentProfileMenuBinding

class ProfileMenuBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentProfileMenuBinding? = null
    private val binding get() = _binding!!
    private var mListener: BottomSheetListener? = null

    interface BottomSheetListener {
        fun onOptionSelected(option: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = context as? BottomSheetListener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editProfileOption.setOnClickListener {
            mListener?.onOptionSelected("edit_profile")
            dismiss()
        }

        binding.settingsOption.setOnClickListener {
            mListener?.onOptionSelected("settings")
            dismiss()
        }

        binding.logoutOption.setOnClickListener {
            mListener?.onOptionSelected("logout")
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}