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
import com.example.finallib.search.MainSearchFragment
import com.example.finallib.admin.UserListFragment
import com.example.finallib.bookshelf.BookshelfFragment

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

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
                    // Update title after pop
                    updateTitleAfterPop()
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
                R.id.nav_home -> replaceFragment(BookshelfFragment(), "Tủ sách")

                R.id.nav_upload_book -> replaceFragment(UploadBookFragment(), "Upload Sách")

                R.id.nav_search -> replaceFragment(MainSearchFragment(), "Tìm kiếm")

                R.id.nav_register_seller -> replaceFragment(RegisterSellerFragment(), "Đăng ký bán hàng")

                R.id.nav_admin_noti -> replaceFragment(AdminNotificationFragment(), "Duyệt đơn")

                R.id.nav_user_list -> replaceFragment(UserListFragment(), "Danh sách người dùng")

                R.id.nav_logs -> replaceFragment(SystemLogFragment(), "Hệ thống Log")

                R.id.nav_change_pass -> replaceFragment(ChangePasswordFragment(), "Đổi mật khẩu")

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
            replaceFragment(BookshelfFragment(), "Tủ sách")
            navView.setCheckedItem(R.id.nav_home)
        }
    }

    private fun replaceFragment(fragment: Fragment, title: String) {
        supportActionBar?.title = title
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        if (fragment !is BookshelfFragment) {
            transaction.addToBackStack(null)
        }
        transaction.commit()
    }

    private fun updateTitleAfterPop() {
        // Simple logic to restore title based on current fragment
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        val title = when (currentFragment) {
            is BookshelfFragment -> "Tủ sách"
            is MainSearchFragment -> "Tìm kiếm"
            is UploadBookFragment -> "Upload Sách"
            is RegisterSellerFragment -> "Đăng ký bán hàng"
            is AdminNotificationFragment -> "Duyệt đơn"
            is UserListFragment -> "Danh sách người dùng"
            is SystemLogFragment -> "Hệ thống Log"
            is ChangePasswordFragment -> "Đổi mật khẩu"
            else -> "FinalLib"
        }
        supportActionBar?.title = title
    }

    private fun updateNavHeader() {
        val user = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()

        if (user != null) {
            val headerView = navView.getHeaderView(0)
            val tvName = headerView.findViewById<TextView>(R.id.tv_header_name)
            val tvRole = headerView.findViewById<TextView>(R.id.tv_header_role)

            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val fullName = document.getString("fullName")
                        val role = document.getString("role") ?: "User"
                        tvName.text = fullName ?: "Xin chào!"
                        tvRole.text = "[$role]"
                    }
                }
                .addOnFailureListener {
                    tvName.text = "Khách"
                    tvRole.text = ""
                }
        }
    }

    private fun checkUserRole() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    val role = document.getString("role")
                    val menu = navView.menu

                    menu.findItem(R.id.nav_logs)?.isVisible = false
                    menu.findItem(R.id.nav_admin_noti)?.isVisible = false
                    menu.findItem(R.id.nav_register_seller)?.isVisible = false
                    menu.findItem(R.id.nav_upload_book)?.isVisible = false

                    when (role) {
                        "Admin" -> {
                            menu.findItem(R.id.nav_logs)?.isVisible = true
                            menu.findItem(R.id.nav_admin_noti)?.isVisible = true
                            menu.findItem(R.id.nav_upload_book)?.isVisible = true
                        }
                        "Seller" -> {
                            menu.findItem(R.id.nav_upload_book)?.isVisible = true
                        }
                        "User" -> {
                            menu.findItem(R.id.nav_register_seller)?.isVisible = true
                        }
                    }
                }
        }
    }
}
