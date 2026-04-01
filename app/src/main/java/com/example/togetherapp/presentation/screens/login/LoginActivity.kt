package com.example.togetherapp.presentation.screens.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.togetherapp.MainActivity
import com.example.togetherapp.data.local.SessionManager
import com.example.togetherapp.data.repository.UserRepositoryImpl
import com.example.togetherapp.databinding.ActivityLoginBinding
import com.example.togetherapp.domain.models.User
import com.example.togetherapp.presentation.screens.register.RegisterActivity
import com.example.togetherapp.presentation.viewmodel.LoginViewModel
import com.example.togetherapp.presentation.viewmodel.LoginViewModelFactory

class LoginActivity : ComponentActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel
    private lateinit var sessionManager: SessionManager

    private var shareToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager.getInstance(applicationContext)
        shareToken = intent.getStringExtra("share_token")

        val repository = UserRepositoryImpl()
        val factory = LoginViewModelFactory(application, repository)
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        setupClickListeners()
        setupObservers()
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            val username = binding.usernameInput.text.toString()
            val password = binding.passwordInput.text.toString()

            if (username.isNotBlank() && password.isNotBlank()) {
                viewModel.login(username, password)
            } else {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }

        binding.continueWithoutLoginBtn.setOnClickListener {
            sessionManager.clearSession()
            Toast.makeText(this, "Продолжаем без входа", Toast.LENGTH_SHORT).show()
            navigateToMain(openInterests = false)
        }

        binding.logRegisterButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.btnRetryInput.setOnClickListener { hideAllStates() }

        binding.btnRetryNetwork.setOnClickListener {
            hideAllStates()
            Toast.makeText(this, "Повтор подключения", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginViewModel.LoginState.Loading -> showLoading()

                is LoginViewModel.LoginState.Success -> {
                    val user: User = state.user
                    sessionManager.saveUser(user)

                    Toast.makeText(this, "Вход выполнен", Toast.LENGTH_SHORT).show()
                    navigateToMain(openInterests = !user.is_onboarding_completed)
                }

                is LoginViewModel.LoginState.Error -> {
                    when (state.errorType) {
                        LoginViewModel.LoginState.ErrorType.INPUT_ERROR -> {
                            showInputError(state.message)
                        }

                        LoginViewModel.LoginState.ErrorType.NETWORK_ERROR -> {
                            showNetworkError()
                        }
                    }
                }
            }
        }
    }

    private fun navigateToMain(openInterests: Boolean = false) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("open_interests", openInterests)

            if (!shareToken.isNullOrBlank()) {
                putExtra("share_token", shareToken)
            }
        }

        startActivity(intent)
        finish()
    }

    private fun showLoading() {
        hideAllStates()
        binding.layoutLoading.visibility = android.view.View.VISIBLE
    }

    private fun showInputError(message: String) {
        hideAllStates()
        binding.layoutErrorInput.visibility = android.view.View.VISIBLE
        binding.loginErrorText.text = message
    }

    private fun showNetworkError() {
        hideAllStates()
        binding.layoutErrorNetwork.visibility = android.view.View.VISIBLE
    }

    private fun hideAllStates() {
        binding.layoutLoading.visibility = android.view.View.GONE
        binding.layoutErrorInput.visibility = android.view.View.GONE
        binding.layoutErrorNetwork.visibility = android.view.View.GONE
    }
}