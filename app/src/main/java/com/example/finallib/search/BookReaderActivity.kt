package com.example.finallib.search

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.example.finallib.R
import com.example.finallib.utils.FileDownloadService
import com.example.finallib.utils.PublicationReaderService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * BookReaderActivity - Màn hình đọc sách
 * Sử dụng PublicationReaderService với Readium library
 *
 * Lưu ý: filePath truyền vào là ��ường dẫn tới file mã hóa (.enc) lưu persistent.
 * Activity nhận thêm extra "userId" (Firebase uid) để giải mã offline.
 * Plaintext sẽ được giải mã sang file tạm trong cache và bị xóa khi thoát.
 */
class BookReaderActivity : AppCompatActivity() {

    // encFilePath: đường dẫn tới file mã hóa (.enc) lưu persistent (filesDir)
    private var encFilePath: String? = null

    // tempDecryptedFilePath: đường dẫn file plaintext tạm được giải mã (cache) — sẽ bị xóa khi thoát
    private var tempDecryptedFilePath: String? = null

    private var currentUserId: String? = null
    private lateinit var bookTitle: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_reader)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_reader)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Nhận dữ liệu từ Intent
        bookTitle = intent.getStringExtra("bookTitle") ?: "Sách"
        encFilePath = intent.getStringExtra("tempFilePath") // encrypted file path
        currentUserId = intent.getStringExtra("userId")

        supportActionBar?.title = bookTitle

        // Kiểm tra file và userId
        if (encFilePath == null || !File(encFilePath!!).exists()) {
            Toast.makeText(this, "Lỗi: File sách không được tìm thấy", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (currentUserId == null) {
            Toast.makeText(this, "Lỗi: userId không hợp lệ", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load và mở sách (sẽ giải mã tạm nếu cần)
        loadAndOpenBook(encFilePath!!, currentUserId!!)
    }

    private fun loadAndOpenBook(filePath: String, userId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            PublicationReaderService.loadPublicationFromFile(
                context = this@BookReaderActivity,
                filePath = filePath,
                userId = userId,
                callback = { result ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        result.onSuccess { pair ->
                            val publication = pair.first
                            val decryptedPath = pair.second

                            // Lưu đường dẫn file plaintext tạm để xóa sau khi đọc
                            tempDecryptedFilePath = decryptedPath

                            Toast.makeText(
                                this@BookReaderActivity,
                                "Đang mở sách: ${publication.metadata.title}",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Mở reader trong fragment
                            PublicationReaderService.openReaderInFragment(
                                publication = publication,
                                fragmentManager = supportFragmentManager,
                                containerId = R.id.reader_container
                            )
                        }
                        result.onFailure { error ->
                            Toast.makeText(
                                this@BookReaderActivity,
                                "Lỗi mở sách: ${error.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }
                    }
                }
            )
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        cleanupTempFile()
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        cleanupTempFile()
    }

    private fun cleanupTempFile() {
        // Chỉ xóa file plaintext tạm đã giải mã (nằm trong cache)
        if (tempDecryptedFilePath != null) {
            val deletedDec = FileDownloadService.deleteFile(tempDecryptedFilePath!!)
            if (deletedDec) {
                android.util.Log.d("BookReaderActivity", "Decrypted temp file deleted: $tempDecryptedFilePath")
            } else {
                android.util.Log.w("BookReaderActivity", "Failed to delete decrypted temp file: $tempDecryptedFilePath")
            }
            tempDecryptedFilePath = null
        }

        // NOT: không xóa encFilePath ở đây để giữ khả năng đọc offline.
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupTempFile()
    }
}
