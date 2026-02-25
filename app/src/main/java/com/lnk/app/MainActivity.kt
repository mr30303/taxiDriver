package com.lnk.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.lnk.app.navigation.AppNavGraph
import com.lnk.app.ui.theme.TaxiNoteTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TaxiNoteTheme {
                CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavGraph()
                    }
                }
            }
        }
    }
}
