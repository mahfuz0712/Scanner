package com.mux.android.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** About tab: version, update check, author links. */
@Composable
fun AboutScreen(onUpdateFound: (UpdateInfo) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("About", color = TextHi, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Section("App") {
            Text(AppInfo.NAME, color = Accent, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Version ${appVersion(ctx)}", color = Amber, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Scan documents to PDF, and scan QR codes and barcodes. Your scans stay on your phone. The camera is only used for scanning.",
                color = TextHi, fontSize = 14.sp
            )
        }

        Section("Updates") {
            Button(
                onClick = {
                    scope.launch {
                        checking = true
                        status = "Checking..."
                        if (!isOnline(ctx)) {
                            status = "No internet connection"
                            checking = false
                            return@launch
                        }
                        val (ok, info) = checkUpdate(ctx)
                        status = when {
                            !ok -> "Check failed"
                            info == null -> "You are up to date"
                            else -> "New version available: v${info.version}"
                        }
                        if (info != null) onUpdateFound(info)
                        checking = false
                    }
                },
                enabled = !checking,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("Check for updates") }
            if (status.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(status, color = TextDim, fontSize = 13.sp)
            }
        }

        Section("Author") {
            LinkRow("Name", AppInfo.AUTHOR, null)
            LinkRow("Email", AppInfo.EMAIL, "mailto:${AppInfo.EMAIL}")
            LinkRow("GitHub", "github.com/${AppInfo.GITHUB_USER}", AppInfo.GITHUB)
            LinkRow("Portfolio", AppInfo.PORTFOLIO.removePrefix("https://"), AppInfo.PORTFOLIO)
        }

        Section("More apps") {
            Text("Discover more apps by the developer on zApps Store.", color = TextHi, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { Util.open(ctx, AppInfo.MORE_APPS) },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("Open zApps Store") }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Panel)
            .padding(14.dp)
    ) {
        Text(title.uppercase(), color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun LinkRow(label: String, value: String, link: String?) {
    val ctx = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (link != null) Modifier.clickable { Util.open(ctx, link) } else Modifier)
            .padding(vertical = 6.dp)
    ) {
        Text(label, color = TextDim, fontSize = 11.sp)
        Text(value, color = if (link != null) Accent else TextHi, fontSize = 16.sp)
    }
}