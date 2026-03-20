package com.example.togetherapp.presentation.screens.personalcollection

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.togetherapp.presentation.screens.login.LoginActivity
import com.example.togetherapp.data.local.SessionManager
import com.example.togetherapp.data.repository.CollectionRepositoryImpl
import com.example.togetherapp.databinding.FragmentCollectionsBinding
import com.example.togetherapp.presentation.state.CollectionsUiState
import com.example.togetherapp.presentation.viewmodel.CollectionsViewModel
import com.example.togetherapp.presentation.viewmodel.CollectionsViewModelFactory

class PersonalCollectionsFragment : Fragment() {

    private var _binding: FragmentCollectionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CollectionsViewModel
    private lateinit var adapter: PersonalCollectionsAdapter

    // Singleton SessionManager
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionsBinding.inflate(inflater, container, false)
        // Используем singleton SessionManager
        sessionManager = SessionManager.getInstance(requireContext().applicationContext)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModel()
        setupRecyclerView()
        setupListeners()
        setupObservers()
    }

    /** Инициализация ViewModel */
    private fun initViewModel() {
        val repository = CollectionRepositoryImpl(sessionManager)
        val factory = CollectionsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[CollectionsViewModel::class.java]
    }

    /** Настройка RecyclerView */
    private fun setupRecyclerView() {
        adapter = PersonalCollectionsAdapter(
            onCreateClick = {
                // Переход на экран создания подборки
                findNavController().navigate(
                    com.example.togetherapp.R.id.action_collectionsFragment_to_createCollectionFragment
                )
            },
            onItemClick = { collection ->
                Toast.makeText(requireContext(), collection.title, Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvCollections.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCollections.adapter = adapter
    }

    /** Обработчики кнопок */
    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        binding.btnRetry.setOnClickListener {
            viewModel.retry()
        }

        binding.emptyCreateButton.root.setOnClickListener {
            findNavController().navigate(
                com.example.togetherapp.R.id.action_collectionsFragment_to_createCollectionFragment
            )
        }
    }

    /** Подписка на LiveData */
    private fun setupObservers() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }
    }

    /** Отображение состояния UI */
    private fun renderState(state: CollectionsUiState) {
        hideAllStates()
        when (state) {
            is CollectionsUiState.Loading -> binding.progressLoading.visibility = View.VISIBLE
            is CollectionsUiState.Unauthorized -> binding.layoutUnauthorized.visibility = View.VISIBLE
            is CollectionsUiState.Empty -> binding.layoutEmpty.visibility = View.VISIBLE
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

    /** Скрытие всех состояний */
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