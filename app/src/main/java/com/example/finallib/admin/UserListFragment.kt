package com.example.finallib.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finallib.R
import com.example.finallib.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserListFragment : Fragment(R.layout.fragment_user_list) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var spinnerRole: Spinner

    private lateinit var adapter: UserAdapter

    // Hai danh sách riêng biệt
    private val originalList = ArrayList<User>() // Danh sách gốc (Full)
    private val filteredList = ArrayList<User>() // Danh sách đang hiển thị

    private var currentSearchText = ""
    private var currentRoleFilter = "Tất cả"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.supportActionBar?.title = "Quản lý người dùng"

        recyclerView = view.findViewById(R.id.recycler_users)
        searchView = view.findViewById(R.id.search_view)
        spinnerRole = view.findViewById(R.id.spinner_role)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Khởi tạo Adapter
        adapter = UserAdapter(filteredList, currentUserId) { selectedUser ->
            showUserDetailDialog(selectedUser)
        }
        recyclerView.adapter = adapter

        setupSpinner()

        setupSearchView()

        loadUsers()
    }

    private fun setupSpinner() {
        val roles = listOf("Tất cả", "User", "Seller", "Admin")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roles)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = spinnerAdapter

        spinnerRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentRoleFilter = roles[position]
                filterUsers()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchText = newText ?: ""
                filterUsers()
                return true
            }
        })
    }

    // Hàm Filter
    private fun filterUsers() {
        filteredList.clear()

        val textToSearch = currentSearchText.lowercase().trim()

        for (user in originalList) {
            val matchRole = if (currentRoleFilter == "Tất cả") true else (user.role == currentRoleFilter)

            val matchName = user.fullName.lowercase().contains(textToSearch)
            val matchEmail = user.email.lowercase().contains(textToSearch)

            if (matchRole && (matchName || matchEmail)) {
                filteredList.add(user)
            }
        }

        adapter.updateList(filteredList)
    }

    private fun loadUsers() {
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                originalList.clear()
                for (doc in documents) {
                    val user = doc.toObject(User::class.java)
                    originalList.add(user)
                }
                filterUsers()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showUserDetailDialog(user: User) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Thông tin chi tiết")
        val info = """
            Họ tên: ${user.fullName}
            Email: ${user.email}
            Vai trò: ${user.role}
        """.trimIndent()
        builder.setMessage(info)
        builder.setPositiveButton("Đóng") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }
}