package com.seblaziz.egitim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seblaziz.egitim.data.CatalogIndex
import com.seblaziz.egitim.data.thumbOf

@Composable
fun HomeScreen() {
    val repo = LocalRepo.current
    val nav = LocalNav.current
    val library = LocalLibrary.current
    var refresh by remember { mutableIntStateOf(0) }
    val state by produceState<Result<CatalogIndex>?>(null, refresh) {
        value = runCatching { repo.index(forceRefresh = refresh > 0) }
    }
    val idx = state?.getOrNull()
    when {
        state == null -> Loading()
        idx == null -> ErrorBox("Katalog yüklenemedi. İnternet bağlantınızı kontrol edin.")
        else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Hero(idx) { refresh++ }
            }
            if (library.recents.isNotEmpty()) {
                item { SectionTitle("Kaldığın yerden devam et") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(library.recents.take(15), key = { it.videoId }) { r ->
                            Column(
                                Modifier.width(cardWidth()).clip(RoundedCornerShape(12.dp))
                                    .clickable { nav.navigate(Routes.player(r.gradeId, r.playlistId, r.index)) }.padding(4.dp),
                            ) {
                                Thumb(thumbOf(r.videoId), Modifier.fillMaxWidth())
                                Text(r.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
            }
            idx.levels.forEach { level ->
                item(key = "lv-" + level.id) {
                    SectionTitle(level.title, Modifier.padding(top = 8.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(level.grades, key = { it.id }) { g ->
                            GradeCard(g.title, thumbOf(g.cover), g.playlists) { nav.navigate(Routes.grade(g.id)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hero(idx: CatalogIndex, onRefresh: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF3A1D24), Bg)))
            .statusBarsPadding().padding(16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Accent))
                Text("  EĞİTİM SEBLAZİZ", fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, "Yenile", tint = TextDim) }
            }
            Spacer(Modifier.height(10.dp))
            Text("Sınırsızca Öğrenin", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("İlginizi Çeken Şeyi Bulun", fontSize = 20.sp, color = TextDim)
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.clip(RoundedCornerShape(12.dp)).background(Surface1).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🎓", fontSize = 22.sp)
                Column(Modifier.padding(start = 10.dp)) {
                    Text(idx.provider.name, fontWeight = FontWeight.Bold)
                    Text(idx.provider.slogan, color = TextDim, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun GradeCard(title: String, cover: String, playlists: Int, onClick: () -> Unit) {
    Column(Modifier.width(cardWidth()).clip(RoundedCornerShape(14.dp)).background(Surface1).clickable(onClick = onClick)) {
        Thumb(cover, Modifier.fillMaxWidth())
        Column(Modifier.padding(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$playlists oynatma listesi", color = TextDim, fontSize = 12.sp)
        }
    }
}
