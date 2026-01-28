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

        // Ánh xạ View
        recyclerView = view.findViewById(R.id.recycler_users)
        searchView = view.findViewById(R.id.search_view)
        spinnerRole = view.findViewById(R.id.spinner_role)

        // Cấu hình RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Khởi tạo Adapter với filteredList (ban đầu rỗng)
        adapter = UserAdapter(filteredList, currentUserId) { selectedUser ->
            showUserDetailDialog(selectedUser)
        }
        recyclerView.adapter = adapter

        // 1. Cấu hình Spinner (Bộ lọc Role)
        setupSpinner()

        // 2. Cấu hình SearchView (Tìm kiếm)
        setupSearchView()

        // 3. Tải dữ liệu
        loadUsers()
    }

    private fun setupSpinner() {
        // Tạo danh sách lựa chọn
        val roles = listOf("Tất cả", "User", "Seller", "Admin")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roles)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = spinnerAdapter

        // Bắt sự kiện chọn item
        spinnerRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentRoleFilter = roles[position] // Lưu lại lựa chọn ("Tất cả", "User"...)
                filterUsers() // Gọi hàm lọc
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
                currentSearchText = newText ?: "" // Lưu lại từ khóa
                filterUsers() // Gọi hàm lọc mỗi khi gõ chữ
                return true
            }
        })
    }

    // --- HÀM LỌC LOGIC (Quan trọng nhất) ---
    private fun filterUsers() {
        filteredList.clear()

        val textToSearch = currentSearchText.lowercase().trim()

        for (user in originalList) {
            // 1. Kiểm tra Role (Nếu chọn "Tất cả" thì bỏ qua kiểm tra role)
            val matchRole = if (currentRoleFilter == "Tất cả") true else (user.role == currentRoleFilter)

            // 2. Kiểm tra Tên hoặc Email
            val matchName = user.fullName.lowercase().contains(textToSearch)
            val matchEmail = user.email.lowercase().contains(textToSearch)

            // 3. Nếu thỏa mãn CẢ HAI điều kiện -> Thêm vào list
            if (matchRole && (matchName || matchEmail)) {
                filteredList.add(user)
            }
        }

        // Cập nhật lên giao diện
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
                // Tải xong thì lọc lần đầu (để hiện full danh sách)
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
            ID: ${user.id}
        """.trimIndent()
        builder.setMessage(info)
        builder.setPositiveButton("Đóng") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }
}