package com.example.finallib.main

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.example.finallib.R
import com.example.finallib.model.Book
import com.example.finallib.model.Tag
import com.example.finallib.utils.CloudinaryUploadService
import com.example.finallib.utils.FirebaseService
import com.example.finallib.utils.TagAdapter
import com.example.finallib.utils.TagItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.bumptech.glide.Glide
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
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
    private lateinit var btnSelectCover: Button
    private lateinit var btnUpload: Button
    private lateinit var btnCancel: Button
    private lateinit var tvSelectedFile: TextView
    private lateinit var tvUploadStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var rvTags: RecyclerView
    private lateinit var tvNoTags: TextView
    private lateinit var ivBookCover: ImageView
    private lateinit var rgAccessibility: RadioGroup
    private lateinit var rbPublic: RadioButton
    private lateinit var rbPrivate: RadioButton
    private lateinit var tilPrice: TextInputLayout
    private lateinit var etPrice: TextInputEditText

    private var selectedFileUri: Uri? = null
    private var selectedFileName: String = ""
    private var selectedCoverUri: Uri? = null
    private var selectedTags: List<String> = emptyList()
    private var selectedAccessibility: String = "public"
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
        btnSelectCover = dialog.findViewById(R.id.btn_select_cover)
        btnUpload = dialog.findViewById(R.id.btn_upload)
        btnCancel = dialog.findViewById(R.id.btn_cancel)
        tvSelectedFile = dialog.findViewById(R.id.tv_selected_file)
        tvUploadStatus = dialog.findViewById(R.id.tv_upload_status)
        progressBar = dialog.findViewById(R.id.progress_bar)
        rvTags = dialog.findViewById(R.id.rv_tags)
        tvNoTags = dialog.findViewById(R.id.tv_no_tags)
        ivBookCover = dialog.findViewById(R.id.iv_book_cover)
        rgAccessibility = dialog.findViewById(R.id.rg_accessibility)
        rbPublic = dialog.findViewById(R.id.rb_public)
        rbPrivate = dialog.findViewById(R.id.rb_private)
        tilPrice = dialog.findViewById(R.id.til_price)
        etPrice = dialog.findViewById(R.id.et_price)

        // Setup FlexboxLayoutManager for Tags
        val layoutManager = FlexboxLayoutManager(context)
        layoutManager.flexDirection = FlexDirection.ROW
        layoutManager.flexWrap = FlexWrap.WRAP
        rvTags.layoutManager = layoutManager

        // Setup RadioGroup listener for accessibility
        rgAccessibility.setOnCheckedChangeListener { _, checkedId ->
            selectedAccessibility = when (checkedId) {
                R.id.rb_private -> {
                    tilPrice.visibility = View.VISIBLE
                    "private"
                }
                else -> {
                    tilPrice.visibility = View.GONE
                    "public"
                }
            }
        }

        // Set click listeners
        btnSelectFile.setOnClickListener {
            fileLauncher.launch("*/*")
        }

        btnSelectCover.setOnClickListener {
            fileLauncher.launch("image/*")
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
                        rvTags.visibility = View.GONE
                    } else {
                        tvNoTags.visibility = View.GONE
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

    fun setSelectedCover(uri: Uri?) {
        selectedCoverUri = uri
        if (uri != null) {
            Glide.with(context)
                .load(uri)
                .centerCrop()
                .into(ivBookCover)
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

        // Validate price for private books
        var price = 0.0
        if (selectedAccessibility == "private") {
            val priceStr = etPrice.text.toString().trim()
            if (priceStr.isEmpty()) {
                Toast.makeText(context, "Vui lòng nhập giá tiền cho sách Private", Toast.LENGTH_SHORT).show()
                return
            }
            price = try {
                priceStr.toDouble()
            } catch (e: Exception) {
                Toast.makeText(context, "Giá tiền không hợp lệ", Toast.LENGTH_SHORT).show()
                return
            }

            if (price <= 0) {
                Toast.makeText(context, "Giá tiền phải lớn hơn 0", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Disable buttons & show progress
        btnSelectFile.isEnabled = false
        btnSelectCover.isEnabled = false
        btnUpload.isEnabled = false
        progressBar.visibility = View.VISIBLE
        tvUploadStatus.text = "Đang upload file lên Cloudinary..."

        // Start upload
        lifecycleScope.launch(Dispatchers.IO) {
            val result = CloudinaryUploadService.uploadFile(
                context = context,
                fileUri = selectedFileUri!!,
                fileName = selectedFileName
            )

            lifecycleScope.launch(Dispatchers.Main) {
                result.onSuccess { uploadedUrl ->
                    // Nếu có ảnh bìa, upload ảnh bìa trước
                    if (selectedCoverUri != null) {
                        uploadCoverImage(uploadedUrl, price)
                    } else {
                        saveBookToDatabase(
                            title, author, description, selectedTags, uploadedUrl, "", selectedAccessibility, price
                        )
                    }
                }

                result.onFailure { error ->
                    tvUploadStatus.text = "Lỗi: ${error.message}"
                    progressBar.visibility = View.GONE
                    btnSelectFile.isEnabled = true
                    btnSelectCover.isEnabled = true
                    btnUpload.isEnabled = true
                    Toast.makeText(context, "Lỗi upload: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun uploadCoverImage(fileUrl: String, price: Double) {
        tvUploadStatus.text = "Đang upload ảnh bìa sách..."

        lifecycleScope.launch(Dispatchers.IO) {
            val result = CloudinaryUploadService.uploadFile(
                context = context,
                fileUri = selectedCoverUri!!,
                fileName = "cover_" + System.currentTimeMillis()
            )

            lifecycleScope.launch(Dispatchers.Main) {
                result.onSuccess { coverUrl ->
                    saveBookToDatabase(
                        etTitle.text.toString().trim(),
                        etAuthor.text.toString().trim(),
                        etDescription.text.toString().trim(),
                        selectedTags,
                        fileUrl,
                        coverUrl,
                        selectedAccessibility,
                        price
                    )
                }

                result.onFailure { error ->
                    tvUploadStatus.text = "Lỗi upload ảnh bìa: ${error.message}"
                    progressBar.visibility = View.GONE
                    btnSelectFile.isEnabled = true
                    btnSelectCover.isEnabled = true
                    btnUpload.isEnabled = true
                    Toast.makeText(context, "Lỗi upload ảnh: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveBookToDatabase(
        title: String,
        author: String,
        description: String,
        tags: List<String>,
        fileUrl: String,
        coverUrl: String = "",
        accessibility: String = "public",
        price: Double = 0.0
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
                    cover = coverUrl,
                    sellerId = userId,
                    uploadedBy = currentUser?.email ?: "",
                    accessibility = accessibility,
                    price = price
                )

                val result = FirebaseService.uploadBookData(newBook)

                lifecycleScope.launch(Dispatchers.Main) {
                    result.onSuccess { docId ->
                        tvUploadStatus.text = "✅ Upload thành công!"
                        progressBar.visibility = View.GONE
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

                    result.onFailure { error ->
                        tvUploadStatus.text = "Lỗi: ${error.message}"
                        progressBar.visibility = View.GONE
                        btnSelectFile.isEnabled = true
                        btnSelectCover.isEnabled = true
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
                    progressBar.visibility = View.GONE
                    btnSelectFile.isEnabled = true
                    btnSelectCover.isEnabled = true
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
