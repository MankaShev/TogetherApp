package com.example.togetherapp.presentation.screens.interests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.togetherapp.R
import com.example.togetherapp.data.local.SessionManager
import com.example.togetherapp.databinding.FragmentInterestsBinding
import com.example.togetherapp.presentation.adapters.InterestsAdapter
import com.example.togetherapp.presentation.viewmodel.InterestsViewModel

class InterestsFragment : Fragment() {

    private var _binding: FragmentInterestsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InterestsViewModel by viewModels()

    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: InterestsAdapter

    private var userId: Int = -1
    private var isEditMode: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInterestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager.getInstance(requireContext())

        userId = sessionManager.getUserId()
        isEditMode = arguments?.getBoolean("is_edit_mode", false) ?: false

        if (userId == -1) {
            Toast.makeText(requireContext(), "Пользователь не найден", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        viewModel.loadInterests(userId, isEditMode)
    }

    private fun setupRecyclerView() {
        adapter = InterestsAdapter(
            items = emptyList(),
            selectedIds = emptySet(),
            onToggle = { interestId ->
                viewModel.toggleInterest(interestId)
            }
        )

        binding.interestsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.interestsRecyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            viewModel.saveInterests(
                userId = userId,
                completeOnboarding = !isEditMode
            )
        }
    }

    private fun observeViewModel() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is InterestsViewModel.InterestsState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                    binding.saveButton.isEnabled = true
                }

                is InterestsViewModel.InterestsState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.saveButton.isEnabled = false
                }

                is InterestsViewModel.InterestsState.Content -> {
                    binding.progressBar.visibility = View.GONE
                    binding.saveButton.isEnabled = true
                    adapter.updateData(state.interests, state.selectedIds)
                }

                is InterestsViewModel.InterestsState.Saving -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.saveButton.isEnabled = false
                }

                is InterestsViewModel.InterestsState.Saved -> {
                    binding.progressBar.visibility = View.GONE
                    binding.saveButton.isEnabled = true

                    Toast.makeText(
                        requireContext(),
                        "Интересы сохранены",
                        Toast.LENGTH_SHORT
                    ).show()

                    if (isEditMode) {
                        findNavController().popBackStack()
                    } else {
                        findNavController().navigate(R.id.exploreFragment)
                    }
                }

                is InterestsViewModel.InterestsState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.saveButton.isEnabled = true

                    Toast.makeText(
                        requireContext(),
                        state.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}