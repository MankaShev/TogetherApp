package com.example.togetherapp.presentation.screens.home

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity // Важно: используем AppCompatActivity
import com.example.togetherapp.R
import com.example.togetherapp.presentation.screens.map.MapActivity

class HomeActivity : AppCompatActivity() { // Наследуемся от AppCompatActivity

    private lateinit var mapButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_screen)

        mapButton = findViewById(R.id.go_to_map_button)

        mapButton.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }
    }
}