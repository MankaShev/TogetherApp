package com.example.togetherapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.togetherapp.data.local.SessionManager
import com.example.togetherapp.data.repository.CollectionRepositoryImpl
import com.example.togetherapp.data.repository.FriendRepositoryImpl
import com.example.togetherapp.data.repository.PlaceRepositoryImpl
import com.example.togetherapp.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager.getInstance(applicationContext)

        setupNavigation()
        checkGuestMode()
        handleOpenInterests()
        handleSharedCollectionLink()
    }

    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                ?: return

        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)
    }

    private fun checkGuestMode() {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Вы вошли как гость", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleOpenInterests() {
        val shouldOpenInterests = intent.getBooleanExtra("open_interests", false)
        if (!shouldOpenInterests) return

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                ?: return

        val navController = navHostFragment.navController

        if (navController.currentDestination?.id != R.id.interestsFragment) {
            val bundle = Bundle().apply {
                putBoolean("is_edit_mode", false)
            }
            navController.navigate(R.id.interestsFragment, bundle)
        }
    }

    private fun handleSharedCollectionLink() {
        val shareToken = intent.getStringExtra("share_token")
        if (shareToken.isNullOrBlank()) return

        val repository = CollectionRepositoryImpl(
            sessionManager = sessionManager,
            placeRepository = PlaceRepositoryImpl(),
            friendRepository = FriendRepositoryImpl()
        )

        lifecycleScope.launch {
            try {
                val collection = repository.getCollectionByShareToken(shareToken)

                if (collection == null) {
                    Toast.makeText(
                        this@MainActivity,
                        "Подборка по ссылке не найдена",
                        Toast.LENGTH_LONG
                    ).show()
                    intent.removeExtra("share_token")
                    return@launch
                }

                val navHostFragment =
                    supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                        ?: return@launch

                val navController = navHostFragment.navController

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

                navController.navigate(R.id.collectionDetailFragment, bundle)
                intent.removeExtra("share_token")
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Ошибка открытия подборки: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                intent.removeExtra("share_token")
            }
        }
    }
}