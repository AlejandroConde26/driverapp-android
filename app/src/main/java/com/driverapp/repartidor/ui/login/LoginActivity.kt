package com.driverapp.repartidor.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.driverapp.repartidor.databinding.ActivityLoginBinding
import com.driverapp.repartidor.ui.main.MainActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val vm: LoginViewModel by viewModels()
    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.eyeToggle.setOnClickListener {
            passwordVisible = !passwordVisible
            binding.passwordInput.inputType = if (passwordVisible) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            binding.passwordInput.setSelection(binding.passwordInput.text?.length ?: 0)
        }

        binding.forgotPassword.setOnClickListener {
            binding.errorText.isVisible = true
            binding.errorText.text = "Contacta al administrador para restablecer tu contraseña"
        }

        binding.loginButton.setOnClickListener {
            vm.login(
                binding.emailInput.text?.toString() ?: "",
                binding.passwordInput.text?.toString() ?: ""
            )
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { state ->
                    when (state) {
                        LoginState.Idle -> Unit
                        LoginState.Loading -> {
                            binding.loading.isVisible = true
                            binding.loginButton.isEnabled = false
                            binding.errorText.isVisible = false
                        }
                        is LoginState.Error -> {
                            binding.loading.isVisible = false
                            binding.loginButton.isEnabled = true
                            binding.errorText.isVisible = true
                            binding.errorText.text = state.message
                            Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_LONG).show()
                        }
                        is LoginState.Success -> {
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        }
                    }
                }
            }
        }
    }
}