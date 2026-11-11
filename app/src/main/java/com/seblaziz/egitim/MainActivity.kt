package com.seblaziz.egitim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.seblaziz.egitim.data.Library
import com.seblaziz.egitim.data.Repository
import com.seblaziz.egitim.ui.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = Repository(applicationContext)
        val library = Library(applicationContext)
        setContent { App(repo, library) }
    }
}
