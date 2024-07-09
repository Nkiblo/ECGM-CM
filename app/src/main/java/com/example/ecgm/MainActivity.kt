package com.example.ecgm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.example.ecgm.databinding.ActivityMainBinding
import com.example.ecgm.ui.home.HomeFragment
import com.example.ecgm.ui.projects.ProjectFragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Main activity for handling navigation and user interactions
open class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var navUsername: TextView
    private lateinit var navEmail: TextView
    private lateinit var navProfileImageView: ImageView
    private var userRole: String? = null
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply locale before setting content view
        LocaleHelper.setLocale(this, getCurrentLanguage())
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        drawerLayout = binding.drawerLayout
        navView = binding.navView
        navUsername = navView.getHeaderView(0).findViewById(R.id.nav_username)
        navEmail = navView.getHeaderView(0).findViewById(R.id.nav_email)
        navProfileImageView = navView.getHeaderView(0).findViewById(R.id.imageView)

        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Configure AppBarConfiguration with top-level destinations
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_tasks, R.id.nav_finished_tasks, R.id.nav_projects, R.id.nav_profile, R.id.nav_language, R.id.nav_disconnect
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.setNavigationItemSelectedListener(this)

        // Load user info into the header
        loadUserInfo()

        // Configure FAB click listener
        binding.appBarMain.fab.setOnClickListener {
            showPopupMenu(it)
        }
    }

    // Get current language from shared preferences
    private fun getCurrentLanguage(): String {
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        return sharedPref.getString("selected_language", "pt") ?: "en"
    }

    // Load user info from Firebase
    private fun loadUserInfo() {
        val user = auth.currentUser
        if (user != null) {
            navEmail.text = user.email
            db.collection("users").document(user.uid).get().addOnSuccessListener { document ->
                if (document != null) {
                    val username = document.getString("username")
                    if (!username.isNullOrEmpty()) {
                        navUsername.text = username
                    }

                    val profileImageUrl = document.getString("profileImageUrl")
                    if (!profileImageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.mipmap.ic_launcher_round) // Placeholder image while loading
                            .error(R.mipmap.ic_launcher_round) // Error image if loading fails
                            .into(navProfileImageView)
                    }

                    userRole = document.getString("role")
                    userId = document.getString("id")

                    // Log user info
                    Log.d("MainActivity", "User ID: $userId")
                    Log.d("MainActivity", "User Role: $userRole")

                    // Pass user info to the fragment
                    val navController = findNavController(R.id.nav_host_fragment_content_main)
                    val bundle = Bundle().apply {
                        putString("userId", userId)
                        putString("userRole", userRole)
                    }

                    val homeFragment = HomeFragment().apply {
                        arguments = bundle
                    }

                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment_content_main, homeFragment)
                        .commit()
                }
            }.addOnFailureListener {
                // Handle error loading user info
                Log.e("MainActivity", "Error loading user info", it)
            }
        }
    }

    // Show popup menu based on user role
    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        when (userRole) {
            "admin" -> {
                popupMenu.menuInflater.inflate(R.menu.popup_menu_admin, popupMenu.menu)
            }
            "project_manager" -> {
                popupMenu.menuInflater.inflate(R.menu.popup_menu_project_manager, popupMenu.menu)
            }
            "user" -> {
                popupMenu.menuInflater.inflate(R.menu.popup_menu_user, popupMenu.menu)
            }
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_manage_users -> {
                    // Handle manage users action
                    startActivity(Intent(this, UserEditSelectionActivity::class.java))
                    true
                }
                R.id.action_create_project -> {
                    // Handle create project action
                    startActivity(Intent(this, CreateProjectActivity::class.java))
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    // Handle navigation up action
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    // Handle navigation item selections
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                // Navigate to HomeFragment with user info
                val bundle = Bundle().apply {
                    putString("userId", userId)
                    putString("userRole", userRole)
                }
                val homeFragment = HomeFragment().apply {
                    arguments = bundle
                }
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment_content_main, homeFragment)
                    .addToBackStack(null)
                    .commit()
            }
            R.id.nav_profile -> {
                startActivityForResult(Intent(this, ProfileActivity::class.java), PROFILE_REQUEST_CODE)
            }
            R.id.nav_tasks -> {
                startActivity(Intent(this, AssignedTasksActivity::class.java))
            }
            R.id.nav_finished_tasks -> {
                startActivity(Intent(this, UserFinishedTaskActivity::class.java))
            }
            R.id.nav_language -> {
                showLanguageSelectionDialog()
            }
            R.id.nav_projects -> {
                // Navigate to ProjectFragment with user info
                val bundle = Bundle().apply {
                    putString("userId", userId)
                    putString("userRole", userRole)
                }
                val projectFragment = ProjectFragment().apply {
                    arguments = bundle
                }
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment_content_main, projectFragment)
                    .addToBackStack(null)
                    .commit()
            }
            R.id.nav_disconnect -> {
                startActivityForResult(Intent(this, LoginActivity::class.java), PROFILE_REQUEST_CODE)
            }
            // Handle other navigation menu item clicks if needed
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    // Show language selection dialog
    private fun showLanguageSelectionDialog() {
        val languages = arrayOf("English", "Portuguese")

        AlertDialog.Builder(this)
            .setTitle("Select Language")
            .setItems(languages) { dialog, which ->
                when (which) {
                    0 -> switchLanguage("en")
                    1 -> switchLanguage("pt")
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Switch app language
    private fun switchLanguage(languageCode: String) {
        LocaleHelper.setLocale(this, languageCode)

        // Save the selected language in SharedPreferences
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("selected_language", languageCode)
            apply()
        }

        // Restart the activity to apply the language change
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    // Handle activity result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PROFILE_REQUEST_CODE && resultCode == RESULT_OK) {
            // Update user info in the drawer menu when returning from ProfileActivity
            loadUserInfo()
        }
    }

    companion object {
        const val PROFILE_REQUEST_CODE = 1
    }
}
