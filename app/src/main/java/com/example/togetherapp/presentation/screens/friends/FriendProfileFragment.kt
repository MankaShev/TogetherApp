package com.example.togetherapp.presentation.screens.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.togetherapp.R
import com.example.togetherapp.data.local.SessionManager
import com.example.togetherapp.data.repository.CollectionRepositoryImpl
import com.example.togetherapp.data.repository.FriendRepositoryImpl
import com.example.togetherapp.data.repository.PlaceRepositoryImpl
import com.example.togetherapp.data.repository.UserRepositoryImpl
import com.example.togetherapp.presentation.viewmodel.FriendProfileViewModel
import com.example.togetherapp.presentation.viewmodel.FriendProfileViewModelFactory

class FriendProfileFragment : Fragment() {

    private lateinit var viewModel: FriendProfileViewModel
    private lateinit var sessionManager: SessionManager

    private lateinit var btnBack: Button
    private lateinit var ivAvatar: ImageView
    private lateinit var tvLogin: TextView
    private lateinit var tvAccessLabel: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var tvCollectionsEmpty: TextView
    private lateinit var rvCollections: RecyclerView

    private lateinit var collectionsAdapter: FriendProfileCollectionsAdapter

    private var ownerUserId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_friend_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager.getInstance(requireContext())
        ownerUserId = arguments?.getInt("userId", -1) ?: -1

        val friendRepository = FriendRepositoryImpl()
        val placeRepository = PlaceRepositoryImpl()
        val collectionRepository = CollectionRepositoryImpl(
            sessionManager = sessionManager,
            placeRepository = placeRepository,
            friendRepository = friendRepository
        )
        val userRepository = UserRepositoryImpl()

        viewModel = ViewModelProvider(
            this,
            FriendProfileViewModelFactory(
                userRepository = userRepository,
                collectionRepository = collectionRepository,
                friendRepository = friendRepository
            )
        )[FriendProfileViewModel::class.java]

        initViews(view)
        initAdapter()
        initListeners()
        observeViewModel()
        loadData()
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        ivAvatar = view.findViewById(R.id.ivAvatar)
        tvLogin = view.findViewById(R.id.tvLogin)
        tvAccessLabel = view.findViewById(R.id.tvAccessLabel)
        progressBar = view.findViewById(R.id.progressBar)
        tvError = view.findViewById(R.id.tvError)
        tvCollectionsEmpty = view.findViewById(R.id.tvCollectionsEmpty)
        rvCollections = view.findViewById(R.id.rvCollections)
    }

    private fun initAdapter() {
        collectionsAdapter = FriendProfileCollectionsAdapter { collection ->
            val authorLogin = viewModel.uiState.value?.user?.login ?: "Неизвестный автор"

            val bundle = Bundle().apply {
                putInt("collectionId", collection.id)
                putString("collectionTitle", collection.title)
                putString("collectionDescription", collection.description ?: "")
                putString("collectionAuthor", authorLogin)
                putInt("collectionAuthorId", ownerUserId)
                putString("collectionAccessType", collection.access_type)
                putString("collectionDeadlineAt", collection.deadline_at)
                putBoolean("isFromExplore", false)
                putBoolean("forceOwnerMode", false)
            }

            findNavController().navigate(
                R.id.action_friendProfileFragment_to_collectionDetailFragment,
                bundle
            )
        }

        rvCollections.layoutManager = LinearLayoutManager(requireContext())
        rvCollections.adapter = collectionsAdapter
    }

    private fun initListeners() {
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            tvLogin.text = state.user?.login ?: "Пользователь"
            tvAccessLabel.text = state.accessLabel

            collectionsAdapter.submitList(state.collections)
            tvCollectionsEmpty.visibility =
                if (state.isCollectionsEmpty) View.VISIBLE else View.GONE

            if (!state.errorMessage.isNullOrBlank()) {
                tvError.visibility = View.VISIBLE
                tvError.text = state.errorMessage
                viewModel.clearErrorMessage()
            } else {
                tvError.visibility = View.GONE
            }
        }
    }

    private fun loadData() {
        val viewerUserId = sessionManager.getUserId()

        if (ownerUserId == -1) {
            Toast.makeText(requireContext(), "Не передан userId профиля", Toast.LENGTH_SHORT).show()
            return
        }

        if (viewerUserId == -1) {
            Toast.makeText(requireContext(), "Не удалось определить текущего пользователя", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.loadProfile(
            ownerUserId = ownerUserId,
            viewerUserId = viewerUserId
        )
    }
}