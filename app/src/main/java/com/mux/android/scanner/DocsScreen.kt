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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Scan paper documents into PDF files + list of saved PDFs. */
@Composable
fun DocsScreen() {
    val ctx = LocalContext.current
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
                    Util.toast(ctx, "Saved ${f.name}")
                } else {
                    Util.toast(ctx, "Could not save the PDF")
                }
            }
        }
    }

    val startScan = {
        val activity = ctx.findActivity()
        if (activity == null) {
            Util.toast(ctx, "Cannot start the scanner")
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
                    Util.toast(ctx, "Scanner not available: ${e.message ?: "needs Google Play services and internet the first time"}")
                }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().background(Bg).statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Documents", color = TextHi, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Scan paper with the camera. Edges are found automatically, you can crop, clean up and add pages. Saved as PDF.",
                    color = TextDim, fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { startScan() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Scan document to PDF", fontSize = 16.sp)
                }
            }
        }
        if (files.isEmpty()) {
            item {
                Text("No scanned PDFs yet.", color = TextDim, fontSize = 14.sp, modifier = Modifier.padding(top = 20.dp))
            }
        }
        items(files, key = { it.absolutePath }) { f ->
            DocCard(
                file = f,
                onRename = { renaming = f },
                onDelete = { deleting = f }
            )
        }
    }

    renaming?.let { f ->
        var name by remember(f) { mutableStateOf(f.nameWithoutExtension) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    val clean = name.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_")
                    if (clean.isNotEmpty()) {
                        val target = File(f.parentFile, "$clean.pdf")
                        if (target.exists() && target != f) {
                            Util.toast(ctx, "A file with that name already exists")
                        } else {
                            if (target != f) f.renameTo(target)
                            files = DocStore.list(ctx)
                            renaming = null
                        }
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel") } }
        )
    }

    deleting?.let { f ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete PDF?") },
            text = { Text(f.name) },
            confirmButton = {
                TextButton(onClick = {
                    f.delete()
                    files = DocStore.list(ctx)
                    deleting = null
                }) { Text("Delete", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun DocCard(file: File, onRename: () -> Unit, onDelete: () -> Unit) {
    val ctx = LocalContext.current
    val info by produceState<PdfInfo?>(null, file.absolutePath, file.lastModified()) {
        value = withContext(Dispatchers.IO) { DocStore.info(file) }
    }
    var menu by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Panel)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(width = 56.dp, height = 74.dp).clip(RoundedCornerShape(6.dp)).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                val bmp = info?.thumb
                if (bmp != null) {
                    Image(bmp.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Text("PDF", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(file.nameWithoutExtension, color = TextHi, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val pages = info?.pages ?: 0
                val pageText = if (pages > 0) "$pages page${if (pages == 1) "" else "s"} · " else ""
                Text(
                    pageText + Formatter.formatShortFileSize(ctx, file.length()) + " · " +
                        DateUtils.getRelativeTimeSpanString(file.lastModified(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS),
                    color = TextDim, fontSize = 12.sp
                )
            }
            Box {
                TextButton(onClick = { menu = true }) { Text("⋮", color = TextHi, fontSize = 20.sp) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text("Delete", color = Danger) }, onClick = { menu = false; onDelete() })
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { openPdf(ctx, file) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("Open", fontSize = 13.sp) }
            OutlinedButton(onClick = { sharePdf(ctx, file) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) { Text("Share", fontSize = 13.sp) }
            OutlinedButton(
                onClick = {
                    if (DocStore.saveToDownloads(ctx, file)) Util.toast(ctx, "Saved to Downloads/Scanner")
                    else Util.toast(ctx, "Could not save to Downloads")
                },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(4.dp)
            ) { Text("Save", fontSize = 13.sp) }
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
        Util.toast(ctx, "No PDF viewer installed")
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
        Util.toast(ctx, "Cannot share")
    }
}