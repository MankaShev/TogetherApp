package com.example.togetherapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.togetherapp.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Сначала устанавливаем binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Проверяем авторизацию, но НЕ закрываем MainActivity,
        // потому что есть кнопка "Продолжить без входа"
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)

        // Настраиваем навигацию
        setupNavigation()

        // Если пользователь не авторизован, показываем Toast
        if (!isLoggedIn) {
            android.widget.Toast.makeText(
                this,
                "Вы вошли без авторизации",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupNavigation() {
        val navView: BottomNavigationView = binding.bottomNavigation
        val navController = findNavController(R.id.nav_host_fragment)

        // Связываем BottomNavigation с навигацией
        navView.setupWithNavController(navController)

        // Обработка выбора пунктов меню (опционально)
        navView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    navController.navigate(R.id.homeFragment)
                    true
                }
                R.id.navigation_map -> {
                    navController.navigate(R.id.mapFragment)
                    true
                }
                R.id.navigation_collections -> {
                    navController.navigate(R.id.collectionsFragment)
                    true
                }
//                R.id.navigation_profile -> {
//                    navController.navigate(R.id.profileFragment)
//                    true
//                }
                else -> false
            }
        }
    }
}