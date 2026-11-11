package com.seblaziz.egitim.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seblaziz.egitim.data.Grade
import com.seblaziz.egitim.data.SavedPlaylist

@Composable
fun PlaylistScreen(gradeId: String, playlistId: String) {
    val repo = LocalRepo.current
    val nav = LocalNav.current
    val library = LocalLibrary.current
    val state by produceState<Result<Grade>?>(null, gradeId) { value = runCatching { repo.grade(gradeId) } }
    val grade = state?.getOrNull()
    val pl = grade?.playlist(playlistId)

    Column(Modifier.statusBarsPadding()) {
        when {
            state == null -> Loading()
            pl == null || grade == null -> {
                TopBar("Liste", null, onBack = { nav.popBackStack() })
                ErrorBox("Bu liste artık mevcut değil.")
            }
            else -> {
                val fav = library.isFavorite(pl.id)
                TopBar(pl.title, "${grade.title} · ${pl.videos.size} video", onBack = { nav.popBackStack() }) {
                    IconButton(onClick = {
                        library.toggleFavorite(SavedPlaylist(grade.id, pl.id, pl.title, pl.cover, pl.videos.size))
                    }) {
                        Icon(if (fav) Icons.Filled.Star else Icons.Outlined.StarOutline, "Favori", tint = if (fav) Accent else TextDim)
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(if (isWide()) 380.dp else 320.dp),
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 24.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(Modifier.padding(8.dp)) {
                            Button(
                                onClick = { nav.navigate(Routes.player(grade.id, pl.id, 0)) },
                                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                            ) {
                                Icon(Icons.Filled.PlayArrow, null)
                                Text(" Tümünü oynat")
                            }
                        }
                    }
                    itemsIndexed(pl.videos, key = { _, v -> v.id }) { i, v ->
                        VideoRow(v.title, v.thumb, v.duration) { nav.navigate(Routes.player(grade.id, pl.id, i)) }
                    }
                }
            }
        }
    }
}
