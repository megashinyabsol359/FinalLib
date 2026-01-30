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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.finallib.R
import com.example.finallib.library.LibraryFragment
import com.example.finallib.utils.CloudinaryConfig
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


import com.example.finallib.auth.LoginActivity
import com.example.finallib.auth.ChangePasswordFragment
import com.example.finallib.auth.RegisterSellerFragment
import com.example.finallib.admin.SystemLogFragment
import com.example.finallib.admin.AdminNotificationFragment
import com.example.finallib.search.SearchActivity
import com.example.finallib.admin.UserListFragment
import com.example.finallib.bookshelf.BookshelfFragment
import com.example.finallib.admin.BookApprovalActivity

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
            // Kiểm tra loại file (sách hay ảnh)
            val mimeType = contentResolver.getType(it)
            if (mimeType?.startsWith("image/") == true) {
                uploadDialog?.setSelectedCover(it)
            } else {
                uploadDialog?.setSelectedFile(it, fileName)
            }
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
                }
                else if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                }
                else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Menu
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> replaceFragment(BookshelfFragment())

                R.id.nav_upload_book -> {
                    showUploadDialog()
                }

                // Tìm kiếm
                R.id.nav_search -> {
                    startActivity(Intent(this, SearchActivity::class.java))
                }

                // User: Đăng ký bán hàng
                R.id.nav_register_seller -> replaceFragment(RegisterSellerFragment())

                // Admin: Duyệt đơn
                R.id.nav_admin_noti -> replaceFragment(AdminNotificationFragment())

                // Admin: Duyệt sách upload
                R.id.nav_book_approval -> {
                    startActivity(Intent(this, BookApprovalActivity::class.java))
                }

                // Admin: Danh sách tài khoản
                R.id.nav_user_list -> replaceFragment(UserListFragment())

                // Admin: Xem Log
                R.id.nav_logs -> replaceFragment(SystemLogFragment())

                // Chung: Đổi mật khẩu
                R.id.nav_change_pass -> replaceFragment(ChangePasswordFragment())

                // Đăng xuất (Thoát ra LoginActivity)
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
            replaceFragment(BookshelfFragment())
            navView.setCheckedItem(R.id.nav_home)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        if (fragment !is LibraryFragment) {
            transaction.addToBackStack(null)
        }
        transaction.commit()
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

    // Phân quyền Menu
    private fun checkUserRole() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    val role = document.getString("role")
                    val menu = navView.menu

                    when (role) {
                        "Admin" -> {
                            menu.findItem(R.id.nav_logs)?.isVisible = true
                            menu.findItem(R.id.nav_admin_noti)?.isVisible = true
                            menu.findItem(R.id.nav_user_list)?.isVisible = true
                            menu.findItem(R.id.nav_book_approval)?.isVisible = true
                        }
                        "User" -> {
                            menu.findItem(R.id.nav_register_seller)?.isVisible = true
                        }
                    }
                }
        }
    }
}