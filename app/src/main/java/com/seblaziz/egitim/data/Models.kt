package com.seblaziz.egitim.data

import org.json.JSONObject

data class Provider(
    val name: String,
    val slogan: String,
    val about: String,
    val website: String,
    val social: String,
)

data class GradeRef(val id: String, val title: String, val cover: String, val playlists: Int)
data class Level(val id: String, val title: String, val grades: List<GradeRef>)
data class CatalogIndex(val updatedAt: String, val provider: Provider, val levels: List<Level>)

data class Video(val id: String, val title: String, val duration: String) {
    val thumb: String get() = thumbOf(id)
}

data class Playlist(val id: String, val title: String, val videos: List<Video>) {
    val cover: String get() = videos.firstOrNull()?.thumb ?: ""
}

data class Shelf(val id: String, val title: String, val playlists: List<Playlist>)
data class Grade(val id: String, val title: String, val shelves: List<Shelf>) {
    fun playlist(id: String): Playlist? = shelves.asSequence().flatMap { it.playlists }.firstOrNull { it.id == id }
}

fun thumbOf(videoId: String) = if (videoId.isBlank()) "" else "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"

object Parser {
    fun index(text: String): CatalogIndex {
        val o = JSONObject(text)
        val p = o.getJSONObject("provider")
        val levels = o.getJSONArray("levels")
        return CatalogIndex(
            updatedAt = o.optString("updatedAt"),
            provider = Provider(
                p.optString("name"), p.optString("slogan"), p.optString("about"),
                p.optString("website"), p.optString("social"),
            ),
            levels = List(levels.length()) { i ->
                val l = levels.getJSONObject(i)
                val g = l.getJSONArray("grades")
                Level(l.getString("id"), l.getString("title"), List(g.length()) { j ->
                    val r = g.getJSONObject(j)
                    GradeRef(r.getString("id"), r.getString("title"), r.optString("cover"), r.optInt("playlists"))
                })
            },
        )
    }

    fun grade(text: String): Grade {
        val o = JSONObject(text)
        val shelves = o.getJSONArray("shelves")
        return Grade(o.getString("id"), o.getString("title"), List(shelves.length()) { i ->
            val s = shelves.getJSONObject(i)
            val pls = s.getJSONArray("playlists")
            Shelf(s.getString("id"), s.getString("title"), List(pls.length()) { j ->
                val p = pls.getJSONObject(j)
                val vs = p.getJSONArray("videos")
                Playlist(p.getString("id"), p.getString("title"), List(vs.length()) { k ->
                    val v = vs.getJSONObject(k)
                    Video(v.getString("id"), v.optString("title"), v.optString("duration"))
                })
            })
        })
    }
}
