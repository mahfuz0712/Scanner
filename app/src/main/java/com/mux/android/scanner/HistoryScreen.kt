package com.mux.android.scanner

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Everything scanned with the camera, newest first. */
@Composable
fun HistoryScreen() {
    val ctx = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    val list = HistoryStore.items

    LazyColumn(
        Modifier.fillMaxSize().background(Bg).statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("History", color = TextHi, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (list.isNotEmpty()) {
                    TextButton(onClick = { confirmClear = true }) { Text("Clear all", color = Danger) }
                }
            }
        }
        if (list.isEmpty()) {
            item { Text("Scanned QR codes and barcodes will show up here.", color = TextDim, fontSize = 14.sp) }
        }
        items(list.toList(), key = { it.time.toString() + it.value }) { h ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Panel)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        h.kind + " · " + h.format,
                        color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        DateUtils.getRelativeTimeSpanString(h.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                        color = TextDim, fontSize = 11.sp
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(h.value, color = TextHi, fontSize = 15.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HistoryAction("Copy", Modifier.weight(1f)) { Util.copy(ctx, h.value) }
                    HistoryAction("Share", Modifier.weight(1f)) { Util.shareText(ctx, h.value) }
                    if (h.uri != null) {
                        HistoryAction("Open", Modifier.weight(1f)) { Util.open(ctx, h.uri) }
                    } else {
                        HistoryAction("Search", Modifier.weight(1f)) { Util.webSearch(ctx, h.value) }
                    }
                    HistoryAction("Delete", Modifier.weight(1f)) { HistoryStore.remove(h) }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = { Text("All scanned codes will be removed. Your PDFs are not touched.") },
            confirmButton = {
                TextButton(onClick = {
                    HistoryStore.clear()
                    confirmClear = false
                }) { Text("Clear", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun HistoryAction(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)) {
        Text(label, fontSize = 12.sp, maxLines = 1)
    }
}