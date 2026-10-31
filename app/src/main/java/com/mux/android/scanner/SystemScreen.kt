package com.mux.android.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

/** SYS tab: app info, theme, permission, updates, author. */
@Composable
fun SystemScreen(tablet: Boolean, onUpdateFound: (UpdateInfo) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (tablet) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    AppPanel(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp)); PermissionsPanel(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp)); EnginePanel(Modifier.fillMaxWidth())
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    UpdatePanel(onUpdateFound, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp)); AuthorPanel(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp)); MoreAppsPanel(Modifier.fillMaxWidth())
                }
            }
        } else {
            AppPanel(Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp)); PermissionsPanel(Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp)); EnginePanel(Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp)); UpdatePanel(onUpdateFound, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp)); AuthorPanel(Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp)); MoreAppsPanel(Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AppPanel(modifier: Modifier) {
    val c = Hud.colors
    val version = appVersion(LocalContext.current)
    HudPanel(modifier, "SYSTEM") {
        HudText("◈ " + AppInfo.NAME.uppercase(), color = c.primary, size = 28.sp, bold = true)
        HudText("VERSION $version", color = c.accent, size = 12.sp)
        Spacer(Modifier.height(8.dp))
        HudText(
            "Scan documents to PDF, and scan QR codes and barcodes, in a sci-fi HUD interface. Your scans stay on your phone. Adaptive layout for phones and tablets.",
            color = c.text, size = 13.sp
        )
        Spacer(Modifier.height(8.dp))
        HudText("THEME", color = c.dim, size = 10.sp)
        SettingRow("COLOR THEME", null, if (AppSettings.amber) "AMBER" else "CYAN", KeyKind.ACCENT) {
            AppSettings.applyAmber(!AppSettings.amber)
        }
    }
}

@Composable
private fun PermissionsPanel(modifier: Modifier) {
    val c = Hud.colors
    val ctx = LocalContext.current
    var tick by remember { mutableStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    val ok = remember(tick) {
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    HudPanel(modifier, "PERMISSIONS") {
        Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
            HudText("CAMERA", color = c.text, size = 12.sp, modifier = Modifier.weight(1f))
            HudText(if (ok) "OK" else "MISSING", color = if (ok) c.good else c.danger, size = 12.sp, bold = true)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            HudButton("GRANT", Modifier.weight(1f).height(44.dp), KeyKind.ACCENT, enabled = !ok, fontSize = 12.sp) {
                launcher.launch(Manifest.permission.CAMERA)
            }
            HudButton("APP SETTINGS", Modifier.weight(1f).height(44.dp), KeyKind.FN, fontSize = 12.sp) { Util.openAppSettings(ctx) }
        }
    }
}

@Composable
private fun EnginePanel(modifier: Modifier) {
    val c = Hud.colors
    HudPanel(modifier, "SCAN ENGINES") {
        HudText("CODE READER", color = c.primary, size = 12.sp, bold = true)
        HudText("ML Kit barcode model, built into the app. Works offline.", color = c.text, size = 12.sp)
        Spacer(Modifier.height(6.dp))
        HudText("DOCUMENT SCANNER", color = c.primary, size = 12.sp, bold = true)
        HudText("ML Kit document scanner through Google Play services. Needs internet the first time to download the module.", color = c.text, size = 12.sp)
    }
}

@Composable
private fun UpdatePanel(onUpdateFound: (UpdateInfo) -> Unit, modifier: Modifier) {
    val c = Hud.colors
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    HudPanel(modifier, "UPDATES") {
        HudButton(
            label = "CHECK FOR UPDATES",
            modifier = Modifier.fillMaxWidth().height(46.dp),
            kind = KeyKind.OP,
            fontSize = 13.sp
        ) {
            scope.launch {
                status = "CHECKING..."
                if (!isOnline(ctx)) {
                    status = "NO INTERNET CONNECTION"
                    return@launch
                }
                val (ok, info) = checkUpdate(ctx)
                status = when {
                    !ok -> "CHECK FAILED"
                    info == null -> "UP TO DATE"
                    else -> "NEW VERSION AVAILABLE: v${info.version}"
                }
                if (info != null) onUpdateFound(info)
            }
        }
        if (status.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            HudText(status, color = c.dim, size = 12.sp)
        }
    }
}

@Composable
private fun AuthorPanel(modifier: Modifier) {
    HudPanel(modifier, "AUTHOR") {
        LinkRow("NAME", AppInfo.AUTHOR, null)
        LinkRow("EMAIL", AppInfo.EMAIL, "mailto:" + AppInfo.EMAIL)
        LinkRow("GITHUB", "github.com/" + AppInfo.GITHUB_USER, AppInfo.GITHUB)
        LinkRow("PORTFOLIO", AppInfo.PORTFOLIO.removePrefix("https://"), AppInfo.PORTFOLIO)
    }
}

@Composable
private fun MoreAppsPanel(modifier: Modifier) {
    val c = Hud.colors
    val ctx = LocalContext.current
    HudPanel(modifier, "MORE APPS") {
        HudText("Discover more apps by the developer on zApps Store.", color = c.text, size = 13.sp)
        Spacer(Modifier.height(8.dp))
        HudButton(
            label = "OPEN zApps STORE ▸",
            modifier = Modifier.fillMaxWidth().height(48.dp),
            kind = KeyKind.ACCENT,
            fontSize = 14.sp
        ) { Util.open(ctx, AppInfo.MORE_APPS) }
    }
}