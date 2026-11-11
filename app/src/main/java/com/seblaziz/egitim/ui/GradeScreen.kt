package com.seblaziz.egitim.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seblaziz.egitim.data.Grade
import kotlinx.coroutines.launch

@Composable
fun GradeScreen(gradeId: String) {
    val repo = LocalRepo.current
    val nav = LocalNav.current
    val state by produceState<Result<Grade>?>(null, gradeId) { value = runCatching { repo.grade(gradeId) } }
    val grade = state?.getOrNull()
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(Modifier.statusBarsPadding()) {
        TopBar(grade?.title ?: "", "tonguç AKADEMİ", onBack = { nav.popBackStack() })
        when {
            state == null -> Loading()
            grade == null -> ErrorBox("Bu sınıfın içeriği yüklenemedi.")
            else -> {
                // TV'deki gibi üstte ders seçici: dokununca ilgili rafa kayar
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(grade.shelves, key = { _, s -> s.id }) { i, s ->
                        FilterChip(
                            selected = list.firstVisibleItemIndex == i,
                            onClick = { scope.launch { list.animateScrollToItem(i) } },
                            label = { Text(s.title) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Accent, selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                            ),
                        )
                    }
                }
                LazyColumn(state = list, contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(grade.shelves, key = { it.id }) { shelf ->
                        Column {
                            SectionTitle("${shelf.title}  ·  ${shelf.playlists.size}", Modifier.padding(top = 6.dp))
                            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(shelf.playlists, key = { it.id }) { p ->
                                    PlaylistCard(p.title, p.cover, p.videos.size, onClick = {
                                        nav.navigate(Routes.playlist(grade.id, p.id))
                                    }, modifier = Modifier.width(cardWidth()))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
