package com.mason.cashify_budgettracker

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mason.cashify_budgettracker.databinding.ActivityAddExpenseBinding
import com.mason.cashify_budgettracker.model.Expense
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddExpenseBinding
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private var selectedDate = Calendar.getInstance()
    private var selectedStartTime = Calendar.getInstance()
    private var selectedEndTime = Calendar.getInstance()

    private val defaultCategories = listOf("Food", "Transport", "Entertainment", "Bills", "Other")
    private val categories = mutableListOf<String>()
    private lateinit var categoryAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize with default categories
        categories.addAll(defaultCategories)

        setupDateAndTimePickers()
        setupCategoryDropdown()
        setupSaveButton()
        setupPhotoButton()
        setupAddCategoryButton()
        setDefaultValues()

        // Load user's custom categories
        loadUserCategories()
    }

    private fun setDefaultValues() {
        binding.etDate.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate.time))
        binding.etStartTime.setText(String.format("%02d:%02d", selectedStartTime.get(Calendar.HOUR_OF_DAY), selectedStartTime.get(Calendar.MINUTE)))
        binding.etEndTime.setText(String.format("%02d:%02d", selectedEndTime.get(Calendar.HOUR_OF_DAY), selectedEndTime.get(Calendar.MINUTE)))
    }

    private fun setupDateAndTimePickers() {
        binding.etDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    selectedDate.set(year, month, day)
                    binding.etDate.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate.time))
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.etStartTime.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    selectedStartTime.set(Calendar.HOUR_OF_DAY, hour)
                    selectedStartTime.set(Calendar.MINUTE, minute)
                    binding.etStartTime.setText(String.format("%02d:%02d", hour, minute))
                },
                selectedStartTime.get(Calendar.HOUR_OF_DAY),
                selectedStartTime.get(Calendar.MINUTE),
                true
            ).show()
        }

        binding.etEndTime.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    selectedEndTime.set(Calendar.HOUR_OF_DAY, hour)
                    selectedEndTime.set(Calendar.MINUTE, minute)
                    binding.etEndTime.setText(String.format("%02d:%02d", hour, minute))
                },
                selectedEndTime.get(Calendar.HOUR_OF_DAY),
                selectedEndTime.get(Calendar.MINUTE),
                true
            ).show()
        }
    }

    private fun setupCategoryDropdown() {
        categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.actvCategory.setAdapter(categoryAdapter)
    }

    private fun loadUserCategories() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("categories")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val category = document.getString("name") ?: continue
                    if (!categories.contains(category)) {
                        categories.add(category)
                    }
                }
                categories.sort()
                categoryAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading categories: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val amount = binding.etAmount.text.toString().toDoubleOrNull() ?: 0.0
            val description = binding.etDescription.text.toString()
            val category = binding.actvCategory.text.toString()
            val isIncome = binding.radioGroupType.checkedRadioButtonId == R.id.rbIncome

            if (description.isBlank()) {
                Toast.makeText(this, "Please enter a description", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (category.isBlank()) {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Add the category if it's new
            if (!categories.contains(category)) {
                addNewCategory(category)
            }

            val expense = Expense(
                amount = amount,
                description = description,
                date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate.time),
                category = category,
                type = if (isIncome) "income" else "expense",
                userId = auth.currentUser?.uid ?: "",
                startTime = binding.etStartTime.text.toString(),
                endTime = binding.etEndTime.text.toString()
            )

            saveExpenseToFirestore(expense)
        }
    }

    private fun saveExpenseToFirestore(expense: Expense) {
        db.collection("expenses")
            .add(expense)
            .addOnSuccessListener {
                Toast.makeText(this, "Transaction saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving transaction: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupPhotoButton() {
        binding.btnAddPhoto.setOnClickListener {
            Toast.makeText(this, "Photo feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAddCategoryButton() {
        binding.btnAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }
    }

    private fun showAddCategoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_category, null)
        val editTextCategory = dialogView.findViewById<EditText>(R.id.etNewCategory)

        AlertDialog.Builder(this)
            .setTitle("Create New Category")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val newCategory = editTextCategory.text.toString().trim()
                if (newCategory.isNotEmpty()) {
                    addNewCategory(newCategory)
                } else {
                    Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addNewCategory(newCategory: String) {
        if (categories.contains(newCategory)) {
            Toast.makeText(this, "Category already exists", Toast.LENGTH_SHORT).show()
            return
        }

        categories.add(newCategory)
        categories.sort()
        categoryAdapter.notifyDataSetChanged()
        binding.actvCategory.setText(newCategory)

        saveCategoryToFirestore(newCategory)
    }

    private fun saveCategoryToFirestore(categoryName: String) {
        val userId = auth.currentUser?.uid ?: return

        val categoryData = hashMapOf(
            "name" to categoryName,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(userId).collection("categories")
            .add(categoryData)
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving category: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}