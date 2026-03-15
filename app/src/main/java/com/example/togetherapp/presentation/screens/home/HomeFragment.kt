package com.example.togetherapp.presentation.screens.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.togetherapp.databinding.FragmentHomeBinding
import com.example.togetherapp.presentation.state.UiState
import com.example.togetherapp.presentation.viewmodel.HomeViewModel
import com.example.togetherapp.domain.models.Collection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    // Адаптер (нужно будет создать позже)
    private lateinit var adapter: HomeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        setupObservers()

        // Устанавливаем начальное состояние кнопок
        updateTabSelection(binding.btnCollections)
    }

    private fun setupRecyclerView() {
        adapter = HomeAdapter { collection ->
            Toast.makeText(requireContext(), collection.name, Toast.LENGTH_SHORT).show()
        }
        binding.rvPopularCollections.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPopularCollections.adapter = adapter
    }

    private fun setupClickListeners() {
        // Кнопка "Подборки"
        binding.btnCollections.setOnClickListener {
            updateTabSelection(binding.btnCollections)
            viewModel.selectTab(CardType.COLLECTION)
        }

        // Кнопка "Места"
        binding.btnPlaces.setOnClickListener {
            updateTabSelection(binding.btnPlaces)
            viewModel.selectTab(CardType.PLACE)
        }

        // Кнопка повтора при ошибке
        binding.btnRetry.setOnClickListener {
            viewModel.retry()
        }

        // Поиск
        binding.etSearch.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    Toast.makeText(requireContext(), "Поиск: $it", Toast.LENGTH_SHORT).show()
                    viewModel.onSearchQueryChanged(it)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean = true
        })
    }

    private fun setupObservers() {
        viewModel.collectionsState.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }
    }

    private fun renderState(state: UiState<List<Collection>>) {
        hideAllStates()

        when (state) {
            is UiState.Loading -> {
                binding.progressLoading.visibility = View.VISIBLE
            }
            is UiState.Error -> {
                binding.layoutError.visibility = View.VISIBLE
                binding.tvErrorMessage.text = state.message
            }
            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                } else {
                    binding.rvPopularCollections.visibility = View.VISIBLE
                    // adapter.submitList(state.data) // Раскомментировать когда будет адаптер
                }
            }
        }
    }

    private fun updateTabSelection(selectedTab: TextView) {
        binding.btnCollections.isSelected = false
        binding.btnPlaces.isSelected = false
        selectedTab.isSelected = true

        // Обновляем цвета текста
        binding.btnCollections.setTextColor(
            if (selectedTab == binding.btnCollections)
                android.graphics.Color.WHITE
            else
                android.graphics.Color.BLACK
        )
        binding.btnPlaces.setTextColor(
            if (selectedTab == binding.btnPlaces)
                android.graphics.Color.WHITE
            else
                android.graphics.Color.BLACK
        )
    }

    private fun hideAllStates() {
        binding.progressLoading.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE
        binding.rvPopularCollections.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}