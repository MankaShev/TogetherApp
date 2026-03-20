package com.example.togetherapp.presentation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.togetherapp.MainActivity
import com.example.togetherapp.data.local.OnboardingManager
import com.example.togetherapp.data.local.SessionManager
import com.example.togetherapp.presentation.screens.login.LoginActivity
import com.example.togetherapp.presentation.screens.onboarding.OnboardingActivity

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val onboardingManager = OnboardingManager.getInstance(applicationContext)
        val sessionManager = SessionManager.getInstance(applicationContext)

        val onboardingCompleted = onboardingManager.isOnboardingCompleted()
        val isLoggedIn = try {
            sessionManager.isLoggedIn()
        } catch (e: Exception) {
            false
        }

        val intent = when {
            !onboardingCompleted -> Intent(this, OnboardingActivity::class.java)
            isLoggedIn -> Intent(this, MainActivity::class.java)
            else -> Intent(this, LoginActivity::class.java)
        }

        startActivity(intent)
        finish()
    }
}