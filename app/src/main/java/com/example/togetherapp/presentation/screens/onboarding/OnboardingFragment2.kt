package com.example.togetherapp.presentation.screens.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.togetherapp.R
import com.example.togetherapp.data.local.OnboardingManager
import com.example.togetherapp.databinding.FragmentOnboarding2Binding
import com.example.togetherapp.presentation.screens.login.LoginActivity

class OnboardingFragment2 : Fragment() {

    private var _binding: FragmentOnboarding2Binding? = null
    private val binding get() = _binding!!

    private lateinit var onboardingManager: OnboardingManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboarding2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onboardingManager = OnboardingManager.getInstance(requireContext())

        binding.btnSkip2.setOnClickListener {
            completeOnboardingAndOpenLogin()
        }

        binding.btnContinue2.setOnClickListener {
            findNavController().navigate(R.id.action_onboarding2_to_onboarding3)
        }
    }

    private fun completeOnboardingAndOpenLogin() {
        onboardingManager.setOnboardingCompleted(true)

        val intent = Intent(requireContext(), LoginActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}