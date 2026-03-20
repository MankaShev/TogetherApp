package com.example.togetherapp.presentation.screens.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.togetherapp.data.local.OnboardingManager
import com.example.togetherapp.databinding.FragmentOnboarding3Binding
import com.example.togetherapp.presentation.screens.login.LoginActivity

class OnboardingFragment3 : Fragment() {

    private var _binding: FragmentOnboarding3Binding? = null
    private val binding get() = _binding!!

    private lateinit var onboardingManager: OnboardingManager

    private val locationPermissionRequestCode = 100

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboarding3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onboardingManager = OnboardingManager.getInstance(requireContext())

        binding.btnAllow.setOnClickListener {
            requestLocationPermission()
        }

        binding.btnAllowThisTime.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Разрешение только на этот раз",
                Toast.LENGTH_SHORT
            ).show()
            completeOnboarding()
        }

        binding.btnDeny.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Разрешение отклонено",
                Toast.LENGTH_SHORT
            ).show()
            completeOnboarding()
        }
    }

    private fun requestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Toast.makeText(
                    requireContext(),
                    "Разрешение уже предоставлено",
                    Toast.LENGTH_SHORT
                ).show()
                completeOnboarding()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                Toast.makeText(
                    requireContext(),
                    "Разрешение нужно для показа мест рядом",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    locationPermissionRequestCode
                )
            }

            else -> {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    locationPermissionRequestCode
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == locationPermissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    requireContext(),
                    "Разрешение получено!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Разрешение не получено",
                    Toast.LENGTH_SHORT
                ).show()
            }

            completeOnboarding()
        }
    }

    private fun completeOnboarding() {
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