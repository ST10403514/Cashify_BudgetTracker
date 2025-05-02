package com.mason.cashify_budgettracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.auth.FirebaseAuth
import com.mason.cashify_budgettracker.data.AppDatabase
import com.mason.cashify_budgettracker.data.Category
import com.mason.cashify_budgettracker.data.Goal
import com.mason.cashify_budgettracker.databinding.ActivityAddGoalBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddGoalActivity : AppCompatActivity() {

    //Declare UI binding, Firebase auth, and local database
    private lateinit var binding: ActivityAddGoalBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: AppDatabase

    //Variables for storing photo path and category data
    private var photoPath: String? = null
    private val categories = mutableListOf<String>()
    private val defaultCategories = listOf("Food", "Transport", "Entertainment", "Bills", "Other")

    //Handle permission request result for external storage access
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            Log.d("AddGoalActivity", "Storage permission granted, launching gallery")
            launchGallery()
        } else {
            Log.w("AddGoalActivity", "Storage permission denied")
            Toast.makeText(this, "Storage permission is required to select photos", Toast.LENGTH_LONG).show()
        }
    }

    //Handle result from image picker (gallery)
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val photoFile = createImageFile()
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(photoFile).use { output ->
                        input.copyTo(output)
                    }
                }
                photoPath = photoFile.absolutePath
                binding.ivPhoto.setImageURI(photoFile.toUri())
                binding.ivPhoto.visibility = View.VISIBLE
                Log.d("AddGoalActivity", "Photo selected from gallery: $photoPath")
            } catch (e: Exception) {
                Log.e("AddGoalActivity", "Error processing gallery image: $e")
                Toast.makeText(this, "Error selecting photo", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d("AddGoalActivity", "Gallery selection cancelled")
            Toast.makeText(this, "Photo selection cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Initialize view binding and layout
        try {
            binding = ActivityAddGoalBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("AddGoalActivity", "onCreate: Binding and setContentView successful")
        } catch (e: Exception) {
            Log.e("AddGoalActivity", "Error in onCreate: $e")
            Toast.makeText(this, "Error loading Add Goal page", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        //Initialize Firebase authentication
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Log.w("AddGoalActivity", "No user logged in, redirecting to AuthActivity")
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        //Handle back button click
        binding.btnBack.setOnClickListener {
            Log.d("AddGoalActivity", "Back button clicked")
            finish()
        }

        //Restore saved photo if configuration changes
        if (savedInstanceState != null) {
            photoPath = savedInstanceState.getString("photoPath")
            if (photoPath != null) {
                binding.ivPhoto.setImageURI(File(photoPath!!).toUri())
                binding.ivPhoto.visibility = View.VISIBLE
                Log.d("AddGoalActivity", "Restored photoPath: $photoPath")
            }
        }

        //Load database and categories asynchronously
        lifecycleScope.launch {
            try {
                database = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(this@AddGoalActivity)
                }
                loadCategories()
                Log.d("AddGoalActivity", "Database initialized and categories loaded")
            } catch (e: Exception) {
                Log.e("AddGoalActivity", "Error initializing database: $e")
                Toast.makeText(this@AddGoalActivity, "Error accessing database", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        //Set up category dropdown menu with adapter
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        (binding.categoryInput as MaterialAutoCompleteTextView).setAdapter(categoryAdapter)
        binding.categoryInput.setOnClickListener {
            binding.categoryInput.showDropDown()
        }

        //Open month picker dialog when user clicks month field
        binding.monthInput.setOnClickListener {
            Log.d("AddGoalActivity", "Month input clicked")
            showMonthPicker()
        }

        //Open photo selection when user clicks "Add Photo"
        binding.btnCapturePhoto.setOnClickListener {
            Log.d("AddGoalActivity", "Add Photo clicked")
            checkPermissionsAndSelectPhoto()
        }

        //Open dialog to add a new custom category
        binding.addCategoryText.setOnClickListener {
            Log.d("AddGoalActivity", "Add Category clicked")
            showAddCategoryDialog()
        }

        //Save goal data to database on save button click
        binding.btnSave.setOnClickListener {
            Log.d("AddGoalActivity", "Save clicked")
            saveGoal()
        }

        //Handle bottom navigation menu selection
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Log.d("AddGoalActivity", "Navigating to MainActivity")
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_categories -> {
                    Log.d("AddGoalActivity", "Navigating to CategoriesActivity")
                    startActivity(Intent(this, CategoriesActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_goals -> {
                    Log.d("AddGoalActivity", "Goals tab selected")
                    true
                }
                else -> false
            }
        }
        binding.bottomNav.menu.findItem(R.id.nav_goals)?.isChecked = true
    }

    //Save instance state when needed
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("photoPath", photoPath)
        Log.d("AddGoalActivity", "onSaveInstanceState: Saved photoPath")
    }

    //Show Material date picker dialog for selecting month
    private fun showMonthPicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Goal Month")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        //handle date selection and format month/year
        datePicker.addOnPositiveButtonClickListener { selection ->
            try {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = selection
                val sdf = SimpleDateFormat("MM/yyyy", Locale.getDefault())
                val monthYear = sdf.format(calendar.time)
                binding.monthInput.setText(monthYear)
                Log.d("AddGoalActivity", "Month selected: $monthYear")
            } catch (e: Exception) {
                Log.e("AddGoalActivity", "Error selecting month: $e")
                Toast.makeText(this, "Error selecting month", Toast.LENGTH_SHORT).show()
            }
        }

        //Log cancel action
        datePicker.addOnNegativeButtonClickListener {
            Log.d("AddGoalActivity", "Month picker cancelled")
        }

        //Display picker dialog
        datePicker.show(supportFragmentManager, "MONTH_PICKER")
    }

    //Checks for storage permission and proceeds to launch gallery or request permission
    private fun checkPermissionsAndSelectPhoto() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        when {
            //If permission is already granted, launch gallery
            checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED -> {
                Log.d("AddGoalActivity", "Storage permission already granted, launching gallery")
                launchGallery()
            }
            //If rationale should be shown, show explanation dialog
            shouldShowRequestPermissionRationale(permission) -> {
                Log.d("AddGoalActivity", "Showing permission rationale for storage")
                showPermissionRationale()
            }
            //Otherwise, directly request permission
            else -> {
                Log.d("AddGoalActivity", "Requesting storage permission")
                permissionLauncher.launch(permission)
            }
        }
    }

    //Displays rationale dialog to user for needing storage permission
    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Storage Permission Required")
            .setMessage("This app needs storage access to select photos for your goals. Please grant the permission.")
            .setPositiveButton("OK") { _, _ ->
                Log.d("AddGoalActivity", "User acknowledged rationale, requesting storage permission")
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                permissionLauncher.launch(permission)
            }
            .setNegativeButton("Cancel") { _, _ ->
                Log.w("AddGoalActivity", "User cancelled permission rationale")
                Toast.makeText(this, "Storage permission is required to select photos", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    //Launches image gallery for photo selection
    private fun launchGallery() {
        galleryLauncher.launch("image/*")
        Log.d("AddGoalActivity", "Gallery launched")
    }

    //Loads user-specific and default categories into category dropdown
    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val dbCategories = withContext(Dispatchers.IO) {
                    database.categoryDao().getCategories(userId)
                }
                categories.clear()
                categories.addAll(defaultCategories)
                categories.addAll(dbCategories.map { it.name }.filter { !defaultCategories.contains(it) })
                categories.sort()
                val categoryAdapter = ArrayAdapter(this@AddGoalActivity, android.R.layout.simple_dropdown_item_1line, categories)
                (binding.categoryInput as MaterialAutoCompleteTextView).setAdapter(categoryAdapter)
                Log.d("AddGoalActivity", "Categories loaded: $categories")
            } catch (e: Exception) {
                Log.e("AddGoalActivity", "Error loading categories: $e")
                Toast.makeText(this@AddGoalActivity, "Error loading categories", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Displays dialog to add new custom category
    private fun showAddCategoryDialog() {
        try {
            val builder = AlertDialog.Builder(this)
            val dialogBinding = layoutInflater.inflate(R.layout.dialog_add_category, null)
            val categoryInput = dialogBinding.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.categoryInput)
            builder.setView(dialogBinding)
                .setTitle("Add Category")
                .setPositiveButton("Add") { _, _ ->
                    val categoryName = categoryInput?.text?.toString()?.trim() ?: ""
                    //If category is valid and not duplicate, insert into database
                    if (categoryName.isNotEmpty() && !categories.contains(categoryName)) {
                        lifecycleScope.launch {
                            try {
                                val userId = auth.currentUser?.uid ?: return@launch
                                withContext(Dispatchers.IO) {
                                    database.categoryDao().insert(Category(userId = userId, name = categoryName))
                                }
                                loadCategories()
                                Toast.makeText(this@AddGoalActivity, "Category added", Toast.LENGTH_SHORT).show()
                                Log.d("AddGoalActivity", "Category added: $categoryName")
                            } catch (e: Exception) {
                                Log.e("AddGoalActivity", "Error adding category: $e")
                                Toast.makeText(this@AddGoalActivity, "Error adding category", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Invalid or duplicate category", Toast.LENGTH_SHORT).show()
                        Log.w("AddGoalActivity", "Invalid category: $categoryName")
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
            Log.d("AddGoalActivity", "Add Category dialog shown")
        } catch (e: Exception) {
            Log.e("AddGoalActivity", "Error showing add category dialog: $e")
            Toast.makeText(this, "Error showing category dialog", Toast.LENGTH_SHORT).show()
        }
    }

    /*
        -------------------------------------------------------------------------
        Title: Save data in a local database using Room
        Author: Android Developers
        Date Published: 2023
        Date Accessed: 22 April 2025
        Code Version: 2.6.0.
        Availability: https://developer.android.com/training/data-storage/room
        -------------------------------------------------------------------------
     */

    //Validates and saves goal with provided input data
    private fun saveGoal() {
        val month = binding.monthInput.text.toString().trim()
        val category = binding.categoryInput.text.toString().trim()
        val type = if (binding.radioIncome.isChecked) "income" else "expense"
        val description = binding.descriptionInput.text.toString().trim()
        val minGoalStr = binding.minGoalInput.text.toString().trim()
        val maxGoalStr = binding.maxGoalInput.text.toString().trim()
        val userId = auth.currentUser?.uid ?: return

        //Validate required input fields
        if (month.isEmpty() || category.isEmpty() || minGoalStr.isEmpty() || maxGoalStr.isEmpty()) {
            Toast.makeText(this, "Please fill month, category, min goal, and max goal", Toast.LENGTH_SHORT).show()
            Log.w("AddGoalActivity", "Validation failed: Empty fields")
            return
        }

        val minGoal = minGoalStr.toDoubleOrNull()
        val maxGoal = maxGoalStr.toDoubleOrNull()

        //Validate numeric conversion
        if (minGoal == null || maxGoal == null) {
            Toast.makeText(this, "Invalid min or max goal", Toast.LENGTH_SHORT).show()
            Log.w("AddGoalActivity", "Validation failed: Invalid min/max goal")
            return
        }

        //Validate logical range
        if (minGoal >= maxGoal) {
            Toast.makeText(this, "Minimum goal must be less than maximum goal", Toast.LENGTH_SHORT).show()
            Log.w("AddGoalActivity", "Validation failed: minGoal >= maxGoal")
            return
        }

        lifecycleScope.launch {
            try {
                //Resolve category ID or fallback to name
                val categoryId = withContext(Dispatchers.IO) {
                    database.categoryDao().getCategories(userId)
                        .find { it.name == category }?.id?.toString() ?: category
                }

                //Construct Goal object and insert into DB
                val goal = Goal(
                    userId = userId,
                    month = month,
                    category = category,
                    categoryId = categoryId,
                    type = type,
                    description = description,
                    photoPath = photoPath ?: "",
                    minGoal = minGoal,
                    maxGoal = maxGoal,
                    createdAt = System.currentTimeMillis()
                )
                withContext(Dispatchers.IO) {
                    database.goalDao().insert(goal)
                }
                Toast.makeText(this@AddGoalActivity, "Goal saved", Toast.LENGTH_SHORT).show()
                Log.d("AddGoalActivity", "Goal saved: $goal")
                startActivity(Intent(this@AddGoalActivity, GoalsActivity::class.java))
                finish()
            } catch (e: Exception) {
                Log.e("AddGoalActivity", "Error saving goal: $e")
                Toast.makeText(this@AddGoalActivity, "Error saving goal", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Creates temporary image file in app's external storage
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir("photos")
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    //Logs when activity resumes
    override fun onResume() {
        super.onResume()
        Log.d("AddGoalActivity", "onResume called")
    }
}