/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package com.example.finallib.main

import android.content.Context
import android.os.Build
import android.os.StrictMode
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.material.color.DynamicColors
import java.io.File
import java.util.Properties
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import com.example.finallib.BuildConfig.DEBUG
import com.example.finallib.data.BookRepository
import com.example.finallib.data.db.AppDatabase
import com.example.finallib.domain.Bookshelf
import com.example.finallib.domain.CoverStorage
import com.example.finallib.domain.PublicationRetriever
import com.example.finallib.reader.ReaderRepository
import com.example.finallib.utils.tryOrLog
import timber.log.Timber

class Application : android.app.Application() {

    lateinit var readium: Readium
        private set

    lateinit var storageDir: File

    lateinit var bookRepository: BookRepository
        private set

    lateinit var bookshelf: Bookshelf
        private set

    lateinit var readerRepository: ReaderRepository
        private set

    private val coroutineScope: CoroutineScope =
        MainScope()

    private val Context.navigatorPreferences: DataStore<Preferences>
        by preferencesDataStore(name = "navigator-preferences")

    override fun onCreate() {
        if (DEBUG) {
            enableStrictMode()
            Timber.plant(Timber.DebugTree())
        }

        super.onCreate()

        DynamicColors.applyToActivitiesIfAvailable(this)

        readium = Readium(this)

        storageDir = computeStorageDir()

        val database = AppDatabase.getDatabase(this)

        bookRepository = BookRepository(database.booksDao())

        val downloadsDir = File(cacheDir, "downloads")

        // Cleans the download dir.
        tryOrLog { downloadsDir.delete() }

        val publicationRetriever =
            PublicationRetriever(
                context = applicationContext,
                assetRetriever = readium.assetRetriever,
                bookshelfDir = storageDir,
                tempDir = downloadsDir,
                httpClient = readium.httpClient,
                lcpService = readium.lcpService.getOrNull()
            )

        bookshelf =
            Bookshelf(
                bookRepository,
                CoverStorage(storageDir, httpClient = readium.httpClient),
                readium.publicationOpener,
                readium.assetRetriever,
                publicationRetriever
            )

        readerRepository = ReaderRepository(
            this@Application,
            readium,
            bookRepository,
            navigatorPreferences
        )
    }

    private fun computeStorageDir(): File {
        val properties = Properties()
        val inputStream = assets.open("configs/config.properties")
        properties.load(inputStream)
        val useExternalFileDir =
            properties.getProperty("useExternalFileDir", "false")!!.toBoolean()

        return File(
            if (useExternalFileDir) {
                getExternalFilesDir(null)?.path + "/"
            } else {
                filesDir?.path + "/"
            }
        )
    }

    /**
     * Strict mode will log violation of VM and threading policy.
     * Use it to make sure the app doesn't do too much work on the main thread.
     */
    private fun enableStrictMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return
        }

        val executor = Executors.newSingleThreadExecutor()
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyListener(executor) { violation ->
                    Timber.e(violation, "Thread policy violation")
                }
//                .penaltyDeath()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyListener(executor) { violation ->
                    Timber.e(violation, "VM policy violation")
                }
//                .penaltyDeath()
                .build()
        )
    }
}
