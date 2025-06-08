
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
import com.mason.cashify_budgettracker.data.Category
import com.mason.cashify_budgettracker.data.CategoryRepository
import com.mason.cashify_budgettracker.data.Goal
import com.mason.cashify_budgettracker.data.GoalRepository
import com.mason.cashify_budgettracker.databinding.ActivityAddGoalBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddGoalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddGoalBinding
    private lateinit var auth: FirebaseAuth
    private var photoPath: String? = null
    private val categories = mutableListOf<String>()
    private val defaultCategories = listOf("Food", "Transport", "Entertainment", "Bills", "Other")

    //launcher for requesting storage permission
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            Log.d("AddGoalActivity", "Storage permission granted, launching gallery")
            launchGallery()
        } else {
            Log.w("AddGoalActivity", "Storage permission denied")
            Toast.makeText(this, "Storage permission is required to select photos", Toast.LENGTH_LONG).show()
        }
    }

    //launcher for selecting an image from the gallery
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

        //initialize Firebase Auth and check if user is logged in
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Log.w("AddGoalActivity", "No user logged in, redirecting to AuthActivity")
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        binding.btnBack.setOnClickListener {
            Log.d("AddGoalActivity", "Back button clicked")
            finish()
        }

        //restore photo path on configuration changes
        if (savedInstanceState != null) {
            photoPath = savedInstanceState.getString("photoPath")
            if (photoPath != null) {
                binding.ivPhoto.setImageURI(File(photoPath!!).toUri())
                binding.ivPhoto.visibility = View.VISIBLE
                Log.d("AddGoalActivity", "Restored photoPath: $photoPath")
            }
        }

        lifecycleScope.launch {
            try {
                loadCategories()
                Log.d("AddGoalActivity", "Categories loaded")
            } catch (e: Exception) {
                Log.e("AddGoalActivity", "Error loading categories: $e")
                Toast.makeText(this@AddGoalActivity, "Error accessing data", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        //setup category dropdown adapter
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        (binding.categoryInput as MaterialAutoCompleteTextView).setAdapter(categoryAdapter)
        binding.categoryInput.setOnClickListener {
            binding.categoryInput.showDropDown()
        }

        binding.monthInput.setOnClickListener {
            Log.d("AddGoalActivity", "Month input clicked")
            showMonthPicker()
        }

        binding.btnCapturePhoto.setOnClickListener {
            Log.d("AddGoalActivity", "Add Photo clicked")
            checkPermissionsAndSelectPhoto()
        }

        binding.addCategoryText.setOnClickListener {
            Log.d("AddGoalActivity", "Add Category clicked")
            showAddCategoryDialog()
        }

        binding.btnSave.setOnClickListener {
            Log.d("AddGoalActivity", "Save clicked")
            saveGoal()
        }

        //setup bottom navigation
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
                    Log.d("AddGoalActivity", "Navigating to GoalsActivity")
                    startActivity(Intent(this, GoalsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_calendar -> {
                    Log.d("MainActivity", "Navigating to CalendarSets")
                    startActivity(Intent(this, CalendarSets::class.java))
                    true
                }
                R.id.nav_reports -> {
                    Log.d("MainActivity", "Navigating to ReportsActivity")
                    startActivity(Intent(this, ReportsActivity::class.java))
                    true
                }
                else -> false
            }
        }
        binding.bottomNav.menu.findItem(R.id.nav_goals)?.isChecked = true
    }

    //save photo path to instance state bundle on config change
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("photoPath", photoPath)
        Log.d("AddGoalActivity", "onSaveInstanceState: Saved photoPath")
    }

    //show a date picker dialog limited to month/year selection
    private fun showMonthPicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Goal Month")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

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

        datePicker.addOnNegativeButtonClickListener {
            Log.d("AddGoalActivity", "Month picker cancelled")
        }

        datePicker.show(supportFragmentManager, "MONTH_PICKER")
    }

    //check and request storage permissions before opening gallery
    private fun checkPermissionsAndSelectPhoto() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        when {
            checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED -> {
                Log.d("AddGoalActivity", "Storage permission already granted, launching gallery")
                launchGallery()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                Log.d("AddGoalActivity", "Showing permission rationale for storage")
                showPermissionRationale()
            }
            else -> {
                Log.d("AddGoalActivity", "Requesting storage permission")
                permissionLauncher.launch(permission)
            }
        }
    }

    //show rationale dialog for storage permission
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

    //launch the gallery to pick an image
    private fun launchGallery() {
        galleryLauncher.launch("image/*")
        Log.d("AddGoalActivity", "Gallery launched")
    }

    //load categories from repository and update UI
    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val dbCategories = withContext(Dispatchers.IO) {
                    CategoryRepository.getCategories(userId)
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

    //show dialog to add a new custom category
    private fun showAddCategoryDialog() {
        try {
            val builder = AlertDialog.Builder(this)
            val dialogBinding = layoutInflater.inflate(R.layout.dialog_add_category, null)
            val categoryInput = dialogBinding.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.categoryInput)
            builder.setView(dialogBinding)
                .setTitle("Add Category")
                .setPositiveButton("Add") { _, _ ->
                    val categoryName = categoryInput?.text?.toString()?.trim() ?: ""
                    if (categoryName.isNotEmpty() && !categories.contains(categoryName)) {
                        lifecycleScope.launch {
                            try {
                                val userId = auth.currentUser?.uid ?: return@launch
                                withContext(Dispatchers.IO) {
                                    CategoryRepository.insert(Category(userId = userId, name = categoryName))
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

    //validate inputs and save the goal to the repository
    private fun saveGoal() {
        val month = binding.monthInput.text.toString().trim()
        val category = binding.categoryInput.text.toString().trim()
        val type = if (binding.radioIncome.isChecked) "income" else "expense"
        val description = binding.descriptionInput.text.toString().trim()
        val minGoalStr = binding.minGoalInput.text.toString().trim()
        val maxGoalStr = binding.maxGoalInput.text.toString().trim()
        val userId = auth.currentUser?.uid ?: return

        if (month.isEmpty() || category.isEmpty() || minGoalStr.isEmpty() || maxGoalStr.isEmpty()) {
            Toast.makeText(this, "Please fill month, category, min goal, and max goal", Toast.LENGTH_SHORT).show()
            Log.w("AddGoalActivity", "Validation failed: Empty fields")
            return
        }

        val minGoal = minGoalStr.toDoubleOrNull()
        val maxGoal = maxGoalStr.toDoubleOrNull()

        if (minGoal == null || maxGoal == null) {
            Toast.makeText(this, "Invalid min or max goal", Toast.LENGTH_SHORT).show()
            Log.w("AddGoalActivity", "Validation failed: Invalid min/max goal")
            return
        }

        if (minGoal >= maxGoal) {
            Toast.makeText(this, "Minimum goal must be less than maximum goal", Toast.LENGTH_SHORT).show()
            Log.w("AddGoalActivity", "Validation failed: minGoal >= maxGoal")
            return
        }

        lifecycleScope.launch {
            try {
                val categoryId = withContext(Dispatchers.IO) {
                    CategoryRepository.getCategories(userId)
                        .find { it.name == category }?.id ?: category
                }

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
                    GoalRepository.insert(goal)
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

    //create a temporary file to store the selected image
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir("photos")
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    override fun onResume() {
        super.onResume()
        Log.d("AddGoalActivity", "onResume called")
    }
}