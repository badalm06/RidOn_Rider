package com.example.uberriderremake.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.uberriderremake.HomeActivity
import com.example.uberriderremake.MainActivity
import com.example.uberriderremake.R
import com.example.uberriderremake.databinding.ActivityPhoneBinding
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import kotlin.toString

class PhoneActivity : AppCompatActivity() {

        private lateinit var binding: ActivityPhoneBinding
        private lateinit var sendOTPBtn : Button
        private lateinit var phoneNumberET : EditText
        private lateinit var auth : FirebaseAuth
        private lateinit var number : String
        private lateinit var mProgressBar : ProgressBar

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityPhoneBinding.inflate(layoutInflater)
            setContentView(binding.root)

            init()
            sendOTPBtn.setOnClickListener {
                number = phoneNumberET.text.trim().toString()
                if (number.isNotEmpty()){
                    if (number.length == 10){
                        number = "+91$number"
                        mProgressBar.visibility = View.VISIBLE
                        val options = PhoneAuthOptions.newBuilder(auth)
                            .setPhoneNumber(number)
                            .setTimeout(60L, TimeUnit.SECONDS)
                            .setActivity(this)
                            .setCallbacks(callbacks)
                            .build()
                        PhoneAuthProvider.verifyPhoneNumber(options)

                    }else{
                        Toast.makeText(this , "Please Enter correct Number" , Toast.LENGTH_SHORT).show()
                    }
                }else{
                    Toast.makeText(this , "Please Enter Number" , Toast.LENGTH_SHORT).show()

                }
            }
        }

        private fun init(){
            mProgressBar = binding.phoneProgressBar
            mProgressBar.visibility = View.INVISIBLE
            sendOTPBtn = binding.sendOTPBtn
            phoneNumberET = binding.phoneEditTextNumber
            auth = FirebaseAuth.getInstance()
        }

        private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this , "Authenticate Successfully" , Toast.LENGTH_SHORT).show()
                        sendToMain()
                    } else {
                        Log.d("TAG", "signInWithPhoneAuthCredential: ${task.exception.toString()}")
                        if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        }
                    }
                    mProgressBar.visibility = View.INVISIBLE
                }
        }

        private fun sendToMain(){
            startActivity(Intent(this , MainActivity::class.java))

        }
        private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                if (e is FirebaseAuthInvalidCredentialsException) {
                    Log.d("TAG", "onVerificationFailed: ${e.toString()}")
                } else if (e is FirebaseTooManyRequestsException) {
                    Log.d("TAG", "onVerificationFailed: ${e.toString()}")
                }
                mProgressBar.visibility = View.VISIBLE
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                val intent = Intent(this@PhoneActivity , OTPActivity::class.java)
                intent.putExtra("OTP" , verificationId)
                intent.putExtra("resendToken" , token)
                intent.putExtra("phoneNumber" , number)
                startActivity(intent)
                finish()
                mProgressBar.visibility = View.INVISIBLE
            }
        }


        override fun onStart() {
            super.onStart()
            if (auth.currentUser != null){
                startActivity(Intent(this , HomeActivity::class.java))
                finish()
            }
        }
    }
