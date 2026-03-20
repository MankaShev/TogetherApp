package com.example.togetherapp.presentation.screens.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager.getInstance(applicationContext)
        Log.i(TAG, "SessionManager initialized")

        val repository = UserRepositoryImpl()
        val factory = LoginViewModelFactory(application, repository)
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        setupClickListeners()
        setupObservers()
    }

    private fun setupClickListeners() {

        //  Вход
        binding.loginButton.setOnClickListener {
            val username = binding.usernameInput.text.toString()
            val password = binding.passwordInput.text.toString()

            if (username.isNotBlank() && password.isNotBlank()) {
                Log.i(TAG, "Attempt login with username=$username")
                viewModel.login(username, password)
            } else {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }

        //  ГОСТЕВОЙ ВХОД
        binding.continueWithoutLoginBtn.setOnClickListener {
            Log.i(TAG, "Continue without login clicked")

            // очищаем старую сессию
            sessionManager.clearSession()

            Toast.makeText(this, "Продолжаем без входа", Toast.LENGTH_SHORT).show()

            // Проверка
            Log.i(TAG, "After clearSession userId = ${sessionManager.getUserId()}")

            navigateToMain()
        }

        // 📝 Регистрация
        binding.logRegisterButton.setOnClickListener {
            navigateToRegister()
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

                    Log.i(TAG, "Login successful: $user")

                    // Сохраняем пользователя
                    sessionManager.saveUser(user)

                    Toast.makeText(this, "Вход выполнен", Toast.LENGTH_SHORT).show()
                    navigateToMain()
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

    private fun navigateToMain() {
        Log.i(TAG, "Navigating to MainActivity")
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun navigateToRegister() {
        Log.i(TAG, "Navigating to RegisterActivity")
        startActivity(Intent(this, RegisterActivity::class.java))
    }

    // UI состояния

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