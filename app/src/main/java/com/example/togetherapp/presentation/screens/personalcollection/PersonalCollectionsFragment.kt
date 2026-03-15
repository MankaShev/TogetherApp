package com.example.togetherapp.presentation.screens.personalcollection

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.togetherapp.LoginActivity
import com.example.togetherapp.databinding.FragmentCollectionsBinding
import com.example.togetherapp.presentation.state.CollectionsUiState
import com.example.togetherapp.presentation.viewmodel.CollectionsViewModel

class PersonalCollectionsFragment : Fragment() {

    private var _binding: FragmentCollectionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CollectionsViewModel by viewModels()
    private lateinit var adapter: PersonalCollectionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        setupObservers()
    }

    private fun setupRecyclerView() {
        adapter = PersonalCollectionsAdapter(
            onCreateClick = {
                Toast.makeText(requireContext(), "Создать подборку", Toast.LENGTH_SHORT).show()
                // Здесь будет переход на экран создания подборки
            },
            onItemClick = { collection ->
                // Исправлено: name -> title
                Toast.makeText(requireContext(), collection.title, Toast.LENGTH_SHORT).show()
                // Здесь будет переход на экран подборки
            }
        )
        binding.rvCollections.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCollections.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        binding.btnRetry.setOnClickListener {
            viewModel.retry()
        }

        // Клик по кнопке "Создать подборку" в пустом состоянии
        binding.emptyCreateButton.root.setOnClickListener {
            Toast.makeText(requireContext(), "Создать подборку", Toast.LENGTH_SHORT).show()
            // Здесь будет переход на экран создания подборки
        }
    }

    private fun setupObservers() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }
    }

    private fun renderState(state: CollectionsUiState) {
        hideAllStates()

        when (state) {
            is CollectionsUiState.Loading -> {
                binding.progressLoading.visibility = View.VISIBLE
            }
            is CollectionsUiState.Unauthorized -> {
                binding.layoutUnauthorized.visibility = View.VISIBLE
            }
            is CollectionsUiState.Empty -> {
                binding.layoutEmpty.visibility = View.VISIBLE
            }
            is CollectionsUiState.Error -> {
                binding.layoutError.visibility = View.VISIBLE
                binding.tvErrorMessage.text = state.message
            }
            is CollectionsUiState.Success -> {
                binding.rvCollections.visibility = View.VISIBLE
                adapter.submitList(state.collections)
            }
        }
    }

    private fun hideAllStates() {
        binding.progressLoading.visibility = View.GONE
        binding.layoutUnauthorized.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
        binding.rvCollections.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}