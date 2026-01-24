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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- 1. IMPORT CHÍNH XÁC CHO READIUM 3.X ---
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubNavigatorFactory // <--- Class mới quan trọng
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

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

                val httpClient = DefaultHttpClient()

                val assetRetriever = AssetRetriever(context.contentResolver, httpClient)

                val parser = DefaultPublicationParser(context, httpClient, assetRetriever, pdfFactory = null)
                val opener = PublicationOpener(parser, contentProtections = emptyList())

                val assetResult = assetRetriever.retrieve(bookFile)

                assetResult.onFailure { error ->
                    withContext(Dispatchers.Main) { tvStatus.text = "Lỗi file: $error" }
                    return@launch
                }

                val asset = assetResult.getOrNull()!!

                val publicationResult = opener.open(asset, allowUserInteraction = false)

                publicationResult.onSuccess { publication ->
                    withContext(Dispatchers.Main) {
                        tvStatus.text = "Mở thành công: ${publication.metadata.title}"
                        openReader(publication)
                    }
                }.onFailure { error ->
                    withContext(Dispatchers.Main) {
                        tvStatus.text = "Lỗi mở sách: $error"
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

    private fun openReader(publication: Publication) {

        val navigatorFactory = EpubNavigatorFactory(publication)
        val fragmentFactory = navigatorFactory.createFragmentFactory(
            initialLocator = null,
            listener = null
        )

        parentFragmentManager.fragmentFactory = fragmentFactory

        val fragment = parentFragmentManager.fragmentFactory.instantiate(
            requireContext().classLoader,
            EpubNavigatorFragment::class.java.name
        )

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack("Reader")
            .commit()
    }
}