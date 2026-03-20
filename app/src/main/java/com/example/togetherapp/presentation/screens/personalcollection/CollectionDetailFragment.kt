package com.example.togetherapp.presentation.screens.personalcollection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.togetherapp.data.local.SessionManager
import com.example.togetherapp.data.repository.CollectionRepositoryImpl
import com.example.togetherapp.data.repository.PlaceRepositoryImpl
import com.example.togetherapp.databinding.FragmentCollectionDetailBinding

class CollectionDetailFragment : Fragment() {

    private var _binding: FragmentCollectionDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CollectionDetailViewModel
    private lateinit var adapter: CollectionPlacesAdapter
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager.getInstance(requireContext().applicationContext)

        val collectionId = arguments?.getInt("collectionId") ?: -1
        val collectionTitle = arguments?.getString("collectionTitle").orEmpty()
        val collectionDescription = arguments?.getString("collectionDescription").orEmpty()
        val collectionAuthor = arguments?.getString("collectionAuthor") ?: "Неизвестный автор"
        val collectionAccessType = arguments?.getString("collectionAccessType").orEmpty()

        binding.tvCollectionTitle.text = collectionTitle
        binding.tvCollectionDescription.text = collectionDescription
        binding.tvCollectionAuthor.text = collectionAuthor
        binding.tvCollectionPrivacy.text = mapAccessType(collectionAccessType)

        initViewModel()
        setupRecyclerView()
        setupObservers()
        setupCloseButton()

        if (collectionId != -1) {
            binding.progressLoading.visibility = View.VISIBLE
            viewModel.loadPlaces(collectionId)
        } else {
            binding.progressLoading.visibility = View.GONE
            Toast.makeText(
                requireContext(),
                "Ошибка: не найден id подборки",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun initViewModel() {
        val placeRepository = PlaceRepositoryImpl()
        val repository = CollectionRepositoryImpl(sessionManager, placeRepository)
        val factory = CollectionDetailViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[CollectionDetailViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = CollectionPlacesAdapter()
        binding.rvPlaces.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlaces.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.places.observe(viewLifecycleOwner) { places ->
            binding.progressLoading.visibility = View.GONE
            adapter.submitList(places)

            if (places.isEmpty()) {
                binding.rvPlaces.visibility = View.GONE
                binding.tvPlacesTitle.visibility = View.GONE
            } else {
                binding.rvPlaces.visibility = View.VISIBLE
                binding.tvPlacesTitle.visibility = View.VISIBLE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            binding.progressLoading.visibility = View.GONE
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupCloseButton() {
        binding.btnClose.setOnClickListener {
            val navController = findNavController()
            val wasPopped = navController.popBackStack()

            if (!wasPopped) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun mapAccessType(accessType: String): String {
        return when (accessType.lowercase()) {
            "private" -> "Приватная"
            "public" -> "Публичная"
            "friends" -> "Для друзей"
            else -> "Не указано"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}