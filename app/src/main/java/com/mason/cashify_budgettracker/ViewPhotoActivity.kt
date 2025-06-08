package com.mason.cashify_budgettracker

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mason.cashify_budgettracker.databinding.ActivityViewPhotoBinding

class ViewPhotoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewPhotoBinding

    //onCreate is called when the activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //inflate the layout using view binding
        try {
            binding = ActivityViewPhotoBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d("ViewPhotoActivity", "onCreate: Binding and setContentView successful")
        } catch (e: Exception) {
            //catch any errors during binding and layout inflation
            Log.e("ViewPhotoActivity", "Error in onCreate: $e")
            Toast.makeText(this, "Error loading photo view", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        //retrieve photo path passed through intent
        val photoPath = intent.getStringExtra("photoPath")
        Log.d("ViewPhotoActivity", "Received photoPath: $photoPath")

        //check if photo path is empty or null
        if (photoPath.isNullOrEmpty()) {
            Log.w("ViewPhotoActivity", "No photo path provided")
            Toast.makeText(this, "No photo available", Toast.LENGTH_SHORT).show()
            finish()  // Finish activity if no photo path
            return
        }

        //try to open photo file and display it
        try {
            contentResolver.openInputStream(android.net.Uri.parse(photoPath))?.use { inputStream ->
                //decode input stream into a Bitmap and display it
                val bitmap = BitmapFactory.decodeStream(inputStream)
                binding.ivFullPhoto.setImageBitmap(bitmap)
                Log.d("ViewPhotoActivity", "Displayed photo: $photoPath")
            } ?: throw Exception("Failed to open input stream for URI")
        } catch (e: Exception) {
            //handle errors when loading photo
            Log.e("ViewPhotoActivity", "Error loading photo: $e")
            Toast.makeText(this, "Error loading photo", Toast.LENGTH_SHORT).show()
            finish()
        }

        //set up back button listener
        binding.btnBack.setOnClickListener {
            Log.d("ViewPhotoActivity", "Back button clicked")
            finish()
        }
    }
}
