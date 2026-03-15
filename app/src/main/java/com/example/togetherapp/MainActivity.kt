package com.example.togetherapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity // Используем AppCompatActivity для XML
import com.example.togetherapp.presentation.screens.home.HomeActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_screen)

        val loginButton = findViewById<Button>(R.id.login_button)

        loginButton.setOnClickListener {
            // Временная логика: просто переход на главный экран
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish() // Закрываем экран логина, чтобы нельзя было вернуться назад кнопкой
        }
    }
}