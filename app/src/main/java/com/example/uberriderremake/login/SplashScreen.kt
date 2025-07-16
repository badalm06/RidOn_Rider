package com.example.uberriderremake.login

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.uberriderremake.Common
import com.example.uberriderremake.HomeActivity
import com.example.uberriderremake.IntroScreens.FirstIntro
import com.example.uberriderremake.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // ✅ User is logged in, load user profile
            FirebaseDatabase.getInstance().getReference("users")
                .child(user.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val name = snapshot.child("name").getValue(String::class.java) ?: ""
                        val phone = snapshot.child("phone").getValue(String::class.java) ?: ""
                        val email = snapshot.child("email").getValue(String::class.java) ?: ""
                        val profileImageUrl =
                            snapshot.child("profileImageUrl").getValue(String::class.java) ?: ""

                        // ✅ Set data to Common.currentUser
                        Common.currentUserRider = User_rider().apply {
                            this.name = name
                            this.number = phone
                            this.email = email
                            this.profileImageUrl = profileImageUrl
                        }

                        // ✅ Get Firebase Messaging Token
                        FirebaseMessaging.getInstance().token
                            .addOnCompleteListener { task ->
                                if (!task.isSuccessful) {
                                    Toast.makeText(
                                        this@SplashScreen,
                                        "Token fetch failed",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@addOnCompleteListener
                                }

                                val token = task.result
                                Log.d("TOKEN", token)
                                User_rider.updateToken(this@SplashScreen, token)

                                // ✅ Go to home screen
                                startActivity(Intent(this@SplashScreen, HomeActivity::class.java))
                                finish()
                            }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            this@SplashScreen,
                            "Failed to load user data",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(Intent(this@SplashScreen, PhoneActivity::class.java))
                        finish()
                    }
                })
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, FirstIntro::class.java))
                finish()
            }, 2000)
        }
    }
}
