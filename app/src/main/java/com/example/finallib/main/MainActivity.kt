package com.example.finallib.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.example.finallib.R
import com.example.finallib.library.LibraryFragment
import com.example.finallib.utils.CloudinaryConfig
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


import com.example.finallib.auth.LoginActivity
import com.example.finallib.auth.ChangePasswordActivity
import com.example.finallib.auth.RegisterSellerActivity
import com.example.finallib.admin.SystemLogActivity
import com.example.finallib.admin.AdminNotificationActivity
import com.example.finallib.search.SearchActivity

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private var uploadDialog: UploadBookDialog? = null

    // File picker để chọn file sách
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileNameFromUri(it)
            uploadDialog?.setSelectedFile(it, fileName)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Khởi tạo Cloudinary config
        CloudinaryConfig.initialize(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        updateNavHeader()

        checkUserRole()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Xử lý chuyển fragment nếu cần
                }

                R.id.nav_upload_book -> {
                    showUploadDialog()
                }

                // Tìm kiếm
                R.id.nav_search -> {
                    startActivity(Intent(this, SearchActivity::class.java))
                }

                // User: Đăng ký bán hàng
                R.id.nav_register_seller -> {
                    startActivity(Intent(this, RegisterSellerActivity::class.java))
                }

                // Admin: Duyệt đơn
                R.id.nav_admin_noti -> {
                    startActivity(Intent(this, AdminNotificationActivity::class.java))
                }

                // ADMIN: Xem Log
                R.id.nav_logs -> {
                    startActivity(Intent(this, SystemLogActivity::class.java))
                }

                R.id.nav_change_pass -> {
                    startActivity(Intent(this, ChangePasswordActivity::class.java))
                }

                R.id.nav_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LibraryFragment())
                .commit()
            navView.setCheckedItem(R.id.nav_home)
        }
    }

    private fun showUploadDialog() {
        uploadDialog = UploadBookDialog(
            context = this,
            lifecycleScope = lifecycleScope,
            fileLauncher = filePickerLauncher,
            onSuccess = { docId ->
                // Callback khi upload thành công
            }
        )
        uploadDialog?.show()
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var fileName = "book_file"
        try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                it.moveToFirst()
                fileName = it.getString(nameIndex)
            }
        } catch (e: Exception) {
            fileName = uri.lastPathSegment ?: "book_file"
        }
        return fileName
    }

    private fun updateNavHeader() {
        val user = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()

        if (user != null) {
            val headerView = navView.getHeaderView(0)
            val tvName = headerView.findViewById<TextView>(R.id.tv_header_name)

            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val fullName = document.getString("fullName")
                        tvName.text = fullName ?: "Xin chào!"
                    }
                }
                .addOnFailureListener {
                    tvName.text = "Khách"
                }
        }
    }

    // Kiểm tra Role
    private fun checkUserRole() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    val role = document.getString("role")
                    val menu = navView.menu

                    if (role == "Admin") {
                        menu.findItem(R.id.nav_logs)?.isVisible = true
                        menu.findItem(R.id.nav_admin_noti)?.isVisible = true
                    }
                    else if (role == "User") {
                        menu.findItem(R.id.nav_register_seller)?.isVisible = true
                    }
                }
        }
    }
}