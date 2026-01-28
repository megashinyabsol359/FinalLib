package com.example.finallib.main

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.finallib.R
import com.example.finallib.library.LibraryFragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


import com.example.finallib.auth.LoginActivity

import com.example.finallib.auth.ChangePasswordFragment
import com.example.finallib.auth.RegisterSellerFragment
import com.example.finallib.admin.SystemLogFragment
import com.example.finallib.admin.AdminNotificationFragment
import com.example.finallib.admin.UserListFragment

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
                R.id.nav_home -> replaceFragment(LibraryFragment())

                // User: Đăng ký bán hàng
                R.id.nav_register_seller -> replaceFragment(RegisterSellerFragment())

                // Admin: Duyệt đơn
                R.id.nav_admin_noti -> replaceFragment(AdminNotificationFragment())

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
            replaceFragment(LibraryFragment())
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
                        }
                        "User" -> {
                            menu.findItem(R.id.nav_register_seller)?.isVisible = true
                        }
                    }
                }
        }
    }
}
