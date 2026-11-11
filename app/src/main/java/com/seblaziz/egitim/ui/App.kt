package com.seblaziz.egitim.ui

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.seblaziz.egitim.data.Library
import com.seblaziz.egitim.data.Repository

val LocalRepo = staticCompositionLocalOf<Repository> { error("repo yok") }
val LocalLibrary = staticCompositionLocalOf<Library> { error("library yok") }
val LocalNav = staticCompositionLocalOf<NavHostController> { error("nav yok") }
/** Video tam ekran olduğunda WebView'in verdiği görünüm burada tutulur. */
val LocalFullscreen = staticCompositionLocalOf<MutableState<View?>> { error("fs yok") }

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    const val ABOUT = "about"
    fun grade(g: String) = "grade/$g"
    fun playlist(g: String, p: String) = "playlist/$g/$p"
    fun player(g: String, p: String, i: Int) = "player/$g/$p/$i"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Ana Sayfa", Icons.Outlined.Home),
    Tab(Routes.SEARCH, "Ara", Icons.Outlined.Search),
    Tab(Routes.FAVORITES, "Favoriler", Icons.Outlined.StarOutline),
    Tab(Routes.ABOUT, "Hakkında", Icons.Outlined.Info),
)

/** Tablet ve yatay telefonlarda yan menü, dikey telefonda alt menü. */
@Composable
fun isWide(): Boolean = LocalConfiguration.current.screenWidthDp >= 600

@Composable
fun App(repo: Repository, library: Library) {
    SeblazizTheme {
        val nav = rememberNavController()
        val fullscreen = remember { mutableStateOf<View?>(null) }
        CompositionLocalProvider(
            LocalRepo provides repo, LocalLibrary provides library,
            LocalNav provides nav, LocalFullscreen provides fullscreen,
        ) {
            Box(Modifier.fillMaxSize().background(Bg)) {
                Shell(nav)
                fullscreen.value?.let { v ->
                    AndroidView(factory = { v }, modifier = Modifier.fillMaxSize().background(Color.Black))
                }
            }
        }
    }
}

@Composable
private fun Shell(nav: NavHostController) {
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: Routes.HOME
    val current = tabs.firstOrNull { route == it.route }?.route
        ?: if (route.startsWith("grade") || route.startsWith("playlist") || route.startsWith("player")) Routes.HOME else null
    val go: (String) -> Unit = { r ->
        nav.navigate(r) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val wide = isWide()
    val isPlayer = route.startsWith("player")

    if (wide) {
        Row(Modifier.fillMaxSize()) {
            if (!isPlayer) {
                NavigationRail(containerColor = Bg, modifier = Modifier.statusBarsPadding()) {
                    Spacer(Modifier.height(12.dp))
                    tabs.forEach { t ->
                        NavigationRailItem(
                            selected = current == t.route, onClick = { go(t.route) },
                            icon = { Icon(t.icon, t.label) }, label = { Text(t.label) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Accent, selectedTextColor = Accent,
                                indicatorColor = Surface2,
                            ),
                        )
                    }
                }
            }
            Box(Modifier.weight(1f)) { Graph(nav) }
        }
    } else {
        Scaffold(
            containerColor = Bg,
            bottomBar = {
                if (!isPlayer) NavigationBar(containerColor = Surface1) {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = current == t.route, onClick = { go(t.route) },
                            icon = { Icon(t.icon, t.label) }, label = { Text(t.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Accent, selectedTextColor = Accent,
                                indicatorColor = Surface2,
                            ),
                        )
                    }
                }
            },
        ) { pad ->
            Box(Modifier.fillMaxSize().background(Bg).padding(pad)) { Graph(nav) }
        }
    }
}

@Composable
private fun Graph(nav: NavHostController) {
    NavHost(nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen() }
        composable(Routes.SEARCH) { SearchScreen() }
        composable(Routes.FAVORITES) { FavoritesScreen() }
        composable(Routes.ABOUT) { AboutScreen() }
        composable("grade/{g}") { GradeScreen(it.arguments?.getString("g").orEmpty()) }
        composable("playlist/{g}/{p}") {
            PlaylistScreen(it.arguments?.getString("g").orEmpty(), it.arguments?.getString("p").orEmpty())
        }
        composable("player/{g}/{p}/{i}") {
            PlayerScreen(
                it.arguments?.getString("g").orEmpty(),
                it.arguments?.getString("p").orEmpty(),
                it.arguments?.getString("i")?.toIntOrNull() ?: 0,
            )
        }
    }
}
