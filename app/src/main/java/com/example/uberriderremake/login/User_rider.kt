package com.example.uberriderremake.login

import android.content.Context
import android.widget.Toast
import com.example.uberriderremake.Model.TokenModel
import com.example.uberriderremake.Common
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

data class User_rider(var name: String ="", var email: String = "", var number: String ="", var profileImageUrl: String? = null
) {

    companion object {
        fun updateToken(context: Context, token: String) {
            val tokenModel = TokenModel()
            tokenModel.token = token;

            FirebaseDatabase.getInstance()
                .getReference(Common.TOKEN_REFERENCE)
                .child(FirebaseAuth.getInstance().currentUser!!.uid)
                .setValue(tokenModel)
                .addOnFailureListener { e -> Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show() }
                .addOnSuccessListener {  }
        }
    }

}
