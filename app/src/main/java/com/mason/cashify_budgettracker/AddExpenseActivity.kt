
package com.mason.cashify_budgettracker

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.auth.FirebaseAuth
import com.mason.cashify_budgettracker.data.Category
import com.mason.cashify_budgettracker.data.CategoryRepository
import com.mason.cashify_budgettracker.data.Expense
import com.mason.cashify_budgettracker.data.ExpenseRepository
import com.mason.cashify_budgettracker.databinding.ActivityAddExpenseBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    //UI binding and Firebase auth
    private lateinit var binding: ActivityAddExpenseBinding
    private lateinit var auth: FirebaseAuth
    private var photoUri: Uri? = null
    private val categories = mutableListOf<String>()
    private val defaultCategories = listOf("Food", "Transport", "Entertainment", "Bills", "Other")
    private val calendar = Calendar.getInstance()

    //launcher for selecting image from gallery
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        Log.d("AddExpenseActivity", "Gallery result: uri=$uri")
        if (uri != null) {
            try {
                val photoFile = createPhotoFile()
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(photoFile).use { output ->
                        input.copyTo(output)
                    }
                }
                photoUri = FileProvider.getUriForFile(
                    this,
                    "com.mason.cashify_budgettracker.fileprovider",
                    photoFile
                )
                binding.ivPhoto.setImageURI(photoUri)
                binding.ivPhoto.visibility = View.VISIBLE
                Log.d("AddExpenseActivity", "Photo selected and copied: $photoUri")
            } catch (e: Exception) {
                Log.e("AddExpenseActivity", "Error copying photo: $e")
                Toast.makeText(this, "Error copying photo, please try again", Toast.LENGTH_SHORT).show()
                photoUri = null
                binding.ivPhoto.visibility = View.GONE
            }
        } else {
            photoUri = null
            Toast.makeText(this, "Photo selection cancelled", Toast.LENGTH_SHORT).show()
            Log.d("AddExpenseActivity", "Photo selection cancelled")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityAddExpenseBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("AddExpenseActivity", "onCreate: Binding and setContentView successful")
        } catch (e: Exception) {
            Log.e("AddExpenseActivity", "Error in onCreate: $e")
            Toast.makeText(this, "Error loading Add Expense page", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            Log.w("AddExpenseActivity", "No user logged in, redirecting to AuthActivity")
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        binding.btnBack.setOnClickListener {
            Log.d("AddExpenseActivity", "Back button clicked")
            finish()
        }

        if (savedInstanceState != null) {
            photoUri = savedInstanceState.getParcelable("photoUri")
            if (photoUri != null) {
                binding.ivPhoto.setImageURI(photoUri)
                binding.ivPhoto.visibility = View.VISIBLE
                Log.d("AddExpenseActivity", "Restored photoUri: $photoUri")
            }
        }

        lifecycleScope.launch {
            try {
                loadCategories()
                Log.d("AddExpenseActivity", "Categories loaded")
            } catch (e: Exception) {
                Log.e("AddExpenseActivity", "Error loading categories: $e")
                Toast.makeText(this@AddExpenseActivity, "Error accessing data", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        //setup dropdown for categories
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        (binding.categoryInput as MaterialAutoCompleteTextView).setAdapter(categoryAdapter)
        binding.categoryInput.setOnClickListener {
            binding.categoryInput.showDropDown()
        }

        setupDateTimePickers()

        binding.btnCapturePhoto.setOnClickListener {
            Log.d("AddExpenseActivity", "Select Photo clicked")
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.addCategoryText.setOnClickListener {
            Log.d("AddExpenseActivity", "Add Category clicked")
            showAddCategoryDialog()
        }

        binding.btnSave.setOnClickListener {
            Log.d("AddExpenseActivity", "Save clicked")
            saveExpense()
        }

        //bottom navigation handling
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Log.d("AddExpenseActivity", "Navigating to MainActivity")
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_categories -> {
                    Log.d("AddExpenseActivity", "Add tab selected")
                    true
                }
                R.id.nav_goals -> {
                    Log.d("AddExpenseActivity", "Navigating to GoalsActivity")
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

        binding.bottomNav.menu.findItem(R.id.nav_home)?.isChecked = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("photoUri", photoUri)
        Log.d("AddExpenseActivity", "onSaveInstanceState: Saved photoUri")
    }

    //create file for storing picked photo
    private fun createPhotoFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = File(filesDir, "photos")
        if (!storageDir.exists()) storageDir.mkdirs()
        return File(storageDir, "JPEG_${timeStamp}.jpg")
    }

    //setup date and time pickers
    private fun setupDateTimePickers() {
        binding.dateInput.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                binding.dateInput.setText(formattedDate)
                Log.d("AddExpenseActivity", "Date selected: $formattedDate")
            }, year, month, day).show()
            Log.d("AddExpenseActivity", "DatePickerDialog shown")
        }

        binding.startTimeInput.setOnClickListener {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                binding.startTimeInput.setText(formattedTime)
                Log.d("AddExpenseActivity", "Start time selected: $formattedTime")
            }, hour, minute, true).show()
            Log.d("AddExpenseActivity", "TimePickerDialog shown for startTime")
        }

        binding.endTimeInput.setOnClickListener {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                binding.endTimeInput.setText(formattedTime)
                Log.d("AddExpenseActivity", "End time selected: $formattedTime")
            }, hour, minute, true).show()
            Log.d("AddExpenseActivity", "TimePickerDialog shown for endTime")
        }
    }

    //load categories from DB or default
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
                val categoryAdapter = ArrayAdapter(this@AddExpenseActivity, android.R.layout.simple_dropdown_item_1line, categories)
                (binding.categoryInput as MaterialAutoCompleteTextView).setAdapter(categoryAdapter)
                Log.d("AddExpenseActivity", "Categories loaded: $categories")
            } catch (e: Exception) {
                Log.e("AddExpenseActivity", "Error loading categories: $e")
                Toast.makeText(this@AddExpenseActivity, "Error loading categories", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //show dialog to add a new category
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
                                Toast.makeText(this@AddExpenseActivity, "Category added", Toast.LENGTH_SHORT).show()
                                Log.d("AddExpenseActivity", "Category added: $categoryName")
                            } catch (e: Exception) {
                                Log.e("AddExpenseActivity", "Error adding category: $e")
                                Toast.makeText(this@AddExpenseActivity, "Error adding category", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Invalid or duplicate category", Toast.LENGTH_SHORT).show()
                        Log.w("AddExpenseActivity", "Invalid category: $categoryName")
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
            Log.d("AddExpenseActivity", "Add Category dialog shown")
        } catch (e: Exception) {
            Log.e("AddExpenseActivity", "Error showing add category dialog: $e")
            Toast.makeText(this, "Error showing category dialog", Toast.LENGTH_SHORT).show()
        }
    }

    //validate inputs and save expense to DB
    private fun saveExpense() {
        val category = binding.categoryInput.text.toString().trim()
        val type = if (binding.radioIncome.isChecked) "income" else "expense"
        val amountStr = binding.amountInput.text.toString().trim()
        val date = binding.dateInput.text.toString().trim()
        val startTime = binding.startTimeInput.text.toString().trim()
        val endTime = binding.endTimeInput.text.toString().trim()
        val description = binding.descriptionInput.text.toString().trim()
        val userId = auth.currentUser?.uid ?: return

        if (category.isEmpty() || amountStr.isEmpty() || date.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            Log.w("AddExpenseActivity", "Validation failed: Empty fields")
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
            Log.w("AddExpenseActivity", "Validation failed: Invalid amount")
            return
        }

        lifecycleScope.launch {
            try {
                val categoryId = withContext(Dispatchers.IO) {
                    CategoryRepository.getCategories(userId)
                        .find { it.name == category }?.id ?: category
                }

                val expense = Expense(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    amount = amount,
                    date = date,
                    timestamp = try {
                        val calendar = Calendar.getInstance()
                        calendar.time = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(date)
                        calendar.set(Calendar.HOUR_OF_DAY, 0)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        calendar.timeInMillis
                    } catch (e: Exception) {
                        Log.e("AddExpenseActivity", "Error parsing date: $date", e)
                        Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    },
                    category = category,
                    categoryId = categoryId,
                    type = type,
                    photoPath = photoUri?.toString() ?: "",
                    startTime = startTime,
                    endTime = endTime,
                    description = description
                )

                withContext(Dispatchers.IO) {
                    ExpenseRepository.insert(expense)
                }

                Toast.makeText(this@AddExpenseActivity, "Expense saved", Toast.LENGTH_SHORT).show()
                Log.d("AddExpenseActivity", "Expense saved: $expense")
                startActivity(Intent(this@AddExpenseActivity, MainActivity::class.java))
                finish()
            } catch (e: Exception) {
                Log.e("AddExpenseActivity", "Error saving expense: $e")
                Toast.makeText(this@AddExpenseActivity, "Error saving expense", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("AddExpenseActivity", "onResume called")
    }
}
