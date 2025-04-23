package com.mason.cashify_budgettracker

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.mason.cashify_budgettracker.databinding.ActivityAuthBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var db: AppDatabase
    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth and Firestore
        auth = Firebase.auth
        firestore = FirebaseFirestore.getInstance()

        // Initialize Room Database
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "cashify_database"
        ).build()

        setupUI()
    }

    private fun setupUI() {
        updateUIForLogin(isLoginMode)

        binding.primaryButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (validateInput(username, email, password)) {
                binding.progressBar.visibility = View.VISIBLE
                if (isLoginMode) {
                    loginUser(email, password) // Use emailEditText as username in login mode
                } else {
                    registerUser(username, email, password)
                }
            }
        }

        binding.secondaryButton.setOnClickListener {
            isLoginMode = !isLoginMode
            updateUIForLogin(isLoginMode)
            // Clear all input fields when switching modes
            binding.usernameEditText.text?.clear()
            binding.emailEditText.text?.clear()
            binding.passwordEditText.text?.clear()
        }
    }

    private fun updateUIForLogin(loginMode: Boolean) {
        if (loginMode) {
            binding.primaryButton.text = "Login"
            binding.secondaryButton.text = "Create an account"
            binding.usernameInputLayout.visibility = View.GONE
            binding.emailInputLayout.hint = "Username"
            binding.emailEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT
        } else {
            binding.primaryButton.text = "Register"
            binding.secondaryButton.text = "Already have an account?"
            binding.usernameInputLayout.visibility = View.VISIBLE
            binding.emailInputLayout.hint = "Email"
            binding.emailEditText.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
    }

    private fun validateInput(username: String, email: String, password: String): Boolean {
        binding.usernameEditText.error = null
        binding.emailEditText.error = null
        binding.passwordEditText.error = null

        if (isLoginMode) {
            if (TextUtils.isEmpty(email)) { // emailEditText holds username in login mode
                binding.emailEditText.error = "Please enter a username"
                return false
            }
        } else {
            if (TextUtils.isEmpty(username)) {
                binding.usernameEditText.error = "Please enter a username"
                return false
            }
            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailEditText.error = "Please enter a valid email"
                return false
            }
        }

        if (password.length < 6) {
            binding.passwordEditText.error = "Password must be at least 6 characters"
            return false
        }

        return true
    }

    private fun isOnline(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        return network != null
    }

    private fun loginUser(username: String, password: String) {
        CoroutineScope(Dispatchers.Main).launch {
            // Try RoomDB first for offline support
            val user = withContext(Dispatchers.IO) {
                db.userDao().getUserByUsername(username)
            }
            if (user != null) {
                // User found in RoomDB, attempt Firebase login
                auth.signInWithEmailAndPassword(user.email, password)
                    .addOnCompleteListener(this@AuthActivity) { task ->
                        binding.progressBar.visibility = View.GONE
                        if (task.isSuccessful) {
                            startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(
                                this@AuthActivity,
                                "Login failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } else if (isOnline()) {
                // Fall back to Firestore if online
                firestore.collection("users")
                    .whereEqualTo("username", username)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (documents.isEmpty) {
                            binding.progressBar.visibility = View.GONE
                            binding.emailEditText.error = "Username not found"
                            return@addOnSuccessListener
                        }
                        val email = documents.documents[0].getString("email")
                        if (email != null) {
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener(this@AuthActivity) { task ->
                                    binding.progressBar.visibility = View.GONE
                                    if (task.isSuccessful) {
                                        // Save to RoomDB for offline use
                                        CoroutineScope(Dispatchers.IO).launch {
                                            db.userDao().insertUser(User(username, email))
                                        }
                                        startActivity(Intent(this@AuthActivity, MainActivity::class.java))
                                        finish()
                                    } else {
                                        Toast.makeText(
                                            this@AuthActivity,
                                            "Login failed: ${task.exception?.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        } else {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(this@AuthActivity, "Error retrieving email", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@AuthActivity, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@AuthActivity, "Username not found and no internet connection", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerUser(username: String, email: String, password: String) {
        if (!isOnline()) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Registration requires internet connection", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if username is already taken in Firestore
        firestore.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    binding.progressBar.visibility = View.GONE
                    binding.usernameEditText.error = "Username already taken"
                    return@addOnSuccessListener
                }
                // Proceed with registration
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                // Save username and email to Firestore
                                val userData = hashMapOf(
                                    "username" to username,
                                    "email" to email
                                )
                                firestore.collection("users").document(userId)
                                    .set(userData)
                                    .addOnSuccessListener {
                                        // Save to RoomDB
                                        CoroutineScope(Dispatchers.IO).launch {
                                            db.userDao().insertUser(User(username, email))
                                        }
                                        binding.progressBar.visibility = View.GONE
                                        Toast.makeText(
                                            this,
                                            "Registration successful! Please log in.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        // Reset to login mode
                                        isLoginMode = true
                                        updateUIForLogin(true)
                                        binding.usernameEditText.text?.clear()
                                        binding.emailEditText.text?.clear()
                                        binding.passwordEditText.text?.clear()
                                    }
                                    .addOnFailureListener {
                                        binding.progressBar.visibility = View.GONE
                                        Toast.makeText(
                                            this,
                                            "Error saving user data: ${it.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        } else {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(
                                this,
                                "Registration failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error checking username: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}