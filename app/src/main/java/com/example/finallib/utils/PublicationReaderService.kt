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
 *
 * NOTE:
 * - Nếu input là file mã hóa (theo convention: *.enc) thì sẽ giải mã sang file tạm trong cache
 *   trước khi mở. Khi giải mã xảy ra, callback trả về Pair(publication, decryptedPath).
 * - Nếu file không mã hóa, callback trả về Pair(publication, null).
 */
object PublicationReaderService {

    /**
     * Load Publication từ file path
     * @param context Android context
     * @param filePath Đường dẫn file sách (có thể là .enc)
     * @param userId UID của user hiện tại — dùng để giải mã file nếu cần
     * @param callback Callback khi Publication được load thành công/thất bại
     *
     * Callback trả về Result<Pair<Publication, String?>>:
     *  - Pair.first = Publication
     *  - Pair.second = đường dẫn file plaintext đã giải mã (nếu có) -> caller cần xóa khi xong
     */
    suspend fun loadPublicationFromFile(
        context: Context,
        filePath: String,
        userId: String,
        callback: (Result<Pair<Publication, String?>>) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val incoming = File(filePath)

            if (!incoming.exists()) {
                callback(Result.failure(Exception("File không tồn tại: $filePath")))
                return@withContext
            }

            var fileToOpen = incoming
            var decryptedTempPath: String? = null

            // Nếu file được mã hóa theo convention .enc => giải mã vào cache (tạm)
            if (incoming.name.endsWith(".enc")) {
                val cacheDir = File(context.cacheDir, "books")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val decryptedFile = File(cacheDir, incoming.name.removeSuffix(".enc") + "_decrypted.epub")

                val ok = DRMManager.decryptFile(context, incoming, decryptedFile, userId)
                if (!ok || !decryptedFile.exists()) {
                    callback(Result.failure(Exception("DRM: Không thể giải mã file (có thể user không hợp lệ)")))
                    return@withContext
                }

                fileToOpen = decryptedFile
                decryptedTempPath = decryptedFile.absolutePath
            }

            val httpClient = DefaultHttpClient()
            val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
            val parser = DefaultPublicationParser(context, httpClient, assetRetriever, pdfFactory = null)
            val opener = PublicationOpener(parser, contentProtections = emptyList())

            val assetResult = assetRetriever.retrieve(fileToOpen)
            assetResult.onFailure { error ->
                callback(Result.failure(Exception("Lỗi đọc file: $error")))
                return@withContext
            }

            val asset = assetResult.getOrNull()
                ?: run {
                    callback(Result.failure(Exception("Asset null")))
                    return@withContext
                }

            // Preserve previous behavior of opening publication without interactive prompts
            val publicationResult = opener.open(asset, allowUserInteraction = false)

            publicationResult.onSuccess { publication ->
                callback(Result.success(Pair(publication, decryptedTempPath)))
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
     * @param fragmentManager FragmentManager từ Fragment/Activity
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
