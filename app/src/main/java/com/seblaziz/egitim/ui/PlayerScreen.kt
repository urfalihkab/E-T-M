package com.seblaziz.egitim.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.seblaziz.egitim.data.Grade
import com.seblaziz.egitim.data.RecentVideo

private const val BASE = "https://egitim.seblaziz.app/"

/** YouTube'un resmî IFrame oynatıcısı; WebView içinde, uygulamadan çıkmadan oynatır. */
private fun playerHtml(videoId: String) = """
<!doctype html><html><head>
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>html,body{margin:0;height:100%;background:#000;overflow:hidden}#p{position:absolute;inset:0;width:100%;height:100%}</style>
</head><body><div id="p"></div>
<script src="https://www.youtube.com/iframe_api"></script>
<script>
var player;
function onYouTubeIframeAPIReady(){
  player=new YT.Player('p',{videoId:'$videoId',
    playerVars:{autoplay:1,playsinline:1,rel:0,modestbranding:1,fs:1,hl:'tr',origin:'${BASE.trimEnd('/')}'},
    events:{onStateChange:function(e){ if(e.data===0) Android.ended(); },
            onError:function(e){ Android.error(e.data); }}});
}
function loadVid(id){ if(player&&player.loadVideoById){player.loadVideoById(id);} }
</script></body></html>
""".trimIndent()

class PlayerBridge(private val onEnded: () -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    @JavascriptInterface fun ended() { main.post(onEnded) }
    @JavascriptInterface fun error(code: Int) { main.post(onEnded) }
}

private fun Context.findActivity(): Activity? {
    var c = this
    while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }
    return null
}

@Composable
fun PlayerScreen(gradeId: String, playlistId: String, startIndex: Int) {
    val repo = LocalRepo.current
    val nav = LocalNav.current
    val library = LocalLibrary.current
    val state by produceState<Result<Grade>?>(null, gradeId) { value = runCatching { repo.grade(gradeId) } }
    val grade = state?.getOrNull()
    val pl = grade?.playlist(playlistId)

    if (state == null) { Loading(); return }
    if (pl == null || grade == null || pl.videos.isEmpty()) { ErrorBox("Video bulunamadı."); return }

    var index by rememberSaveable { mutableIntStateOf(startIndex.coerceIn(0, pl.videos.lastIndex)) }
    val video = pl.videos[index]

    LaunchedEffect(video.id) {
        library.addRecent(RecentVideo(grade.id, pl.id, index, video.id, video.title))
    }

    val next: () -> Unit = { if (index < pl.videos.lastIndex) index += 1 }
    val prev: () -> Unit = { if (index > 0) index -= 1 }
    val landscapeWide = LocalConfiguration.current.let { it.screenWidthDp > it.screenHeightDp && it.screenWidthDp >= 600 }
    val listState = rememberLazyListState()
    LaunchedEffect(index) { listState.animateScrollToItem(index) }

    val playerBox: @Composable (Modifier) -> Unit = { m ->
        YouTubePlayer(video.id, onEnded = { next() }, modifier = m)
    }
    val info: @Composable () -> Unit = {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(video.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${index + 1} / ${pl.videos.size} · ${pl.title}", color = TextDim, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 2)
                IconButton(onClick = { prev() }, enabled = index > 0) { Icon(Icons.Filled.SkipPrevious, "Önceki") }
                IconButton(onClick = { next() }, enabled = index < pl.videos.lastIndex) { Icon(Icons.Filled.SkipNext, "Sonraki") }
            }
        }
    }
    val queue: @Composable (Modifier) -> Unit = { m ->
        LazyColumn(m, state = listState, contentPadding = PaddingValues(8.dp)) {
            itemsIndexed(pl.videos, key = { _, v -> v.id }) { i, v ->
                VideoRow(v.title, v.thumb, v.duration, active = i == index) { index = i }
            }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
            Text(grade.title, color = TextDim)
        }
        if (landscapeWide) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(0.64f).fillMaxHeight()) {
                    playerBox(Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                    info()
                }
                queue(Modifier.weight(0.36f).fillMaxHeight())
            }
        } else {
            playerBox(Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            info()
            queue(Modifier.weight(1f))
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubePlayer(videoId: String, onEnded: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val fullscreen = LocalFullscreen.current
    val endedCb by rememberUpdatedState(onEnded)
    val loaded = remember { arrayOf("") }

    val web = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(android.graphics.Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            addJavascriptInterface(PlayerBridge { endedCb() }, "Android")
            webChromeClient = object : WebChromeClient() {
                private var callback: CustomViewCallback? = null
                override fun onShowCustomView(view: View, cb: CustomViewCallback) {
                    callback = cb
                    fullscreen.value = view
                    context.findActivity()?.let { act ->
                        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        WindowCompat.getInsetsController(act.window, act.window.decorView).apply {
                            hide(WindowInsetsCompat.Type.systemBars())
                            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        }
                    }
                }
                override fun onHideCustomView() {
                    fullscreen.value = null
                    callback?.onCustomViewHidden()
                    callback = null
                    context.findActivity()?.let { act ->
                        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        WindowCompat.getInsetsController(act.window, act.window.decorView).show(WindowInsetsCompat.Type.systemBars())
                    }
                }
            }
        }
    }

    // Tam ekrandayken geri tuşu önce tam ekrandan çıkar
    BackHandler(enabled = fullscreen.value != null) { web.webChromeClient?.onHideCustomView() }

    LaunchedEffect(videoId) {
        if (loaded[0].isEmpty()) {
            web.loadDataWithBaseURL(BASE, playerHtml(videoId), "text/html", "utf-8", null)
        } else if (loaded[0] != videoId) {
            web.evaluateJavascript("loadVid('$videoId')", null)
        }
        loaded[0] = videoId
    }

    DisposableEffect(Unit) {
        onDispose {
            if (fullscreen.value != null) web.webChromeClient?.onHideCustomView()
            web.stopLoading()
            web.loadUrl("about:blank")
            web.destroy()
        }
    }

    Box(modifier.background(Color.Black)) {
        AndroidView(factory = { web }, modifier = Modifier.fillMaxSize())
    }
}
