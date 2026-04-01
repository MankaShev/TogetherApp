package com.example.togetherapp.presentation.screens.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.togetherapp.R
import com.example.togetherapp.data.local.SessionManager
import com.example.togetherapp.data.repository.CollectionRepositoryImpl
import com.example.togetherapp.data.repository.FriendRepositoryImpl
import com.example.togetherapp.data.repository.PlaceRepositoryImpl
import com.example.togetherapp.databinding.FragmentExploreBinding
import com.example.togetherapp.domain.models.CollectionModel
import com.example.togetherapp.presentation.state.UiState
import com.example.togetherapp.presentation.viewmodel.ExploreViewModel
import com.example.togetherapp.presentation.viewmodel.ExploreViewModelFactory

class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ExploreViewModel
    private lateinit var adapter: HomeAdapter
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager.getInstance(requireContext().applicationContext)

        initViewModel()
        setupRecyclerView()
        setupClickListeners()
        setupObservers()

        viewModel.loadPublicCollections()
    }

    private fun initViewModel() {
        val placeRepository = PlaceRepositoryImpl()
        val friendRepository = FriendRepositoryImpl()
        val repository = CollectionRepositoryImpl(
            sessionManager = sessionManager,
            placeRepository = placeRepository,
            friendRepository = friendRepository
        )
        val factory = ExploreViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ExploreViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = HomeAdapter { collection ->
            openCollectionDetails(collection)
        }

        binding.rvPopularCollections.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPopularCollections.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnRetry.setOnClickListener {
            viewModel.retry()
        }

        binding.etSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.onSearchQueryChanged(query.orEmpty().trim())
                binding.etSearch.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.onSearchQueryChanged(newText.orEmpty().trim())
                return true
            }
        })
    }

    private fun setupObservers() {
        viewModel.collectionsState.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }
    }

    private fun renderState(state: UiState<List<CollectionModel>>) {
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
                    adapter.submitList(state.data)
                }
            }
        }
    }

    private fun hideAllStates() {
        binding.progressLoading.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE
        binding.rvPopularCollections.visibility = View.GONE
    }

    private fun openCollectionDetails(collection: CollectionModel) {
        val bundle = Bundle().apply {
            putInt("collectionId", collection.id)
            putString("collectionTitle", collection.title)
            putString("collectionDescription", collection.description ?: "")
            putString("collectionAuthor", "Пользователь #${collection.user_id}")
            putInt("collectionAuthorId", collection.user_id)
            putString("collectionAccessType", collection.access_type)
            putString("collectionDeadlineAt", collection.deadline_at)
            putBoolean("isFromExplore", true)
            putBoolean("forceOwnerMode", false)
        }

        findNavController().navigate(
            R.id.collectionDetailFragment,
            bundle
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}