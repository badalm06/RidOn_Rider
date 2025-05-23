package com.example.uberriderremake

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import com.bumptech.glide.Glide
import com.example.uberriderremake.databinding.ActivityHomeBinding
import com.example.uberriderremake.login.EditProfileActivity
import com.example.uberriderremake.login.PhoneActivity
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navView: NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    private lateinit var binding: ActivityHomeBinding
    private lateinit var profileImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarHome.toolbar)


        drawerLayout = binding.drawerLayout as DrawerLayout
        navView = findViewById(R.id.nav_view)
        navController = findNavController(R.id.nav_host_fragment_content_home)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        //navView.setupWithNavController(navController)
        init()
    }

    private fun init() {
        navView.setNavigationItemSelectedListener { it ->
            if(it.itemId == R.id.nav_sign_out) {
                val builder = AlertDialog.Builder(this@HomeActivity)
                builder.setTitle("Sign Out")
                builder.setMessage("Do you really want to Sign Out")
                builder.setPositiveButton("Sign Out") { dialog, _ ->
                    // Handle Yes action
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, PhoneActivity::class.java))
                    finish()
                }
                builder.setNegativeButton("Cancle") { dialog, _ ->
                    dialog.dismiss()
                }
                val dialog = builder.create()
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(resources.getColor(android.R.color.holo_red_dark))

                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                        .setTextColor(resources.getColor(R.color.colorAccent))
                }
                dialog.show()
            }
            if(it.itemId == R.id.nav_edit_profile) {
                startActivity(Intent(this, EditProfileActivity::class.java))
            }
            true
        }


        val headerView = navView.getHeaderView(0)
        val txt_name = headerView.findViewById<TextView>(R.id.txt_name) as TextView
        val txt_phone = headerView.findViewById<TextView>(R.id.txt_phone) as TextView
        val image_avatar = headerView.findViewById<ImageView>(R.id.image_avatar)


        txt_name.text = Common.buildWelcomeMessage()
        txt_phone.text = Common.currentUserRider?.number ?: "N/A"
        val imageUrl = Common.currentUserRider?.profileImageUrl
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.person_24)
                .into(image_avatar)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_home)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
