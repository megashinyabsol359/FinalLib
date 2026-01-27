package com.example.finallib.main

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.example.finallib.R
import com.example.finallib.model.Book
import com.example.finallib.model.Tag
import com.example.finallib.utils.CloudinaryUploadService
import com.example.finallib.utils.FirebaseService
import com.example.finallib.utils.TagAdapter
import com.example.finallib.utils.TagItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UploadBookDialog(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val fileLauncher: ActivityResultLauncher<String>,
    private val onSuccess: (String) -> Unit
) {
    private val dialog: Dialog = Dialog(context)
    private lateinit var etTitle: TextInputEditText
    private lateinit var etAuthor: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var btnSelectFile: Button
    private lateinit var btnUpload: Button
    private lateinit var btnCancel: Button
    private lateinit var tvSelectedFile: TextView
    private lateinit var tvUploadStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var rvTags: RecyclerView
    private lateinit var tvNoTags: TextView

    private var selectedFileUri: Uri? = null
    private var selectedFileName: String = ""
    private var selectedTags: List<String> = emptyList()
    private var tagAdapter: TagAdapter? = null

    init {
        setupDialog()
        loadTags()
    }

    private fun setupDialog() {
        dialog.setContentView(R.layout.dialog_upload_book_new)
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.95).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Bind views
        etTitle = dialog.findViewById(R.id.et_title)
        etAuthor = dialog.findViewById(R.id.et_author)
        etDescription = dialog.findViewById(R.id.et_description)
        btnSelectFile = dialog.findViewById(R.id.btn_select_file)
        btnUpload = dialog.findViewById(R.id.btn_upload)
        btnCancel = dialog.findViewById(R.id.btn_cancel)
        tvSelectedFile = dialog.findViewById(R.id.tv_selected_file)
        tvUploadStatus = dialog.findViewById(R.id.tv_upload_status)
        progressBar = dialog.findViewById(R.id.progress_bar)
        rvTags = dialog.findViewById(R.id.rv_tags)
        tvNoTags = dialog.findViewById(R.id.tv_no_tags)

        // Setup RecyclerView
        rvTags.layoutManager = LinearLayoutManager(context)

        // Set click listeners
        btnSelectFile.setOnClickListener {
            fileLauncher.launch("*/*")
        }

        btnUpload.setOnClickListener {
            performUpload()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun loadTags() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("tags").get().await()

                val tags = mutableListOf<TagItem>()
                for (doc in snapshot.documents) {
                    val tag = doc.toObject(Tag::class.java)
                    if (tag != null) {
                        tags.add(TagItem(doc.id, tag.label))
                    }
                }

                lifecycleScope.launch(Dispatchers.Main) {
                    if (tags.isEmpty()) {
                        tvNoTags.text = "Không có tags trong hệ thống"
                        rvTags.visibility = android.view.View.GONE
                    } else {
                        tvNoTags.visibility = android.view.View.GONE
                        tagAdapter = TagAdapter(tags) { selectedTagsList ->
                            selectedTags = selectedTagsList
                        }
                        rvTags.adapter = tagAdapter
                    }
                }
            } catch (e: Exception) {
                lifecycleScope.launch(Dispatchers.Main) {
                    tvNoTags.text = "Lỗi tải tags: ${e.message}"
                }
            }
        }
    }

    fun setSelectedFile(uri: Uri?, fileName: String) {
        selectedFileUri = uri
        selectedFileName = fileName
        if (uri != null) {
            tvSelectedFile.text = "✅ $fileName"
            tvSelectedFile.setTextColor(context.resources.getColor(android.R.color.holo_green_dark))
        }
    }

    private fun performUpload() {
        // Validate input
        val title = etTitle.text.toString().trim()
        val author = etAuthor.text.toString().trim()
        val description = etDescription.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(context, "Vui lòng nhập tên sách", Toast.LENGTH_SHORT).show()
            return
        }

        if (author.isEmpty()) {
            Toast.makeText(context, "Vui lòng nhập tác giả", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isEmpty()) {
            Toast.makeText(context, "Vui lòng nhập mô tả sách", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedTags.isEmpty()) {
            Toast.makeText(context, "Vui lòng chọn ít nhất 1 tag", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedFileUri == null) {
            Toast.makeText(context, "Vui lòng chọn file sách", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable buttons & show progress
        btnSelectFile.isEnabled = false
        btnUpload.isEnabled = false
        progressBar.visibility = android.view.View.VISIBLE
        tvUploadStatus.text = "Đang upload file lên Cloudinary..."

        // Start upload
        lifecycleScope.launch(Dispatchers.IO) {
            val result = CloudinaryUploadService.uploadFile(
                context = context,
                fileUri = selectedFileUri!!,
                fileName = selectedFileName
            )

            result.onSuccess { uploadedUrl ->
                saveBookToDatabase(title, author, description, selectedTags, uploadedUrl)
            }

            result.onFailure { error ->
                lifecycleScope.launch(Dispatchers.Main) {
                    tvUploadStatus.text = "Lỗi: ${error.message}"
                    progressBar.visibility = android.view.View.GONE
                    btnSelectFile.isEnabled = true
                    btnUpload.isEnabled = true
                    Toast.makeText(context, "Lỗi upload: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveBookToDatabase(
        title: String,
        author: String,
        description: String,
        tags: List<String>,
        fileUrl: String
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                val userId = currentUser?.uid ?: ""

                val newBook = Book(
                    title = title,
                    author = author,
                    description = description,
                    url = fileUrl,
                    status = "pending",
                    uploadedAt = System.currentTimeMillis(),
                    tags = tags,
                    language = "Tiếng Việt",
                    cover = "",
                    sellerId = userId,
                    uploadedBy = currentUser?.email ?: ""
                )

                val result = FirebaseService.uploadBookData(newBook)

                result.onSuccess { docId ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        tvUploadStatus.text = "✅ Upload thành công!"
                        progressBar.visibility = android.view.View.GONE
                        Toast.makeText(
                            context,
                            "Sách đã được upload thành công",
                            Toast.LENGTH_LONG
                        ).show()
                        onSuccess(docId)

                        // Đóng dialog sau 1.5 giây
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            dismiss()
                        }, 1500)
                    }
                }

                result.onFailure { error ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        tvUploadStatus.text = "Lỗi: ${error.message}"
                        progressBar.visibility = android.view.View.GONE
                        btnSelectFile.isEnabled = true
                        btnUpload.isEnabled = true
                        Toast.makeText(
                            context,
                            "Lỗi: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                lifecycleScope.launch(Dispatchers.Main) {
                    tvUploadStatus.text = "Lỗi: ${e.message}"
                    progressBar.visibility = android.view.View.GONE
                    btnSelectFile.isEnabled = true
                    btnUpload.isEnabled = true
                }
            }
        }
    }

    fun show() {
        dialog.show()
    }

    fun dismiss() {
        dialog.dismiss()
    }
}
