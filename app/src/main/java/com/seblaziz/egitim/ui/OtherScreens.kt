package com.seblaziz.egitim.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seblaziz.egitim.BuildConfig
import com.seblaziz.egitim.data.CatalogIndex
import com.seblaziz.egitim.data.Grade
import java.util.Locale

private val tr = Locale("tr", "TR")
private fun norm(s: String) = s.lowercase(tr)

private sealed interface Hit {
    data class P(val grade: Grade, val playlistId: String, val title: String, val cover: String, val count: Int) : Hit
    data class V(val grade: Grade, val playlistId: String, val index: Int, val title: String, val thumb: String, val duration: String) : Hit
}

@Composable
fun SearchScreen() {
    val repo = LocalRepo.current
    val nav = LocalNav.current
    var q by rememberSaveable { mutableStateOf("") }
    val all by produceState<List<Grade>?>(null) { value = repo.allGrades() }

    val hits = remember(q, all) {
        val words = norm(q).split(' ').filter { it.isNotBlank() }
        val grades = all
        if (words.isEmpty() || grades == null) emptyList() else buildList<Hit> {
            for (g in grades) for (s in g.shelves) for (p in s.playlists) {
                val pt = norm(p.title + " " + g.title)
                if (words.all { pt.contains(it) }) add(Hit.P(g, p.id, p.title, p.cover, p.videos.size))
                p.videos.forEachIndexed { i, v ->
                    val vt = norm(v.title + " " + g.title)
                    if (words.all { vt.contains(it) }) add(Hit.V(g, p.id, i, v.title, v.thumb, v.duration))
                }
                if (size > 300) return@buildList
            }
        }.distinctBy { if (it is Hit.V) "v" + it.title else "p" + (it as Hit.P).playlistId }
    }

    Column(Modifier.statusBarsPadding()) {
        TopBar("Ara", "Konu, ders veya sınıf yazın (ör. \"6. sınıf kesirler\")", onBack = null)
        OutlinedTextField(
            value = q, onValueChange = { q = it }, singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            placeholder = { Text("Uygulamaları ve dersleri arayın…") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        when {
            all == null -> Loading()
            q.isBlank() -> ErrorBox("Aramak istediğiniz konuyu yazın.")
            hits.isEmpty() -> ErrorBox("Sonuç bulunamadı.")
            else -> LazyColumn(contentPadding = PaddingValues(8.dp)) {
                items(hits) { h ->
                    when (h) {
                        is Hit.P -> VideoRow("📂 ${h.title}", h.cover, "${h.count} video") {
                            nav.navigate(Routes.playlist(h.grade.id, h.playlistId))
                        }
                        is Hit.V -> Column {
                            VideoRow(h.title, h.thumb, h.duration) { nav.navigate(Routes.player(h.grade.id, h.playlistId, h.index)) }
                            Text(h.grade.title, color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(start = 160.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen() {
    val library = LocalLibrary.current
    val nav = LocalNav.current
    Column(Modifier.statusBarsPadding()) {
        TopBar("Favoriler", "Yıldızladığınız listeler", onBack = null)
        if (library.favorites.isEmpty()) {
            ErrorBox("Henüz favori yok. Bir listeyi açıp ☆ simgesine dokunun.")
        } else {
            LazyVerticalGrid(columns = GridCells.Adaptive(cardWidth()), contentPadding = PaddingValues(12.dp)) {
                items(library.favorites, key = { it.playlistId }) { f ->
                    PlaylistCard(f.title, f.cover, f.count, onClick = { nav.navigate(Routes.playlist(f.gradeId, f.playlistId)) })
                }
            }
        }
    }
}

@Composable
fun AboutScreen() {
    val repo = LocalRepo.current
    val context = LocalContext.current
    val idx by produceState<CatalogIndex?>(null) { value = runCatching { repo.index() }.getOrNull() }
    Column(Modifier.statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("EĞİTİM SEBLAZİZ", fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text("Sürüm ${BuildConfig.VERSION_NAME}", color = TextDim)
        Spacer(Modifier.height(20.dp))
        idx?.provider?.let { p ->
            Text(p.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(p.slogan, color = Accent)
            Spacer(Modifier.height(8.dp))
            Text(p.about, lineHeight = 21.sp)
            Spacer(Modifier.height(12.dp))
            Text("Daha fazla bilgi için:", color = TextDim)
            Text(p.website, color = Accent, modifier = Modifier.clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(p.website)))
            })
            Spacer(Modifier.height(4.dp))
            Text("Sosyal medya: ${p.social}", color = TextDim)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Videolar tonguç AKADEMİ'nin herkese açık YouTube kanallarından, YouTube'un resmî oynatıcısıyla gösterilir. " +
                "Tüm içerik hakları sahiplerine aittir. Ders listesi düzenli olarak otomatik güncellenir.",
            color = TextDim, fontSize = 13.sp, lineHeight = 19.sp,
        )
        idx?.updatedAt?.takeIf { it.length >= 10 }?.let {
            Spacer(Modifier.height(8.dp))
            Text("Katalog güncellemesi: ${it.substring(0, 10)}", color = TextDim, fontSize = 12.sp)
        }
    }
}
