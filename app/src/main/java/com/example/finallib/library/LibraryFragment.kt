package com.example.finallib.library

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.finallib.R
import com.example.finallib.main.Application
import com.example.finallib.reader.ReaderActivityContract
import org.readium.r2.shared.util.toUrl
import org.readium.r2.shared.util.AbsoluteUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.example.finallib.utils.FileUtils

class LibraryFragment : Fragment(R.layout.fragment_library) {

    private lateinit var tvStatus: TextView
    private lateinit var btnImport: Button

    private val app: Application
        get() = requireContext().applicationContext as Application

    private val filePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { processBookImport(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvStatus = view.findViewById(R.id.tv_status)
        btnImport = view.findViewById(R.id.btn_import)

        btnImport.setOnClickListener {
            filePicker.launch("application/epub+zip")
        }
    }

    private fun processBookImport(uri: Uri) {
        tvStatus.text = "Đang xử lý..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val context = requireContext()
                val bookFile = FileUtils.copyFileToInternalStorage(context, uri, "temp_import.epub")
                val url = bookFile.toUrl()

                val result = app.bookshelf.addPublication(url as AbsoluteUrl)

                withContext(Dispatchers.Main) {
                    result.onSuccess { bookId ->
                        tvStatus.text = "Nhập thành công!"
                        openReader(bookId)
                    }.onFailure { error ->
                        tvStatus.text = "Lỗi: ${error.message}"
                        Toast.makeText(context, "Lỗi nhập sách: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvStatus.text = "Lỗi hệ thống: ${e.message}"
                    e.printStackTrace()
                }
            }
        }
    }

    private fun openReader(bookId: Long) {
        lifecycleScope.launch {
            app.readerRepository.open(bookId).onSuccess {
                val intent = ReaderActivityContract().createIntent(requireContext(), ReaderActivityContract.Arguments(bookId))
                startActivity(intent)
            }.onFailure {
                Toast.makeText(requireContext(), "Không thể mở sách", Toast.LENGTH_SHORT).show()
            }
        }
    }
}