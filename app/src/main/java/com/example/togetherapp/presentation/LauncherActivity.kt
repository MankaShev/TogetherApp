package com.example.togetherapp.presentation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.togetherapp.MainActivity
import com.example.togetherapp.R
import com.example.togetherapp.data.local.SessionManager
import com.example.togetherapp.presentation.screens.login.LoginActivity

class LauncherActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        sessionManager = SessionManager.getInstance(applicationContext)

        handleLaunch()
    }

    private fun handleLaunch() {
        val shareToken = extractShareToken()

        if (sessionManager.isLoggedIn()) {
            val intentToMain = Intent(this, MainActivity::class.java).apply {
                if (!shareToken.isNullOrBlank()) {
                    putExtra("share_token", shareToken)
                }
            }
            startActivity(intentToMain)
            finish()
        } else {
            val intentToLogin = Intent(this, LoginActivity::class.java).apply {
                if (!shareToken.isNullOrBlank()) {
                    putExtra("share_token", shareToken)
                }
            }
            startActivity(intentToLogin)
            finish()
        }
    }

    private fun extractShareToken(): String? {
        val data = intent?.data ?: return null

        return if (data.scheme == "togetherapp" && data.host == "collection") {
            data.lastPathSegment
        } else {
            null
        }
    }
}