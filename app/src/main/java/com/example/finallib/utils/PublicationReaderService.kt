package com.example.finallib.utils

import android.content.Context
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File

object PublicationReaderService {

    /**
     * Load Publication từ file path (có thể là file .enc)
     * Callback: Result<Pair<Publication, String?>>:
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
            val incomingFile = File(filePath)
            if (!incomingFile.exists()) {
                callback(Result.failure(Exception("File không tồn tại: $filePath")))
                return@withContext
            }

            var fileToOpen = incomingFile
            var decryptedTempPath: String? = null

            if (incomingFile.name.endsWith(".enc")) {
                // decrypt into cache (temporary) to avoid storing plaintext persistently
                val cacheDir = File(context.cacheDir, "books")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val decryptedFile = File(cacheDir, incomingFile.name.removeSuffix(".enc") + "_decrypted.epub")

                val ok = DRMManager.decryptFile(context, incomingFile, decryptedFile, userId)
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

            val asset = assetResult.getOrNull() ?: run {
                callback(Result.failure(Exception("Không lấy được asset từ file")))
                return@withContext
            }

            val publicationResult = opener.open(asset)
            publicationResult.onFailure { err ->
                callback(Result.failure(Exception("Không thể mở publication: $err")))
                return@withContext
            }

            val publication = publicationResult.getOrNull() ?: run {
                callback(Result.failure(Exception("Không thể tạo publication")))
                return@withContext
            }

            callback(Result.success(Pair(publication, decryptedTempPath)))
        } catch (e: Exception) {
            callback(Result.failure(e))
        }
    }

    fun openReaderInFragment(
        publication: Publication,
        fragmentManager: FragmentManager,
        containerId: Int
    ) {
        val fragment = org.readium.r2.navigator.epub.EpubNavigatorFactory().createFragment(publication)
        fragmentManager.beginTransaction()
            .replace(containerId, fragment)
            .commit()
    }
}
