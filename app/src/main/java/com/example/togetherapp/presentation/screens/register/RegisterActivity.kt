package com.example.togetherapp.presentation.screens.register

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.togetherapp.MainActivity
import com.example.togetherapp.data.local.SessionManager
import com.example.togetherapp.databinding.ActivityRegisterBinding
import com.example.togetherapp.presentation.viewmodel.RegisterViewModel

class RegisterActivity : ComponentActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var viewModel: RegisterViewModel
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[RegisterViewModel::class.java]
        sessionManager = SessionManager.getInstance(this)

        setupClickListeners()
        setupObservers()
        showFormState()
    }

    private fun setupClickListeners() {
        binding.registerButton.setOnClickListener {
            val login = binding.loginBox.text.toString().trim()
            val password = binding.passwordBox.text.toString().trim()
            val confirmPassword = binding.passwordAgainBox.text.toString().trim()

            if (login.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.register(
                login = login,
                password = password,
                confirmPassword = confirmPassword
            )
        }

        binding.goToLogin.setOnClickListener {
            finish()
        }

        binding.btnRetryInput.setOnClickListener {
            viewModel.resetState()
            showFormState()
        }

        binding.btnRetryNetwork.setOnClickListener {
            viewModel.resetState()
            showFormState()
        }

        binding.btnGoToLogin.setOnClickListener {
            finish()
        }
    }

    private fun setupObservers() {
        viewModel.registerState.observe(this) { state ->
            when (state) {
                is RegisterViewModel.RegisterState.Idle -> {
                    showFormState()
                }

                is RegisterViewModel.RegisterState.Sending -> {
                    showSendingState()
                }

                is RegisterViewModel.RegisterState.Success -> {
                    sessionManager.saveUser(state.user)

                    hideAllStates()

                    Toast.makeText(
                        this,
                        "Регистрация успешна",
                        Toast.LENGTH_LONG
                    ).show()

                    navigateToMain(openInterests = true)
                }

                is RegisterViewModel.RegisterState.ErrorInput -> {
                    showInputError(state.message)
                }

                is RegisterViewModel.RegisterState.ErrorNetwork -> {
                    showNetworkError()
                }

                is RegisterViewModel.RegisterState.ErrorUserExists -> {
                    showUserExistsError(state.message)
                }

                is RegisterViewModel.RegisterState.ErrorUnknown -> {
                    showInputError(state.message)
                }
            }
        }
    }

    private fun navigateToMain(openInterests: Boolean) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("open_interests", openInterests)
        }
        startActivity(intent)
        finish()
    }

    private fun showFormState() {
        hideAllStates()
        binding.registerFormContainer.visibility = View.VISIBLE
    }

    private fun showSendingState() {
        hideAllStates()
        binding.registerFormContainer.visibility = View.GONE
        binding.stateContainer.visibility = View.VISIBLE
        binding.layoutSending.visibility = View.VISIBLE
    }

    private fun showInputError(message: String) {
        hideAllStates()
        binding.registerFormContainer.visibility = View.GONE
        binding.stateContainer.visibility = View.VISIBLE
        binding.layoutErrorInput.visibility = View.VISIBLE
        binding.registerErrorText.text = message
    }

    private fun showNetworkError() {
        hideAllStates()
        binding.registerFormContainer.visibility = View.GONE
        binding.stateContainer.visibility = View.VISIBLE
        binding.layoutErrorNetwork.visibility = View.VISIBLE
    }

    private fun showUserExistsError(message: String) {
        hideAllStates()
        binding.registerFormContainer.visibility = View.GONE
        binding.stateContainer.visibility = View.VISIBLE
        binding.layoutUserExists.visibility = View.VISIBLE
        binding.userExistsText.text = message
    }

    private fun hideAllStates() {
        binding.registerFormContainer.visibility = View.VISIBLE
        binding.stateContainer.visibility = View.GONE

        binding.layoutSending.visibility = View.GONE
        binding.layoutErrorInput.visibility = View.GONE
        binding.layoutErrorNetwork.visibility = View.GONE
        binding.layoutUserExists.visibility = View.GONE
    }
}