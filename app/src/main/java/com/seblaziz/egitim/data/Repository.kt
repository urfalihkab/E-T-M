package com.seblaziz.egitim.data

import android.content.Context
import com.seblaziz.egitim.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Katalog kaynağı sırası: 1) internet (GitHub'daki güncel catalog/), 2) cihazdaki son indirilen kopya,
 * 3) uygulamanın içine gömülü kopya. Böylece içerik yeni APK kurmadan güncellenir, internetsiz de açılır.
 */
class Repository(private val context: Context) {
    private val cacheDir = File(context.filesDir, "catalog").apply { mkdirs() }
    private val grades = HashMap<String, Grade>()
    private var index: CatalogIndex? = null
    private val mutex = Mutex()

    suspend fun index(forceRefresh: Boolean = false): CatalogIndex = mutex.withLock {
        if (!forceRefresh) index?.let { return it }
        if (forceRefresh) grades.clear()
        load("index.json", forceRefresh, Parser::index).also { index = it }
    }

    suspend fun grade(id: String): Grade = mutex.withLock {
        grades[id] ?: load("grade-$id.json", false, Parser::grade).also { grades[id] = it }
    }

    suspend fun allGrades(): List<Grade> {
        val idx = index()
        return idx.levels.flatMap { it.grades }.mapNotNull { runCatching { grade(it.id) }.getOrNull() }
    }

    private suspend fun <T> load(name: String, preferNetwork: Boolean, parse: (String) -> T): T =
        withContext(Dispatchers.IO) {
            val cached = File(cacheDir, name)
            val tryNetwork = preferNetwork || !cached.exists() ||
                System.currentTimeMillis() - cached.lastModified() > 6 * 60 * 60 * 1000L
            if (tryNetwork) {
                runCatching { download(name) }.getOrNull()?.let { text ->
                    runCatching { parse(text) }.getOrNull()?.let { parsed ->
                        cached.writeText(text)
                        return@withContext parsed
                    }
                }
            }
            if (cached.exists()) runCatching { parse(cached.readText()) }.getOrNull()?.let { return@withContext it }
            parse(context.assets.open(name).bufferedReader().use { it.readText() })
        }

    private fun download(name: String): String? {
        val base = BuildConfig.CATALOG_URL
        if (base.isBlank()) return null
        val conn = URL(base + name).openConnection() as HttpURLConnection
        conn.connectTimeout = 6000
        conn.readTimeout = 15000
        return try {
            if (conn.responseCode == 200) conn.inputStream.bufferedReader().use { it.readText() } else null
        } finally {
            conn.disconnect()
        }
    }
}
