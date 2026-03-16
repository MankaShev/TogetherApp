package com.example.togetherapp.presentation.screens.register

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.togetherapp.databinding.ActivityRegisterBinding
import com.example.togetherapp.presentation.viewmodel.RegisterViewModel

class RegisterActivity : ComponentActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var viewModel: RegisterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[RegisterViewModel::class.java]

        setupClickListeners()
        setupObservers()
    }

    private fun setupClickListeners() {
        // Кнопка регистрации
        binding.registerButton.setOnClickListener {
            val name = binding.nameBox.text.toString()
            val email = binding.emailBox.text.toString()
            val password = binding.passwordBox.text.toString()
            val confirmPassword = binding.passwordAgainBox.text.toString()

            // Базовая валидация
            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                viewModel.register(name, email, password, confirmPassword)
            } else {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }

        // Текст "Уже есть аккаунт? Войти" - возврат на LoginActivity
        binding.goToLogin.setOnClickListener {
            finish() // Просто закрываем RegisterActivity, возвращаемся к LoginActivity
        }

        // Кнопка "Исправить" в состоянии ошибки ввода
        binding.btnRetryInput.setOnClickListener {
            hideAllStates()
            // Возвращаемся к форме
        }

        // Кнопка "Повторить" в состоянии ошибки сети
        binding.btnRetryNetwork.setOnClickListener {
            hideAllStates()
            Toast.makeText(this, "Повтор попытки", Toast.LENGTH_SHORT).show()
        }

        // Кнопка "Перейти ко входу" в состоянии EmailExists
        binding.btnGoToLogin.setOnClickListener {
            finish() // Возвращаемся на LoginActivity
        }
    }

    private fun setupObservers() {
        viewModel.registerState.observe(this) { state ->
            when (state) {
                is RegisterViewModel.RegisterState.Sending -> {
                    showSendingState()
                }
                is RegisterViewModel.RegisterState.Success -> {
                    // Успешная регистрация
                    Toast.makeText(this, "Регистрация успешна! Теперь можно войти", Toast.LENGTH_LONG).show()
                    finish() // Возвращаемся на LoginActivity
                }
                is RegisterViewModel.RegisterState.ErrorInput -> {
                    showInputError(state.message)
                }
                is RegisterViewModel.RegisterState.ErrorNetwork -> {
                    showNetworkError()
                }
                is RegisterViewModel.RegisterState.ErrorEmailExists -> {
                    showEmailExistsError()
                }
            }
        }
    }

    // Управление состояниями UI
    private fun showSendingState() {
        hideAllStates()
        binding.stateContainer.visibility = android.view.View.VISIBLE
        binding.layoutSending.visibility = android.view.View.VISIBLE
    }

    private fun showInputError(message: String) {
        hideAllStates()
        binding.stateContainer.visibility = android.view.View.VISIBLE
        binding.layoutErrorInput.visibility = android.view.View.VISIBLE
        binding.registerErrorText.text = message
    }

    private fun showNetworkError() {
        hideAllStates()
        binding.stateContainer.visibility = android.view.View.VISIBLE
        binding.layoutErrorNetwork.visibility = android.view.View.VISIBLE
    }

    private fun showEmailExistsError() {
        hideAllStates()
        binding.stateContainer.visibility = android.view.View.VISIBLE
        binding.layoutEmailExists.visibility = android.view.View.VISIBLE
    }

    private fun hideAllStates() {
        binding.stateContainer.visibility = android.view.View.GONE
        binding.layoutSending.visibility = android.view.View.GONE
        binding.layoutErrorInput.visibility = android.view.View.GONE
        binding.layoutErrorNetwork.visibility = android.view.View.GONE
        binding.layoutEmailExists.visibility = android.view.View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        // binding очищается автоматически
    }
}