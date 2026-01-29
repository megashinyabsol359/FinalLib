package com.example.finallib.library

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.finallib.R
import com.example.finallib.utils.FileUtils
import com.example.finallib.utils.PublicationReaderService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryFragment : Fragment(R.layout.fragment_library) {

    private lateinit var tvStatus: TextView
    private lateinit var btnImport: Button

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

                val bookFile = FileUtils.copyFileToInternalStorage(context, uri, "book.epub")

                PublicationReaderService.loadPublicationFromFile(context, bookFile.absolutePath) { result ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        result.onSuccess { publication ->
                            tvStatus.text = "Mở thành công: ${publication.metadata.title}"
                            PublicationReaderService.openReaderInFragment(
                                publication = publication,
                                fragmentManager = parentFragmentManager,
                                containerId = R.id.fragment_container
                            )
                        }
                        result.onFailure { error ->
                            tvStatus.text = "Lỗi: ${error.message}"
                        }
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
}