package com.example.togetherapp.presentation.screens.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.togetherapp.R
import com.example.togetherapp.databinding.FragmentOnboarding3Binding
import android.content.Intent
import com.example.togetherapp.presentation.screens.login.LoginActivity

class OnboardingFragment3 : Fragment() {

    private var _binding: FragmentOnboarding3Binding? = null
    private val binding get() = _binding!!

    private val LOCATION_PERMISSION_REQUEST_CODE = 100

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

        binding.btnAllow.setOnClickListener {
            requestLocationPermission()
        }

        binding.btnAllowThisTime.setOnClickListener {
            Toast.makeText(requireContext(), "Разрешение только на этот раз", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        }

        binding.btnDeny.setOnClickListener {
            Toast.makeText(requireContext(), "Разрешение отклонено", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        }
    }

    private fun requestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                Toast.makeText(requireContext(), "Разрешение уже предоставлено", Toast.LENGTH_SHORT).show()
                navigateToLogin()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(requireContext(), "Разрешение нужно для показа мест рядом", Toast.LENGTH_LONG).show()
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            }
            else -> {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireContext(), "Разрешение получено!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Разрешение не получено", Toast.LENGTH_SHORT).show()
                }
                navigateToLogin()
            }
        }
    }

    private fun navigateToLogin() {
        // После запроса разрешений переходим на LoginActivity
        val intent = Intent(requireContext(), LoginActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}