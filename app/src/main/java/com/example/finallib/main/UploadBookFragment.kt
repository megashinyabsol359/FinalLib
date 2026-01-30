package com.example.finallib.main

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.finallib.R
import com.example.finallib.model.Book
import com.example.finallib.opds.GridAutoFitLayoutManager
import com.example.finallib.search.BookSearchAdapter
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class UploadBookFragment : Fragment() {

    private lateinit var btnOpenUpload: Button
    private lateinit var tabLayout: TabLayout
    private lateinit var rvUploads: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var pbLoading: ProgressBar
    
    private var uploadDialog: UploadBookDialog? = null
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private lateinit var adapter: BookSearchAdapter

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileNameFromUri(it)
            val mimeType = requireContext().contentResolver.getType(it)
            if (mimeType?.startsWith("image/") == true) {
                uploadDialog?.setSelectedCover(it)
            } else {
                uploadDialog?.setSelectedFile(it, fileName)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_upload_book, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnOpenUpload = view.findViewById(R.id.btn_open_upload_dialog)
        tabLayout = view.findViewById(R.id.tab_layout_uploads)
        rvUploads = view.findViewById(R.id.rv_uploaded_books)
        tvEmpty = view.findViewById(R.id.tv_empty_uploads)
        pbLoading = view.findViewById(R.id.pb_loading_uploads)

        // Setup RecyclerView - Grid mode like Bookshelf
        adapter = BookSearchAdapter(emptyList())
        rvUploads.layoutManager = GridAutoFitLayoutManager(requireContext(), 120)
        rvUploads.adapter = adapter

        btnOpenUpload.setOnClickListener {
            showUploadDialog()
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                loadUploadedBooks(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        loadUploadedBooks(0)
    }

    private fun loadUploadedBooks(tabIndex: Int) {
        val userId = auth.currentUser?.uid ?: return
        val status = if (tabIndex == 0) "pending" else "approved"
        
        pbLoading.visibility = View.VISIBLE
        rvUploads.visibility = View.GONE
        tvEmpty.visibility = View.GONE

        // REMOVED orderBy to avoid FAILED_PRECONDITION error (missing index)
        // We will sort the results in memory instead.
        db.collection("books")
            .whereEqualTo("sellerId", userId)
            .whereEqualTo("status", status)
            .get()
            .addOnSuccessListener { documents ->
                pbLoading.visibility = View.GONE
                
                // Map to objects and sort by uploadedAt in memory
                val books = documents.toObjects(Book::class.java)
                    .sortedByDescending { it.uploadedAt }
                
                if (books.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = if (tabIndex == 0) 
                        "Không có sách nào đang chờ duyệt." 
                    else 
                        "Bạn chưa có sách nào được đăng thành công."
                } else {
                    adapter.updateList(books)
                    rvUploads.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                pbLoading.visibility = View.GONE
                tvEmpty.text = "Không thể tải dữ liệu. Vui lòng kiểm tra kết nối."
                tvEmpty.visibility = View.VISIBLE
            }
    }

    private fun showUploadDialog() {
        uploadDialog = UploadBookDialog(
            context = requireContext(),
            lifecycleScope = viewLifecycleOwner.lifecycleScope,
            fileLauncher = filePickerLauncher,
            onSuccess = {
                // Refresh "Pending" list after upload
                tabLayout.getTabAt(0)?.select()
                loadUploadedBooks(0)
            }
        )
        uploadDialog?.show()
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var fileName = "book_file"
        try {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
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
}
