package com.mux.android.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/** CODE tab: live camera QR / barcode scanner inside a HUD frame. */
@Composable
fun ScanScreen(tablet: Boolean) {
    val ctx = LocalContext.current
    val c = Hud.colors
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    var result by remember { mutableStateOf<ScanResult?>(null) }
    var torch by remember { mutableStateOf(false) }
    var hasTorch by remember { mutableStateOf(false) }

    val accept: (ScanResult) -> Unit = { r ->
        if (result == null) {
            result = r
            HistoryStore.add(r)
            Util.vibrate(ctx)
        }
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            decodeImage(ctx, uri) { r ->
                if (r == null) Util.toast(ctx, "NO CODE FOUND IN THAT IMAGE") else accept(r)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!granted) permLauncher.launch(Manifest.permission.CAMERA)
    }

    val viewport: @Composable (Modifier) -> Unit = { m ->
        val shape = HudShape(12.dp)
        Box(
            m
                .clip(shape)
                .background(Color.Black)
                .border(1.dp, c.primary.copy(alpha = 0.5f), shape)
        ) {
            if (granted) {
                CameraPreview(
                    paused = result != null,
                    torch = torch,
                    onTorchAvailable = { hasTorch = it },
                    onCode = accept
                )
                Reticle(Modifier.fillMaxSize(), result != null)
                Row(Modifier.padding(10.dp)) {
                    HudText(if (result != null) "● LOCKED" else "● LIVE", color = if (result != null) c.good else c.accent, size = 10.sp, bold = true)
                }
            } else {
                Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                    PermissionPanel("CAMERA ACCESS IS NEEDED TO READ QR CODES AND BARCODES.", onGrant = {
                        permLauncher.launch(Manifest.permission.CAMERA)
                    })
                }
            }
        }
    }

    val side: @Composable (Modifier) -> Unit = { m ->
        Column(m) {
            val r = result
            if (r == null) {
                HudPanel(Modifier.fillMaxWidth(), "STATUS") {
                    HudText(if (granted) "SEARCHING FOR CODE..." else "CAMERA OFFLINE", color = c.accent, size = 13.sp, bold = true)
                    Spacer(Modifier.height(4.dp))
                    HudText("QR · EAN · UPC · CODE 128 / 39 / 93 · ITF · DATA MATRIX · PDF417 · AZTEC", color = c.dim, size = 10.sp)
                }
            } else {
                HudPanel(Modifier.fillMaxWidth(), "TARGET ACQUIRED") {
                    HudText(r.kind + " · " + r.format, color = c.accent, size = 11.sp, bold = true)
                    Spacer(Modifier.height(4.dp))
                    HudText(r.detail ?: r.value, color = c.text, size = 15.sp, maxLines = 6)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth().height(40.dp)) {
                        HudButton("COPY", Modifier.weight(1f).fillMaxHeight(), KeyKind.OP, fontSize = 11.sp) { Util.copy(ctx, r.value, "COPIED") }
                        if (r.secret != null && r.secret.isNotEmpty()) {
                            HudButton("PASS", Modifier.weight(1f).fillMaxHeight(), KeyKind.OP, fontSize = 11.sp) { Util.copy(ctx, r.secret, "PASSWORD COPIED") }
                        }
                        HudButton("SHARE", Modifier.weight(1f).fillMaxHeight(), KeyKind.OP, fontSize = 11.sp) { Util.shareText(ctx, r.value) }
                        if (r.openUri != null) {
                            HudButton("OPEN", Modifier.weight(1f).fillMaxHeight(), KeyKind.GOOD, fontSize = 11.sp) { Util.open(ctx, r.openUri) }
                        } else {
                            HudButton("SEARCH", Modifier.weight(1f).fillMaxHeight(), KeyKind.GOOD, fontSize = 11.sp) { Util.webSearch(ctx, r.value) }
                        }
                    }
                    HudButton("SCAN AGAIN", Modifier.fillMaxWidth().height(44.dp), KeyKind.ACCENT, fontSize = 13.sp) { result = null }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().height(44.dp)) {
                HudButton("TORCH", Modifier.weight(1f).fillMaxHeight(), KeyKind.OP, enabled = granted && hasTorch, selected = torch, fontSize = 12.sp) { torch = !torch }
                HudButton("GALLERY", Modifier.weight(1f).fillMaxHeight(), KeyKind.OP, fontSize = 12.sp) { gallery.launch("image/*") }
            }
        }
    }

    if (tablet) {
        Row(Modifier.fillMaxSize()) {
            viewport(Modifier.weight(1.3f).fillMaxHeight())
            Spacer(Modifier.width(10.dp))
            side(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()))
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            viewport(Modifier.weight(1f).fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            side(Modifier.fillMaxWidth())
        }
    }
}

/** Targeting reticle: corner brackets, rotating arcs and a moving scan line. */
@Composable
private fun Reticle(modifier: Modifier, locked: Boolean) {
    val c = Hud.colors
    val t = rememberInfiniteTransition(label = "reticle")
    val line by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "line"
    )
    val ang by t.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "ang"
    )
    Canvas(modifier) {
        val col = if (locked) c.good else c.primary
        val side = size.minDimension * 0.62f
        val center = Offset(size.width / 2f, size.height / 2f)
        val left = center.x - side / 2f
        val top = center.y - side / 2f
        val right = left + side
        val bottom = top + side
        val len = side * 0.18f
        val sw = 3.dp.toPx()
        drawLine(col, Offset(left, top), Offset(left + len, top), sw, StrokeCap.Round)
        drawLine(col, Offset(left, top), Offset(left, top + len), sw, StrokeCap.Round)
        drawLine(col, Offset(right, top), Offset(right - len, top), sw, StrokeCap.Round)
        drawLine(col, Offset(right, top), Offset(right, top + len), sw, StrokeCap.Round)
        drawLine(col, Offset(left, bottom), Offset(left + len, bottom), sw, StrokeCap.Round)
        drawLine(col, Offset(left, bottom), Offset(left, bottom - len), sw, StrokeCap.Round)
        drawLine(col, Offset(right, bottom), Offset(right - len, bottom), sw, StrokeCap.Round)
        drawLine(col, Offset(right, bottom), Offset(right, bottom - len), sw, StrokeCap.Round)

        val r = size.minDimension * 0.46f
        drawCircle(col.copy(alpha = 0.18f), r, center, style = Stroke(1.dp.toPx()))
        rotate(degrees = ang, pivot = center) {
            drawArc(
                color = col.copy(alpha = 0.5f),
                startAngle = 0f,
                sweepAngle = 70f,
                useCenter = false,
                topLeft = Offset(center.x - r, center.y - r),
                size = Size(2 * r, 2 * r),
                style = Stroke(2.dp.toPx())
            )
            drawArc(
                color = c.accent.copy(alpha = 0.45f),
                startAngle = 180f,
                sweepAngle = 40f,
                useCenter = false,
                topLeft = Offset(center.x - r, center.y - r),
                size = Size(2 * r, 2 * r),
                style = Stroke(2.dp.toPx())
            )
        }
        if (!locked) {
            val y = top + side * line
            drawLine(c.accent.copy(alpha = 0.85f), Offset(left + 8.dp.toPx(), y), Offset(right - 8.dp.toPx(), y), 2.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable
private fun CameraPreview(
    paused: Boolean,
    torch: Boolean,
    onTorchAvailable: (Boolean) -> Unit,
    onCode: (ScanResult) -> Unit
) {
    val ctx = LocalContext.current
    val owner = remember { ctx.findActivity() as LifecycleOwner }
    val previewView = remember {
        PreviewView(ctx).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            // TextureView, so the HUD-shaped clip around the camera works
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val isPaused by rememberUpdatedState(paused)
    val deliver by rememberUpdatedState(onCode)
    val torchCb by rememberUpdatedState(onTorchAvailable)

    DisposableEffect(owner) {
        val main = Handler(Looper.getMainLooper())
        val executor = Executors.newSingleThreadExecutor()
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
        )
        val future = ProcessCameraProvider.getInstance(ctx)
        var provider: ProcessCameraProvider? = null
        future.addListener({
            try {
                val p = future.get()
                provider = p
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                @Suppress("DEPRECATION")
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(android.util.Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { proxy ->
                    analyze(proxy, scanner, { isPaused }) { r -> main.post { deliver(r) } }
                }
                p.unbindAll()
                val cam = p.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                camera = cam
                torchCb(cam.cameraInfo.hasFlashUnit())
            } catch (e: Exception) {
                Util.toast(ctx, "CAMERA NOT AVAILABLE")
            }
        }, ContextCompat.getMainExecutor(ctx))

        onDispose {
            try { provider?.unbindAll() } catch (e: Exception) { }
            camera = null
            try { scanner.close() } catch (e: Exception) { }
            executor.shutdown()
        }
    }

    LaunchedEffect(torch, camera) {
        try { camera?.cameraControl?.enableTorch(torch) } catch (e: Exception) { }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun analyze(proxy: ImageProxy, scanner: BarcodeScanner, paused: () -> Boolean, onResult: (ScanResult) -> Unit) {
    try {
        val media = proxy.image
        if (media == null || paused()) {
            proxy.close()
            return
        }
        val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { list ->
                val r = list.firstNotNullOfOrNull { ScanResult.from(it) }
                if (r != null) onResult(r)
            }
            .addOnCompleteListener { proxy.close() }
    } catch (e: Exception) {
        try { proxy.close() } catch (x: Exception) { }
    }
}

private fun decodeImage(ctx: android.content.Context, uri: Uri, done: (ScanResult?) -> Unit) {
    try {
        val image = InputImage.fromFilePath(ctx, uri)
        val s = BarcodeScanning.getClient()
        s.process(image)
            .addOnSuccessListener { list -> done(list.firstNotNullOfOrNull { ScanResult.from(it) }) }
            .addOnFailureListener { done(null) }
            .addOnCompleteListener { s.close() }
    } catch (e: Exception) {
        done(null)
    }
}
