package com.example.finallib.utils

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.example.finallib.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File

/**
 * PublicationReaderService - Tái sử dụng logic mở sách từ file path
 * Dùng cho cả Fragment (LibraryFragment) và Activity (BookReaderActivity)
 */
object PublicationReaderService {

    /**
     * Load Publication từ file path
     * @param context Android context
     * @param filePath Đường dẫn file sách
     * @param callback Callback khi Publication được load thành công/thất bại
     */
    suspend fun loadPublicationFromFile(
        context: Context,
        filePath: String,
        callback: (Result<Publication>) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val bookFile = File(filePath)
            
            if (!bookFile.exists()) {
                callback(Result.failure(Exception("File không tồn tại: $filePath")))
                return@withContext
            }

            val httpClient = DefaultHttpClient()
            val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
            val parser = DefaultPublicationParser(context, httpClient, assetRetriever, pdfFactory = null)
            val opener = PublicationOpener(parser, contentProtections = emptyList())

            val assetResult = assetRetriever.retrieve(bookFile)

            assetResult.onFailure { error ->
                callback(Result.failure(Exception("Lỗi đọc file: $error")))
                return@withContext
            }

            val asset = assetResult.getOrNull()
                ?: run {
                    callback(Result.failure(Exception("Asset null")))
                    return@withContext
                }

            val publicationResult = opener.open(asset, allowUserInteraction = false)

            publicationResult.onSuccess { publication ->
                callback(Result.success(publication))
            }.onFailure { error ->
                callback(Result.failure(Exception("Lỗi mở sách: $error")))
            }

        } catch (e: Exception) {
            callback(Result.failure(e))
        }
    }

    /**
     * Mở Publication sử dụng EpubNavigatorFragment trong Fragment
     * @param publication Publication object
     * @param fragmentManager FragmentManager từ Fragment
     * @param containerId ID của container view
     */
    fun openReaderInFragment(
        publication: Publication,
        fragmentManager: FragmentManager,
        containerId: Int = R.id.fragment_container
    ) {
        try {
            val navigatorFactory = EpubNavigatorFactory(publication)
            val fragmentFactory = navigatorFactory.createFragmentFactory(
                initialLocator = null,
                listener = null
            )

            fragmentManager.fragmentFactory = fragmentFactory

            val fragment = fragmentManager.fragmentFactory.instantiate(
                fragmentManager.fragments.firstOrNull()?.requireContext()?.classLoader
                    ?: ClassLoader.getSystemClassLoader(),
                EpubNavigatorFragment::class.java.name
            )

            fragmentManager.beginTransaction()
                .replace(containerId, fragment)
                .addToBackStack("Reader")
                .commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Lấy thông tin sách từ Publication
     */
    fun getPublicationInfo(publication: Publication): Map<String, String> {
        return mapOf(
            "title" to (publication.metadata.title ?: "Unknown"),
            "author" to (publication.metadata.authors.firstOrNull()?.name ?: "Unknown"),
            "language" to (publication.metadata.languages.firstOrNull() ?: "Unknown")
        )
    }
}
