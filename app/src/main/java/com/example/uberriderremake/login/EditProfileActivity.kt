package com.example.uberriderremake.login

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.uberriderremake.Common
import com.example.uberriderremake.HomeActivity
import com.example.uberriderremake.R
import com.example.uberriderremake.databinding.ActivityEditProfileBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.concurrent.TimeUnit

class EditProfileActivity : AppCompatActivity() {

        private lateinit var binding: ActivityEditProfileBinding
        private lateinit var firebaseAuth: FirebaseAuth
        private lateinit var database: DatabaseReference
        private lateinit var storageReference: StorageReference
        private lateinit var userRider: FirebaseUser

        private val PICK_IMAGE_REQUEST = 1
        private lateinit var profileImageView: ImageView

        private var isEditing = false
        private var selectedImageUri: Uri? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityEditProfileBinding.inflate(layoutInflater)
            setContentView(binding.root)

            firebaseAuth = FirebaseAuth.getInstance()
            userRider = firebaseAuth.currentUser!!
            database = FirebaseDatabase.getInstance().getReference("rider_users")
            storageReference = FirebaseStorage.getInstance().getReference("profile_images/${userRider.uid}")
            profileImageView = binding.profileImage

            binding.btnBack.setOnClickListener {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }

            binding.driverActivityButton.setOnClickListener {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }


            loadUserProfile()
            loadProfileImage()

            binding.editBtn.setOnClickListener {
                toggleEditMode()
            }

            profileImageView.setOnClickListener {
                if (isEditing) {
                    selectProfileImage()
                }
            }

            binding.logoutBtn.setOnClickListener {
                firebaseAuth.signOut()
                startActivity(Intent(this, PhoneActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                Toast.makeText(this, "Logout Successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
        }


    private fun loadUserProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

        if (uid != null) {
            database.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val name = snapshot.child("name").getValue(String::class.java)
                        val phone = snapshot.child("phone").getValue(String::class.java)
                        val email = snapshot.child("email").getValue(String::class.java)
                        val imageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)

                        binding.etName.setText(name)
                        binding.etEmail.setText(email)
                        binding.etPhone.setText(phone)

                        if (!imageUrl.isNullOrEmpty()) {
                            Glide.with(this@EditProfileActivity)
                                .load(imageUrl)
                                .into(profileImageView)
                        }
                    } else {
                        Toast.makeText(
                            this@EditProfileActivity,
                            "No profile data found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@EditProfileActivity,
                        "Error loading profile",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }



    private fun loadProfileImage() {
            database.child(userRider.uid).child("profileImageUrl")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val imageUrl = snapshot.getValue(String::class.java)
                        if (!imageUrl.isNullOrEmpty()) {
                            Glide.with(this@EditProfileActivity).load(imageUrl).into(profileImageView)
                        } else {
                            profileImageView.setImageResource(R.drawable.person_24)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        profileImageView.setImageResource(R.drawable.person_24)
                    }
                })
        }

        private fun toggleEditMode() {
            isEditing = !isEditing
            setEditable(isEditing)
            binding.editBtn.text = if (isEditing) "Save" else "Edit"
            if (!isEditing) {
                saveUserProfile()
            }
        }

        private fun setEditable(editable: Boolean) {
            binding.etName.isEnabled = editable
            binding.etPhone.isEnabled = editable
            binding.etEmail.isEnabled = editable

        }

        private fun saveUserProfile() {
            val name = binding.etName.text.toString()
            val phone = binding.etPhone.text.toString()
            val email = binding.etEmail.text.toString()

            database.child(userRider.uid).child("phone").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentPhone = snapshot.getValue(String::class.java)
                    if (currentPhone != phone) {
                        sendOtpForVerification(phone, name, email)
                    } else {
                        if (selectedImageUri != null) {
                            uploadImageAndSaveProfile(name, phone, email)
                        } else {
                            updateProfile(name, phone, email, null)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@EditProfileActivity, "Failed to check phone", Toast.LENGTH_SHORT).show()
                }
            })
        }

        private fun uploadImageAndSaveProfile(name: String, phone: String, email: String) {
            storageReference.putFile(selectedImageUri!!)
                .addOnSuccessListener {
                    Toast.makeText(this, "Image Uploaded", Toast.LENGTH_SHORT).show()
                    storageReference.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        Glide.with(this).load(uri).into(profileImageView)
                        updateProfile(name, phone, email, imageUrl)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
                    updateProfile(name, phone, email, null)
                }
        }

        private fun sendOtpForVerification(phoneNumber: String, name: String, email: String) {
            val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                .setPhoneNumber("+91$phoneNumber")
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        verifyOtpAndSave(credential, name, phoneNumber, email)
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        Toast.makeText(this@EditProfileActivity, "OTP failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }

                    override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                        showOtpDialog(verificationId, name, phoneNumber, email)
                    }
                }).build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        }

        private fun showOtpDialog(
            verificationId: String,
            name: String,
            phone: String,
            email: String
        ) {
            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_NUMBER

            AlertDialog.Builder(this)
                .setTitle("Enter OTP")
                .setView(input)
                .setPositiveButton("Verify") { _, _ ->
                    val otp = input.text.toString()
                    val credential = PhoneAuthProvider.getCredential(verificationId, otp)
                    verifyOtpAndSave(credential, name, phone, email)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        private fun verifyOtpAndSave(
            credential: PhoneAuthCredential,
            name: String,
            phone: String,
            email: String
        ) {
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        if (selectedImageUri != null) {
                            uploadImageAndSaveProfile(name, phone, email)
                        } else {
                            updateProfile(name, phone, email, null)
                        }
                    } else {
                        Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        private fun updateProfile(name: String, phone: String, email: String, imageUrl: String?) {
            val userData = mutableMapOf<String, Any>(
                "name" to name,
                "phone" to phone,
                "email" to email
            )
            if (imageUrl != null) {
                userData["profileImageUrl"] = imageUrl
            }

            database.child(userRider.uid).updateChildren(userData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()

                    // ✅ Update Common.currentUser
                    Common.currentUserRider = User_rider().apply {
                        this.name = name
                        this.number = phone
                        this.email = email
                        if (imageUrl != null) {
                            this.profileImageUrl = imageUrl
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
        }


        private fun selectProfileImage() {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)

            if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
                selectedImageUri = data.data
                profileImageView.setImageURI(selectedImageUri)
            }
        }
    }
