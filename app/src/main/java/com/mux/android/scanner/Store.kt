package com.mux.android.scanner

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryItem(val value: String, val format: String, val kind: String, val uri: String?, val time: Long)

/** Scanned QR codes / barcodes, newest first. Saved in SharedPreferences. */
object HistoryStore {
    val items = mutableStateListOf<HistoryItem>()
    private var prefs: SharedPreferences? = null

    fun init(ctx: Context) {
        if (prefs != null) return
        prefs = ctx.applicationContext.getSharedPreferences("scanner", Context.MODE_PRIVATE)
        load()
    }

    private fun load() {
        items.clear()
        try {
            val arr = JSONArray(prefs?.getString("history", "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val u = o.optString("u", "")
                items.add(HistoryItem(o.getString("v"), o.getString("f"), o.getString("k"), if (u.isEmpty()) null else u, o.getLong("t")))
            }
        } catch (e: Exception) {
        }
    }

    private fun save() {
        val arr = JSONArray()
        for (h in items) {
            val o = JSONObject()
            o.put("v", h.value)
            o.put("f", h.format)
            o.put("k", h.kind)
            o.put("t", h.time)
            if (h.uri != null) o.put("u", h.uri)
            arr.put(o)
        }
        prefs?.edit()?.putString("history", arr.toString())?.apply()
    }

    fun add(r: ScanResult) {
        if (items.firstOrNull()?.value == r.value) return
        items.add(0, HistoryItem(r.value, r.format, r.kind, r.openUri, System.currentTimeMillis()))
        while (items.size > 200) items.removeAt(items.lastIndex)
        save()
    }

    fun remove(item: HistoryItem) {
        items.remove(item)
        save()
    }

    fun clear() {
        items.clear()
        save()
    }
}

class PdfInfo(val pages: Int, val thumb: Bitmap?)

/** Scanned PDF files, kept in the app's private storage (filesDir/scans). */
object DocStore {
    fun dir(ctx: Context): File {
        val d = File(ctx.filesDir, "scans")
        d.mkdirs()
        return d
    }

    fun list(ctx: Context): List<File> =
        dir(ctx).listFiles { f -> f.isFile && f.extension.equals("pdf", true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    /** Copies the PDF made by the document scanner into our own folder. */
    fun importPdf(ctx: Context, src: Uri): File? {
        return try {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            var f = File(dir(ctx), "Scan_$stamp.pdf")
            var n = 1
            while (f.exists()) {
                f = File(dir(ctx), "Scan_${stamp}_$n.pdf")
                n++
            }
            val input = ctx.contentResolver.openInputStream(src) ?: return null
            input.use { ins -> f.outputStream().use { out -> ins.copyTo(out) } }
            f
        } catch (e: Exception) {
            null
        }
    }

    fun uri(ctx: Context, file: File): Uri =
        FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)

    /** Saves a copy to Downloads/Scanner so it shows up in the Files app. */
    fun saveToDownloads(ctx: Context, file: File): Boolean {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Scanner")
            }
            val target = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
            val out = ctx.contentResolver.openOutputStream(target) ?: return false
            out.use { o -> file.inputStream().use { it.copyTo(o) } }
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Page count + a thumbnail of the first page. */
    fun info(file: File): PdfInfo {
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                val r = PdfRenderer(pfd)
                try {
                    val pages = r.pageCount
                    var bmp: Bitmap? = null
                    if (pages > 0) {
                        val p = r.openPage(0)
                        try {
                            val w = 240
                            val h = (w * p.height / p.width.toFloat()).toInt().coerceAtLeast(1)
                            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            b.eraseColor(android.graphics.Color.WHITE)
                            p.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bmp = b
                        } finally {
                            p.close()
                        }
                    }
                    PdfInfo(pages, bmp)
                } finally {
                    r.close()
                }
            }
        } catch (e: Exception) {
            PdfInfo(0, null)
        }
    }
}