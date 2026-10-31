package com.mux.android.scanner

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** LOG tab: everything scanned with the camera, newest first. */
@Composable
fun HistoryScreen(tablet: Boolean) {
    val ctx = LocalContext.current
    val c = Hud.colors
    var confirmClear by remember { mutableStateOf(false) }
    val list = HistoryStore.items

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
            HudText(list.size.toString() + " CODES STORED", color = c.dim, size = 11.sp, modifier = Modifier.weight(1f))
            HudButton("CLEAR ALL", Modifier.width(100.dp).fillMaxHeight(), KeyKind.DANGER, enabled = list.isNotEmpty(), fontSize = 11.sp) {
                confirmClear = true
            }
        }
        Spacer(Modifier.height(4.dp))
        if (list.isEmpty()) {
            EmptyNote("SCANNED QR CODES AND BARCODES WILL SHOW UP HERE")
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(list.toList(), key = { it.time.toString() + it.value }) { h ->
                    HudItem(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HudText(h.kind + " · " + h.format, color = c.accent, size = 10.sp, bold = true, modifier = Modifier.weight(1f))
                            HudText(
                                DateUtils.getRelativeTimeSpanString(h.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString().uppercase(),
                                color = c.dim, size = 10.sp
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        HudText(h.value, color = c.text, size = 14.sp, maxLines = 3)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth().height(36.dp)) {
                            HudButton("COPY", Modifier.weight(1f).fillMaxHeight(), KeyKind.OP, fontSize = 11.sp) { Util.copy(ctx, h.value, "COPIED") }
                            HudButton("SHARE", Modifier.weight(1f).fillMaxHeight(), KeyKind.OP, fontSize = 11.sp) { Util.shareText(ctx, h.value) }
                            if (h.uri != null) {
                                HudButton("OPEN", Modifier.weight(1f).fillMaxHeight(), KeyKind.GOOD, fontSize = 11.sp) { Util.open(ctx, h.uri) }
                            } else {
                                HudButton("SEARCH", Modifier.weight(1f).fillMaxHeight(), KeyKind.GOOD, fontSize = 11.sp) { Util.webSearch(ctx, h.value) }
                            }
                            HudButton("DEL", Modifier.weight(1f).fillMaxHeight(), KeyKind.DANGER, fontSize = 11.sp) { HistoryStore.remove(h) }
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        HudDialog("CLEAR HISTORY?", { confirmClear = false }) {
            HudText("All scanned codes will be removed. Your PDFs are not touched.", color = c.text, size = 13.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().height(44.dp)) {
                HudButton("CANCEL", Modifier.weight(1f).fillMaxHeight(), KeyKind.FN, fontSize = 12.sp) { confirmClear = false }
                HudButton("CLEAR", Modifier.weight(1f).fillMaxHeight(), KeyKind.DANGER, fontSize = 12.sp) {
                    HistoryStore.clear()
                    confirmClear = false
                }
            }
        }
    }
}
