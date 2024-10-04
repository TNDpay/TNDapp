package com.example.tnd

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import com.example.tnd.databinding.ActivityAddStoreBinding
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import androidx.appcompat.app.AlertDialog


class AddStoreActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddStoreBinding
    private var isExpanded = false
    private lateinit var db: FirebaseFirestore
    private var userAddress: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddStoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)
        userAddress = intent.getStringExtra("USER_ADDRESS") ?: ""

        setupExpandableLayout()
        setupInfoIcon()

        binding.tvCoordinates.text = "Latitude: $latitude, Longitude: $longitude"
        // Set default value for referral address
        binding.etReferral.setText(userAddress)

        binding.btnSubmit.setOnClickListener {
            val name = binding.etName.text.toString()
            val social = binding.etSocial.text.toString()
            val email = binding.etEmail.text.toString()
            val referralAddress = binding.etReferral.text.toString()
            val openingHour = binding.tpOpeningTime.hour
            val openingMinute = binding.tpOpeningTime.minute
            val closingHour = binding.tpClosingTime.hour
            val closingMinute = binding.tpClosingTime.minute

            if (name.isBlank()) {
                binding.etName.error = "Name is required"
                return@setOnClickListener
            }

            if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Valid email is required"
                return@setOnClickListener
            }

            val operatingHours = String.format("%02d:%02d - %02d:%02d", openingHour, openingMinute, closingHour, closingMinute)

            // Create a new store object
            val store = hashMapOf(
                "name" to name,
                "social" to social,
                "email" to email,
                "operatingHours" to operatingHours,
                "latitude" to latitude,
                "longitude" to longitude,
                "referralAddress" to referralAddress
            )

            // Add a new document with a generated ID
            db.collection("stores")
                .add(store)
                .addOnSuccessListener { documentReference ->
                    Toast.makeText(this, "Commerce added: $name with ID: ${documentReference.id}", Toast.LENGTH_LONG).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error adding commerce: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("FireBase","Error adding commerce: ${e.message}")                }
        }
    }

    private fun setupExpandableLayout() {
        binding.operatingHoursHeader.setOnClickListener {
            isExpanded = !isExpanded
            binding.expandableLayout.isExpanded = isExpanded
            binding.arrowIcon.rotation = if (isExpanded) 180f else 0f
        }
    }
    private fun setupInfoIcon() {
        binding.infoIcon.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Referral Address Information")
                .setMessage("This address will be used for future airdrops and rewards programs. Make sure it's correct!")
                .setPositiveButton("Got it") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }
}