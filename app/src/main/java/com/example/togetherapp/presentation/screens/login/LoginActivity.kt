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

        // Используем singleton SessionManager
        sessionManager = SessionManager.getInstance(applicationContext)
        Log.i(TAG, "SessionManager initialized (singleton)")

        // Создаём репозиторий и ViewModel
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
                Log.i(TAG, "Attempt login with username=$username")
                viewModel.login(username, password)
            } else {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }

        binding.continueWithoutLoginBtn.setOnClickListener {
            Toast.makeText(this, "Продолжаем без входа", Toast.LENGTH_SHORT).show()
            navigateToMain()
        }

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
                    Log.i(TAG, "Login successful, user: $user")

                    // Сохраняем пользователя через singleton SessionManager
                    sessionManager.saveUser(user)
                    Log.i(TAG, "User saved in SessionManager: id=${user.id}")

                    Toast.makeText(this, "Вход выполнен", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }

                is LoginViewModel.LoginState.Error -> {
                    when (state.errorType) {
                        LoginViewModel.LoginState.ErrorType.INPUT_ERROR -> {
                            Log.w(TAG, "Input error: ${state.message}")
                            showInputError(state.message)
                        }
                        LoginViewModel.LoginState.ErrorType.NETWORK_ERROR -> {
                            Log.w(TAG, "Network error: ${state.message}")
                            showNetworkError()
                        }
                    }
                }
            }
        }
    }

    private fun navigateToMain() {
        Log.i(TAG, "Navigating to MainActivity")
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToRegister() {
        Log.i(TAG, "Navigating to RegisterActivity")
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
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