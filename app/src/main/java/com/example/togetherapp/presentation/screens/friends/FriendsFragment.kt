package com.example.togetherapp.presentation.screens.friends

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.togetherapp.R
import com.example.togetherapp.data.repository.FriendRepositoryImpl
import com.example.togetherapp.presentation.viewmodel.FriendsViewModel
import com.example.togetherapp.presentation.viewmodel.FriendsViewModelFactory
import com.example.togetherapp.data.local.SessionManager
import androidx.navigation.fragment.findNavController
class FriendsFragment : Fragment() {

    private val viewModel: FriendsViewModel by viewModels {
        FriendsViewModelFactory(FriendRepositoryImpl())
    }

    private lateinit var btnBack: Button
    private lateinit var etSearchUser: EditText
    private lateinit var btnSearchUser: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvMessage: TextView

    private lateinit var tvSearchEmpty: TextView
    private lateinit var tvIncomingEmpty: TextView
    private lateinit var tvFriendsEmpty: TextView

    private lateinit var rvSearchResults: RecyclerView
    private lateinit var rvIncomingRequests: RecyclerView
    private lateinit var rvFriends: RecyclerView

    private lateinit var searchUsersAdapter: SearchUsersAdapter
    private lateinit var incomingRequestsAdapter: IncomingRequestsAdapter
    private lateinit var friendsAdapter: FriendsAdapter

    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_friends, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager.getInstance(requireContext())

        initViews(view)
        initAdapters()
        initRecyclerViews()
        initListeners()
        observeViewModel()

        val currentUserId = sessionManager.getUserId()
        println("FriendsFragment: currentUserId = $currentUserId")

        if (currentUserId == -1) {
            Toast.makeText(requireContext(), "Не удалось определить пользователя", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.init(currentUserId)
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        etSearchUser = view.findViewById(R.id.etSearchUser)
        btnSearchUser = view.findViewById(R.id.btnSearchUser)
        progressBar = view.findViewById(R.id.progressBar)
        tvMessage = view.findViewById(R.id.tvMessage)

        tvSearchEmpty = view.findViewById(R.id.tvSearchEmpty)
        tvIncomingEmpty = view.findViewById(R.id.tvIncomingEmpty)
        tvFriendsEmpty = view.findViewById(R.id.tvFriendsEmpty)

        rvSearchResults = view.findViewById(R.id.rvSearchResults)
        rvIncomingRequests = view.findViewById(R.id.rvIncomingRequests)
        rvFriends = view.findViewById(R.id.rvFriends)
    }

    private fun initAdapters() {
        searchUsersAdapter = SearchUsersAdapter { user ->
            println("FriendsFragment: send friend request to userId = ${user.id}, login = ${user.login}")
            viewModel.sendFriendRequest(user.id)
        }

        incomingRequestsAdapter = IncomingRequestsAdapter(
            onAcceptClick = { request ->
                println("FriendsFragment: accept requestId = ${request.requestId}")
                viewModel.acceptFriendRequest(request.requestId)
            },
            onDeclineClick = { request ->
                println("FriendsFragment: decline requestId = ${request.requestId}")
                viewModel.declineFriendRequest(request.requestId)
            }
        )

        friendsAdapter = FriendsAdapter { friend ->
            val bundle = Bundle().apply {
                putInt("userId", friend.id)
            }

            findNavController().navigate(
                R.id.action_friendsFragment_to_friendProfileFragment,
                bundle
            )
        }
    }

    private fun initRecyclerViews() {
        rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        rvSearchResults.adapter = searchUsersAdapter

        rvIncomingRequests.layoutManager = LinearLayoutManager(requireContext())
        rvIncomingRequests.adapter = incomingRequestsAdapter

        rvFriends.layoutManager = LinearLayoutManager(requireContext())
        rvFriends.adapter = friendsAdapter
    }

    private fun initListeners() {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnSearchUser.setOnClickListener {
            performSearch()
        }

        etSearchUser.setOnEditorActionListener { _, actionId, event ->
            val isSearchAction = actionId == EditorInfo.IME_ACTION_SEARCH
            val isEnterPressed = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN

            if (isSearchAction || isEnterPressed) {
                performSearch()
                true
            } else {
                false
            }
        }
    }

    private fun performSearch() {
        val query = etSearchUser.text.toString().trim()
        println("FriendsFragment: performSearch query = '$query'")

        viewModel.updateSearchQuery(query)
        viewModel.searchUsers()
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            searchUsersAdapter.submitList(state.searchResults)
            incomingRequestsAdapter.submitList(state.incomingRequests)
            friendsAdapter.submitList(state.friends)

            tvSearchEmpty.visibility = if (state.isSearchEmpty) View.VISIBLE else View.GONE
            tvIncomingEmpty.visibility = if (state.isIncomingEmpty) View.VISIBLE else View.GONE
            tvFriendsEmpty.visibility = if (state.isFriendsEmpty) View.VISIBLE else View.GONE

            if (!state.errorMessage.isNullOrBlank()) {
                tvMessage.visibility = View.VISIBLE
                tvMessage.text = state.errorMessage
                println("FriendsFragment: errorMessage = ${state.errorMessage}")
                viewModel.clearErrorMessage()
            } else {
                tvMessage.visibility = View.GONE
            }

            if (!state.successMessage.isNullOrBlank()) {
                Toast.makeText(requireContext(), state.successMessage, Toast.LENGTH_SHORT).show()
                println("FriendsFragment: successMessage = ${state.successMessage}")
                viewModel.clearSuccessMessage()
            }
        }
    }
}