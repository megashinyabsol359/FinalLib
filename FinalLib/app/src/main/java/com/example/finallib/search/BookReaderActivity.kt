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
 */
class BookReaderActivity : AppCompatActivity() {

    private var tempFilePath: String? = null
    private lateinit var bookTitle: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_reader)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_reader)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Nhận dữ liệu từ Intent
        bookTitle = intent.getStringExtra("bookTitle") ?: "Sách"
        tempFilePath = intent.getStringExtra("tempFilePath")

        supportActionBar?.title = bookTitle

        // Kiểm tra file có tồn tại không
        if (tempFilePath == null || !File(tempFilePath!!).exists()) {
            Toast.makeText(this, "Lỗi: File sách không được tìm thấy", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load và mở sách
        loadAndOpenBook(tempFilePath!!)
    }

    private fun loadAndOpenBook(filePath: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            PublicationReaderService.loadPublicationFromFile(
                context = this@BookReaderActivity,
                filePath = filePath,
                callback = { result ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        result.onSuccess { publication ->
                            Toast.makeText(
                                this@BookReaderActivity,
                                "Đang mở sách: ${publication.metadata.title}",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Mở reader
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
        if (tempFilePath != null) {
            val deleted = FileDownloadService.deleteFile(tempFilePath!!)
            if (deleted) {
                android.util.Log.d("BookReaderActivity", "Temp file deleted: $tempFilePath")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupTempFile()
    }
}

