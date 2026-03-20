package com.example.togetherapp.presentation.screens.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.togetherapp.R
import com.example.togetherapp.databinding.FragmentOnboarding2Binding
import android.content.Intent
import com.example.togetherapp.presentation.screens.login.LoginActivity

class OnboardingFragment2 : Fragment() {

    private var _binding: FragmentOnboarding2Binding? = null
    private val binding get() = _binding!!

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

        binding.btnSkip2.setOnClickListener {
            navigateToLogin()
        }

        binding.btnContinue2.setOnClickListener {
            findNavController().navigate(R.id.action_onboarding2_to_onboarding3)
        }
    }

    private fun navigateToLogin() {
        // Переход на LoginActivity
        val intent = Intent(requireContext(), LoginActivity::class.java)
        startActivity(intent)
        requireActivity().finish()  // Закрываем текущую Activity, чтобы нельзя было вернуться назад
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}