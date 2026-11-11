package com.seblaziz.egitim.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject

/** Favori listeler ve son izlenen videolar (cihazda saklanır). */
data class SavedPlaylist(val gradeId: String, val playlistId: String, val title: String, val cover: String, val count: Int)
data class RecentVideo(val gradeId: String, val playlistId: String, val index: Int, val videoId: String, val title: String)

class Library(context: Context) {
    private val prefs = context.getSharedPreferences("library", Context.MODE_PRIVATE)

    val favorites = mutableStateListOf<SavedPlaylist>().apply { addAll(readFavorites()) }
    val recents = mutableStateListOf<RecentVideo>().apply { addAll(readRecents()) }

    fun isFavorite(playlistId: String) = favorites.any { it.playlistId == playlistId }

    fun toggleFavorite(item: SavedPlaylist) {
        val i = favorites.indexOfFirst { it.playlistId == item.playlistId }
        if (i >= 0) favorites.removeAt(i) else favorites.add(0, item)
        prefs.edit().putString("favorites", JSONArray(favorites.map {
            JSONObject().put("g", it.gradeId).put("p", it.playlistId).put("t", it.title).put("c", it.cover).put("n", it.count)
        }).toString()).apply()
    }

    fun addRecent(item: RecentVideo) {
        recents.removeAll { it.videoId == item.videoId }
        recents.add(0, item)
        while (recents.size > 30) recents.removeAt(recents.lastIndex)
        prefs.edit().putString("recents", JSONArray(recents.map {
            JSONObject().put("g", it.gradeId).put("p", it.playlistId).put("i", it.index).put("v", it.videoId).put("t", it.title)
        }).toString()).apply()
    }

    private fun readFavorites(): List<SavedPlaylist> = runCatching {
        val a = JSONArray(prefs.getString("favorites", "[]"))
        List(a.length()) { a.getJSONObject(it).let { o ->
            SavedPlaylist(o.getString("g"), o.getString("p"), o.getString("t"), o.optString("c"), o.optInt("n"))
        } }
    }.getOrDefault(emptyList())

    private fun readRecents(): List<RecentVideo> = runCatching {
        val a = JSONArray(prefs.getString("recents", "[]"))
        List(a.length()) { a.getJSONObject(it).let { o ->
            RecentVideo(o.getString("g"), o.getString("p"), o.optInt("i"), o.getString("v"), o.optString("t"))
        } }
    }.getOrDefault(emptyList())
}
