package com.mux.android.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        HistoryStore.init(this)
        setContent {
            ScannerTheme {
                ScannerApp()
            }
        }
    }
}

@Composable
fun ScannerApp() {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    LaunchedEffect(Unit) { if (isOnline(ctx)) update = checkUpdate(ctx).second }
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = Accent,
        selectedTextColor = Accent,
        indicatorColor = PanelHi,
        unselectedIconColor = TextDim,
        unselectedTextColor = TextDim
    )
    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Text("📷") }, label = { Text("Code") }, colors = colors)
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Text("📄") }, label = { Text("Documents") }, colors = colors)
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Text("🕘") }, label = { Text("History") }, colors = colors)
                NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Text("ℹ️") }, label = { Text("About") }, colors = colors)
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().background(Bg).padding(bottom = pad.calculateBottomPadding())) {
            when (tab) {
                0 -> ScanScreen()
                1 -> DocsScreen()
                2 -> HistoryScreen()
                else -> AboutScreen { update = it }
            }
        }
    }

    update?.let { u ->
        UpdateDialog(
            u = u,
            current = appVersion(ctx),
            onUpdate = { Util.open(ctx, u.apkUrl); update = null },
            onCancel = { update = null }
        )
    }
}