package com.example.togetherapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.togetherapp.databinding.ActivityLoginBinding
import com.example.togetherapp.presentation.viewmodel.LoginViewModel

class LoginActivity : ComponentActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация ViewModel
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        setupClickListeners()
        setupObservers()
    }

    private fun setupClickListeners() {
        // Кнопка входа
        binding.loginButton.setOnClickListener {
            val username = binding.usernameInput.text.toString()
            val password = binding.passwordInput.text.toString()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                viewModel.login(username, password)
            } else {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }

        // Кнопка "Продолжить без входа"
        binding.continueWithoutLoginBtn.setOnClickListener {
            // НЕ сохраняем авторизацию
            Toast.makeText(this, "Продолжаем без входа", Toast.LENGTH_SHORT).show()
            navigateToMain()
        }

        // Кнопка регистрации - ПЕРЕХОД НА ЭКРАН РЕГИСТРАЦИИ
        binding.logRegisterButton.setOnClickListener {
            // Переход на экран регистрации
            navigateToRegister()
        }

        // Кнопки повтора в состояниях ошибки
        binding.btnRetryInput.setOnClickListener {
            // Возвращаемся к форме входа
            hideAllStates()
        }

        binding.btnRetryNetwork.setOnClickListener {
            // Повторяем попытку (можно показать форму или повторить запрос)
            hideAllStates()
            Toast.makeText(this, "Повтор подключения", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginViewModel.LoginState.Loading -> {
                    showLoading()
                }
                is LoginViewModel.LoginState.Success -> {
                    // Сохраняем авторизацию
                    val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    sharedPref.edit().putBoolean("is_logged_in", true).apply()

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
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToRegister() {
        // Создаем Intent для RegisterActivity
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
        // НЕ закрываем LoginActivity, чтобы можно было вернуться назад
    }

    // Управление состояниями UI
    private fun showLoading() {
        // Скрываем все состояния
        hideAllStates()
        // Показываем загрузку
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