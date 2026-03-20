package com.example.togetherapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.togetherapp.data.local.SessionManager
import com.example.togetherapp.databinding.ActivityMainBinding
import com.yandex.mapkit.MapKitFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {

        //  API ключ карты


        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager.getInstance(applicationContext)

        setupNavigation()

        //  Проверка гостя
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Вы вошли как гость", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupNavigation() {

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        val navController = navHostFragment.navController


        binding.bottomNavigation.setupWithNavController(navController)
    }
}