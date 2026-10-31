package com.mux.android.scanner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** DOCS tab: scan paper to PDF + list of saved PDFs. */
@Composable
fun DocsScreen(tablet: Boolean) {
    val ctx = LocalContext.current
    val c = Hud.colors
    var files by remember { mutableStateOf(DocStore.list(ctx)) }
    var renaming by remember { mutableStateOf<File?>(null) }
    var deleting by remember { mutableStateOf<File?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val sr = GmsDocumentScanningResult.fromActivityResultIntent(res.data)
            val pdf = sr?.pdf
            if (pdf != null) {
                val f = DocStore.importPdf(ctx, pdf.uri)
                if (f != null) {
                    files = DocStore.list(ctx)
                    Util.toast(ctx, "SAVED " + f.name)
                } else {
                    Util.toast(ctx, "COULD NOT SAVE THE PDF")
                }
            }
        }
    }

    val startScan: () -> Unit = {
        val activity = ctx.findActivity()
        if (activity == null) {
            Util.toast(ctx, "CANNOT START THE SCANNER")
        } else {
            val options = GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setPageLimit(50)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build()
            GmsDocumentScanning.getClient(options)
                .getStartScanIntent(activity)
                .addOnSuccessListener { sender -> launcher.launch(IntentSenderRequest.Builder(sender).build()) }
                .addOnFailureListener { e ->
                    Util.toast(ctx, "SCANNER NOT AVAILABLE: " + (e.message ?: "NEEDS GOOGLE PLAY SERVICES + INTERNET THE FIRST TIME"))
                }
        }
    }

    val header: @Composable (Modifier) -> Unit = { m ->
        HudPanel(m, "DOCUMENT SCANNER") {
            HudText(
                "Scan paper with the camera. Edges are found automatically; crop, clean up and add pages. Saved as PDF.",
                color = c.text, size = 12.sp
            )
            Spacer(Modifier.height(8.dp))
            HudButton("SCAN DOCUMENT > PDF", Modifier.fillMaxWidth().height(48.dp), KeyKind.ACCENT, fontSize = 14.sp) { startScan() }
        }
    }

    val list: @Composable (Modifier) -> Unit = { m ->
        if (files.isEmpty()) {
            Column(m) { EmptyNote("NO SCANNED PDF YET") }
        } else {
            LazyColumn(m) {
                items(files, key = { it.absolutePath }) { f ->
                    DocCard(f, onRename = { renaming = f }, onDelete = { deleting = f })
                }
            }
        }
    }

    if (tablet) {
        Row(Modifier.fillMaxSize()) {
            header(Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            list(Modifier.weight(1.3f).fillMaxHeight())
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            header(Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            list(Modifier.weight(1f).fillMaxWidth())
        }
    }

    renaming?.let { f ->
        var name by remember(f) { mutableStateOf(f.nameWithoutExtension) }
        HudDialog("RENAME FILE", { renaming = null }) {
            HudField(name, "FILE NAME", Modifier.fillMaxWidth().height(46.dp)) { name = it }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().height(44.dp)) {
                HudButton("CANCEL", Modifier.weight(1f).fillMaxHeight(), KeyKind.FN, fontSize = 12.sp) { renaming = null }
                HudButton("SAVE", Modifier.weight(1f).fillMaxHeight(), KeyKind.ACCENT, fontSize = 12.sp) {
                    val clean = name.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_")
                    if (clean.isNotEmpty()) {
                        val target = File(f.parentFile, "$clean.pdf")
                        if (target.exists() && target != f) {
                            Util.toast(ctx, "A FILE WITH THAT NAME ALREADY EXISTS")
                        } else {
                            if (target != f) f.renameTo(target)
                            files = DocStore.list(ctx)
                            renaming = null
                        }
                    }
                }
            }
        }
    }

    deleting?.let { f ->
        HudDialog("DELETE PDF?", { deleting = null }) {
            HudText(f.name, color = c.text, size = 13.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().height(44.dp)) {
                HudButton("CANCEL", Modifier.weight(1f).fillMaxHeight(), KeyKind.FN, fontSize = 12.sp) { deleting = null }
                HudButton("DELETE", Modifier.weight(1f).fillMaxHeight(), KeyKind.DANGER, fontSize = 12.sp) {
                    f.delete()
                    files = DocStore.list(ctx)
                    deleting = null
                }
            }
        }
    }
}

@Composable
private fun DocCard(file: File, onRename: () -> Unit, onDelete: () -> Unit) {
    val ctx = LocalContext.current
    val c = Hud.colors
    val info by produceState<PdfInfo?>(null, file.absolutePath, file.lastModified()) {
        value = withContext(Dispatchers.IO) { DocStore.info(file) }
    }

    HudItem(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(width = 50.dp, height = 66.dp)
                    .clip(HudShape(4.dp))
                    .background(Color.White)
                    .border(1.dp, c.primary.copy(alpha = 0.6f), HudShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                val bmp = info?.thumb
                if (bmp != null) {
                    Image(bmp.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    HudText("PDF", color = Color.Gray, size = 11.sp, bold = true)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                HudText(file.nameWithoutExtension, color = c.text, size = 14.sp, bold = true, maxLines = 2)
                val pages = info?.pages ?: 0
                val pageText = if (pages > 0) pages.toString() + (if (pages == 1) " PAGE · " else " PAGES · ") else ""
                HudText(
                    pageText + Formatter.formatShortFileSize(ctx, file.length()).uppercase() + " · " +
                        DateUtils.getRelativeTimeSpanString(file.lastModified(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString().uppercase(),
                    color = c.dim, size = 10.sp
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().height(38.dp)) {
            HudButton("OPEN", Modifier.weight(1f).fillMaxHeight(), KeyKind.GOOD, fontSize = 11.sp) { openPdf(ctx, file) }
            HudButton("SHARE", Modifier.weight(1f).fillMaxHeight(), KeyKind.OP, fontSize = 11.sp) { sharePdf(ctx, file) }
            HudButton("SAVE", Modifier.weight(1f).fillMaxHeight(), KeyKind.OP, fontSize = 11.sp) {
                if (DocStore.saveToDownloads(ctx, file)) Util.toast(ctx, "SAVED TO DOWNLOADS/SCANNER")
                else Util.toast(ctx, "COULD NOT SAVE TO DOWNLOADS")
            }
        }
        Row(Modifier.fillMaxWidth().height(34.dp)) {
            HudButton("RENAME", Modifier.weight(1f).fillMaxHeight(), KeyKind.FN, fontSize = 10.sp) { onRename() }
            HudButton("DELETE", Modifier.weight(1f).fillMaxHeight(), KeyKind.DANGER, fontSize = 10.sp) { onDelete() }
        }
    }
}

private fun openPdf(ctx: Context, f: File) {
    try {
        val i = Intent(Intent.ACTION_VIEW)
            .setDataAndType(DocStore.uri(ctx, f), "application/pdf")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
    } catch (e: Exception) {
        Util.toast(ctx, "NO PDF VIEWER INSTALLED")
    }
}

private fun sharePdf(ctx: Context, f: File) {
    try {
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, DocStore.uri(ctx, f))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(i, "Share PDF").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Util.toast(ctx, "CANNOT SHARE")
    }
}
