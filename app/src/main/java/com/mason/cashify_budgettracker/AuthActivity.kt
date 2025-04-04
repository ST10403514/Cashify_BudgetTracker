package com.mason.cashify_budgettracker

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mason.cashify_budgettracker.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth
    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = Firebase.auth

        setupUI()
    }

    private fun setupUI() {
        updateUIForLogin(isLoginMode)

        binding.primaryButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (validateInput(email, password)) {
                binding.progressBar.visibility = View.VISIBLE
                if (isLoginMode) {
                    loginUser(email, password)
                } else {
                    registerUser(email, password)
                }
            }
        }

        binding.secondaryButton.setOnClickListener {
            isLoginMode = !isLoginMode
            updateUIForLogin(isLoginMode)
        }
    }

    private fun updateUIForLogin(loginMode: Boolean) {
        if (loginMode) {
            binding.primaryButton.text = "Login"
            binding.secondaryButton.text = "Create an account"
        } else {
            binding.primaryButton.text = "Register"
            binding.secondaryButton.text = "Already have an account?"
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        binding.emailEditText.error = null
        binding.passwordEditText.error = null

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.error = "Please enter a valid email"
            return false
        }

        if (password.length < 6) {
            binding.passwordEditText.error = "Password must be at least 6 characters"
            return false
        }

        return true
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    // Login success - go to MainActivity
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Login failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Registration successful!",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Automatically log in after registration
                    loginUser(email, password)
                } else {
                    Toast.makeText(
                        this,
                        "Registration failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    override fun onStart() {
        super.onStart()
        // If user is already logged in, go straight to MainActivity
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}