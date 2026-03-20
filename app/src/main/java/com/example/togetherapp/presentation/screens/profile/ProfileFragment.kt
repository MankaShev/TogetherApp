package com.example.togetherapp.presentation.screens.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.togetherapp.presentation.screens.login.LoginActivity
import com.example.togetherapp.databinding.FragmentProfileBinding
import com.example.togetherapp.R
import androidx.navigation.fragment.findNavController
//import com.example.togetherapp.presentation.state.ProfileUiState

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        setupListeners()
//        setupObservers()
//    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            // Переход на LoginActivity
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish() // Закрываем MainActivity
        }

//        binding.btnRetry.setOnClickListener {
//            viewModel.retry()
//        }

        // Кнопка выхода
        binding.btnLogout.setOnClickListener {
            // Выход и переход на LoginActivity
            viewModel.logout()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        binding.btnMyCollections.setOnClickListener {
            // Переход на экран коллекций
            findNavController().navigate(R.id.action_profileFragment_to_collectionsFragment)
        }

        binding.btnFriends.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_friendsFragment)
        }

        // Остальные слушатели...
    }

//    private fun setupObservers() {
//        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
//            renderState(state)
//        })
//    }

//    private fun renderState(state: ProfileUiState) {
//        hideAllStates()
//
//        when (state) {
//            is ProfileUiState.Loading -> {
//                binding.progressLoading.visibility = View.VISIBLE
//            }
//            is ProfileUiState.Unauthorized -> {
//                binding.layoutUnauthorized.visibility = View.VISIBLE
//            }
//            is ProfileUiState.Error -> {
//                binding.layoutError.visibility = View.VISIBLE
//                binding.tvErrorMessage.text = state.message
//            }
//            is ProfileUiState.Success -> {
//                binding.layoutProfileHeader.visibility = View.VISIBLE
//                binding.divider.visibility = View.VISIBLE
//                binding.layoutMenu.visibility = View.VISIBLE
//
//                binding.tvUserName.text = state.profileData.userName
//                binding.tvUserEmail.text = state.profileData.userEmail
//            }
//        }
//    }

//    private fun hideAllStates() {
//        binding.progressLoading.visibility = View.GONE
//        binding.layoutUnauthorized.visibility = View.GONE
//        binding.layoutError.visibility = View.GONE
//        binding.layoutProfileHeader.visibility = View.GONE
//        binding.divider.visibility = View.GONE
//        binding.layoutMenu.visibility = View.GONE
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}