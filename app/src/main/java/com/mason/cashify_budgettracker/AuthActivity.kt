
package com.mason.cashify_budgettracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.mason.cashify_budgettracker.data.User
import com.mason.cashify_budgettracker.data.UserRepository
import com.mason.cashify_budgettracker.databinding.ActivityAuthBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth

    /*
          -------------------------------------------------------------------------
          Title: Add Firebase to your Android project
          Author: Google Developers
          Date Published: 2023
          Date Accessed: 2 May 2025
          Code Version: 11.0.0.
          Availability: https://firebase.google.com/docs/android/setup
          -------------------------------------------------------------------------
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //inflate the layout using View Binding
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //initialize FirebaseAuth instance
        auth = FirebaseAuth.getInstance()

        //toggle between Login and Signup layouts
        binding.toggleButton.setOnClickListener {
            if (binding.loginLayout.visibility == android.view.View.VISIBLE) {
                binding.loginLayout.visibility = android.view.View.GONE
                binding.signupLayout.visibility = android.view.View.VISIBLE
                binding.toggleButton.text = "Switch to Login"
            } else {
                binding.loginLayout.visibility = android.view.View.VISIBLE
                binding.signupLayout.visibility = android.view.View.GONE
                binding.toggleButton.text = "Switch to Signup"
            }
        }

        //handle login button click
        binding.btnLogin.setOnClickListener {
            val email = binding.loginUsername.text.toString().trim()
            val password = binding.loginPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            loginUser(email, password)
        }

        //handle signup button click
        binding.btnSignup.setOnClickListener {
            val username = binding.signupUsername.text.toString().trim()
            val email = binding.signupEmail.text.toString().trim()
            val password = binding.signupPassword.text.toString().trim()
            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            signupUser(email, password, username)
        }
    }

    //function to log in user using FirebaseAuth
    private fun loginUser(email: String, password: String) {
        Log.d("AuthActivity", "Attempting login with email: $email")
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    //get current user ID after successful login
                    val userId = auth.currentUser?.uid ?: run {
                        Toast.makeText(this, "Failed to get user ID", Toast.LENGTH_SHORT).show()
                        Log.e("AuthActivity", "No user ID after login")
                        return@addOnCompleteListener
                    }
                    Log.d("AuthActivity", "Login successful for email: $email, id: $userId")
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    Log.e("AuthActivity", "Login failed", task.exception)
                }
            }
    }

    //function to sign up new user with FirebaseAuth and Firestore
    private fun signupUser(email: String, password: String, username: String) {
        Log.d("AuthActivity", "Attempting signup with email: $email, username: $username")
        lifecycleScope.launch {
            try {
                //check if username already exists in the repository
                val existingUser = withContext(Dispatchers.IO) {
                    UserRepository.getUserByUsername(username)
                }
                if (existingUser != null) {
                    Toast.makeText(this@AuthActivity, "Username already taken", Toast.LENGTH_SHORT).show()
                    Log.w("AuthActivity", "Signup failed: username already exists: $username")
                    return@launch
                }

                //create user with FirebaseAuth
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this@AuthActivity) { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid ?: run {
                                Toast.makeText(this@AuthActivity, "Failed to get user ID", Toast.LENGTH_SHORT).show()
                                Log.e("AuthActivity", "No user ID after signup")
                                return@addOnCompleteListener
                            }

                            lifecycleScope.launch {
                                try {
                                    val profileUpdates = UserProfileChangeRequest.Builder()
                                        .setDisplayName(username)
                                        .build()
                                    auth.currentUser?.updateProfile(profileUpdates)?.await()

                                    val newUser = User(id = userId, username = username, email = email)
                                    withContext(Dispatchers.IO) {
                                        UserRepository.insert(newUser)
                                    }
                                    Log.d("AuthActivity", "User signed up and saved to Firestore: $username, id: $userId")
                                    Toast.makeText(this@AuthActivity, "Signup successful, please login", Toast.LENGTH_SHORT).show()

                                    //switch to login layout after signup
                                    binding.loginLayout.visibility = android.view.View.VISIBLE
                                    binding.signupLayout.visibility = android.view.View.GONE
                                    binding.toggleButton.text = "Switch to Signup"
                                    binding.loginUsername.setText(email)
                                    binding.loginPassword.text?.clear()
                                    auth.signOut()
                                } catch (e: Exception) {
                                    Toast.makeText(this@AuthActivity, "Error saving user data", Toast.LENGTH_SHORT).show()
                                    Log.e("AuthActivity", "Error saving user to Firestore: $username", e)
                                    auth.signOut()
                                }
                            }
                        } else {
                            Toast.makeText(this@AuthActivity, "Signup failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            Log.e("AuthActivity", "Signup failed", task.exception)
                        }
                    }
            } catch (e: Exception) {
                Toast.makeText(this@AuthActivity, "Error checking username", Toast.LENGTH_SHORT).show()
                Log.e("AuthActivity", "Error querying username: $username", e)
            }
        }
    }
}