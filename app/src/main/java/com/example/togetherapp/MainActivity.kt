package com.example.togetherapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.togetherapp.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yandex.mapkit.MapKitFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Устанавливаем API-ключ (только один раз за жизнь процесса)


        super.onCreate(savedInstanceState)

        // Устанавливаем binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Проверяем авторизацию
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)

        // Настройка навигации
        setupNavigation()

        if (!isLoggedIn) {
            Toast.makeText(this, "Вы вошли без авторизации", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupNavigation() {
        val navView: BottomNavigationView = binding.bottomNavigation

        // Получаем NavController из NavHostFragment
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Связываем BottomNavigationView с NavController
        navView.setupWithNavController(navController)

        // Если нужно кастомное поведение при выборе
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
                else -> false
            }
        }
    }
}