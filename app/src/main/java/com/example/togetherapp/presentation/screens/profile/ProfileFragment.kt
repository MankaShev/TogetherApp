package com.example.togetherapp.presentation.screens.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.togetherapp.R
import com.example.togetherapp.databinding.FragmentProfileBinding
import com.example.togetherapp.presentation.screens.login.LoginActivity

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {

        binding.btnLogin.setOnClickListener {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()

            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        binding.btnMyCollections.setOnClickListener {
            findNavController().navigate(R.id.collectionsFragment)
        }

        binding.btnFriends.setOnClickListener {
            findNavController().navigate(R.id.friendsFragment)
        }
    }

    private fun setupObservers() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }
    }

    private fun renderState(state: ProfileViewModel.ProfileState) {
        hideAllStates()

        when (state) {
            is ProfileViewModel.ProfileState.Loading -> {
                // сюда потом можно добавить progress bar
            }

            is ProfileViewModel.ProfileState.Guest -> {
                binding.layoutUnauthorized.visibility = View.VISIBLE
            }

            is ProfileViewModel.ProfileState.Success -> {
                binding.layoutProfileHeader.visibility = View.VISIBLE
                binding.divider.visibility = View.VISIBLE
                binding.layoutMenu.visibility = View.VISIBLE

                val user = state.user
                binding.tvUserName.text = user.login
                binding.tvProfileName.text = user.login
                binding.tvProfileLogin.text = "@${user.login}"

                binding.tvCollectionsCount.text = "0"
                binding.tvFriendsCount.text = "0"
            }

            is ProfileViewModel.ProfileState.Error -> {
                binding.layoutUnauthorized.visibility = View.VISIBLE
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun hideAllStates() {
        binding.layoutUnauthorized.visibility = View.GONE
        binding.layoutProfileHeader.visibility = View.GONE
        binding.divider.visibility = View.GONE
        binding.layoutMenu.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}