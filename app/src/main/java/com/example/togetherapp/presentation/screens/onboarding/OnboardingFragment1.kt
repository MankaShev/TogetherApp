package com.example.togetherapp.presentation.screens.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.togetherapp.R
import com.example.togetherapp.databinding.FragmentOnboarding1Binding
import android.content.Intent
import com.example.togetherapp.presentation.screens.login.LoginActivity

class OnboardingFragment1 : Fragment() {

    private var _binding: FragmentOnboarding1Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboarding1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSkip1.setOnClickListener {
            // Переход на LoginActivity
            navigateToLogin()
        }

        binding.btnContinue1.setOnClickListener {
            // Переход на второй экран онбординга
            findNavController().navigate(R.id.action_onboarding1_to_onboarding2)
        }
    }

    private fun navigateToLogin() {
        // TODO: открыть LoginActivity
        val intent = Intent(requireContext(), LoginActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}