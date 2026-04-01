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
import com.example.togetherapp.R
import com.example.togetherapp.data.local.SessionManager
import com.example.togetherapp.data.repository.CollectionRepositoryImpl
import com.example.togetherapp.data.repository.FriendRepositoryImpl
import com.example.togetherapp.data.repository.PlaceRepositoryImpl
import com.example.togetherapp.databinding.FragmentCollectionsBinding
import com.example.togetherapp.domain.models.CollectionModel
import com.example.togetherapp.presentation.screens.login.LoginActivity
import com.example.togetherapp.presentation.screens.register.RegisterActivity
import com.example.togetherapp.presentation.state.CollectionsUiState
import com.example.togetherapp.presentation.viewmodel.CollectionsViewModel
import com.example.togetherapp.presentation.viewmodel.CollectionsViewModelFactory
import com.example.togetherapp.presentation.viewmodel.SharedMapViewModel

class PersonalCollectionsFragment : Fragment() {

    private var _binding: FragmentCollectionsBinding? = null
    private val binding get() = _binding!!

    private var isAddingMode: Boolean = false
    private lateinit var viewModel: CollectionsViewModel
    private lateinit var adapter: PersonalCollectionsAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var sharedViewModel: SharedMapViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionsBinding.inflate(inflater, container, false)
        sessionManager = SessionManager.getInstance(requireContext().applicationContext)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isAddingMode = arguments?.getBoolean("isAddingMode", false) ?: false
        sharedViewModel = ViewModelProvider(requireActivity())[SharedMapViewModel::class.java]

        initViewModel()
        setupRecyclerView()
        setupListeners()
        setupObservers()
    }

    private fun initViewModel() {
        val placeRepository = PlaceRepositoryImpl()
        val friendRepository = FriendRepositoryImpl()
        val repository = CollectionRepositoryImpl(
            sessionManager = sessionManager,
            placeRepository = placeRepository,
            friendRepository = friendRepository
        )
        val factory = CollectionsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[CollectionsViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = PersonalCollectionsAdapter(
            onCreateClick = {
                findNavController().navigate(R.id.action_collectionsFragment_to_createCollectionFragment)
            },
            onItemClick = { collection ->
                val selectedPlace = sharedViewModel.selectedPlace.value

                if (isAddingMode || selectedPlace != null) {
                    addPlaceToCollection(collection)
                } else {
                    openCollectionDetails(collection)
                }
            }
        )

        binding.rvCollections.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCollections.adapter = adapter
    }

    private fun openCollectionDetails(collection: CollectionModel) {
        val currentUserId = sessionManager.getUserId()
        val currentUserLogin = sessionManager.getUserLogin()

        val bundle = Bundle().apply {
            putInt("collectionId", collection.id)
            putString("collectionTitle", collection.title)
            putString("collectionDescription", collection.description ?: "")
            putString("collectionAccessType", collection.access_type)
            putString("collectionAuthor", currentUserLogin.ifBlank { "Неизвестный автор" })
            putInt("collectionAuthorId", currentUserId)
            putString("collectionDeadlineAt", collection.deadline_at)
            putBoolean("isFromExplore", false)
            putBoolean("forceOwnerMode", true)
        }

        findNavController().navigate(
            R.id.action_collectionsFragment_to_collectionDetailFragment,
            bundle
        )
    }

    private fun addPlaceToCollection(collection: CollectionModel) {
        val place = sharedViewModel.selectedPlace.value
        if (place == null) {
            Toast.makeText(
                requireContext(),
                "Сначала выберите место на карте",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        binding.progressLoading.visibility = View.VISIBLE
        viewModel.addPlaceToCollection(collection.id, collection.title, place)
    }

    private fun setupListeners() {
        binding.btnLoginGuest.setOnClickListener {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        binding.btnRegisterGuest.setOnClickListener {
            val intent = Intent(requireContext(), RegisterActivity::class.java)
            startActivity(intent)
        }

        binding.btnRetry.setOnClickListener {
            viewModel.retry()
        }

        binding.emptyCreateButton.root.setOnClickListener {
            findNavController().navigate(R.id.action_collectionsFragment_to_createCollectionFragment)
        }
    }

    private fun setupObservers() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }

        viewModel.addPlaceResult.observe(viewLifecycleOwner) { result ->
            binding.progressLoading.visibility = View.GONE

            result?.let {
                if (it.isSuccess) {
                    sharedViewModel.onPlaceAddedToCollection(it.collectionName, it.collectionId)
                    sharedViewModel.clearSelectedPlace()
                    findNavController().popBackStack()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Ошибка: ${it.errorMessage ?: "Не удалось добавить место"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                viewModel.consumeAddPlaceResult()
            }
        }
    }

    private fun renderState(state: CollectionsUiState) {
        if (sessionManager.getUserId() == -1) {
            showGuestState()
            return
        }

        hideAllStates()

        when (state) {
            is CollectionsUiState.Loading -> {
                binding.progressLoading.visibility = View.VISIBLE
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

            else -> Unit
        }
    }

    private fun showGuestState() {
        hideAllStates()
        binding.layoutGuest.visibility = View.VISIBLE
    }

    private fun hideAllStates() {
        binding.progressLoading.visibility = View.GONE
        binding.layoutGuest.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
        binding.rvCollections.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}