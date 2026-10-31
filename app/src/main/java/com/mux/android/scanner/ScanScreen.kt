package com.mux.android.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Size
import android.os.Handler
import android.os.Looper
import android.provider.Settings
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
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

/** Live camera QR / barcode scanner. */
@Composable
fun ScanScreen() {
    val ctx = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var asked by remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        asked = true
    }
    var result by remember { mutableStateOf<ScanResult?>(null) }
    var torch by remember { mutableStateOf(false) }
    var hasTorch by remember { mutableStateOf(false) }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            decodeImage(ctx, uri) { r ->
                if (r == null) {
                    Util.toast(ctx, "No QR code or barcode found in that image")
                } else if (result == null) {
                    result = r
                    HistoryStore.add(r)
                    Util.vibrate(ctx)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!granted) permLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (granted) {
            CameraPreview(
                paused = result != null,
                torch = torch,
                onTorchAvailable = { hasTorch = it },
                onCode = { r ->
                    if (result == null) {
                        result = r
                        HistoryStore.add(r)
                        Util.vibrate(ctx)
                    }
                }
            )
            ScanFrame(Modifier.fillMaxSize())
        } else {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Camera permission needed", color = TextHi, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Scanner uses the camera only to read QR codes and barcodes.", color = TextDim, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                Button(onClick = {
                    if (asked) {
                        ctx.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } else {
                        permLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) { Text(if (asked) "Open app settings" else "Allow camera") }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { gallery.launch("image/*") }) { Text("Scan from an image instead") }
            }
        }

        // top bar
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Scan code", color = TextHi, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (granted && hasTorch) {
                    PillButton(if (torch) "Torch on" else "Torch", highlight = torch) { torch = !torch }
                }
                PillButton("Gallery") { gallery.launch("image/*") }
            }
        }

        if (granted && result == null) {
            Text(
                "Point the camera at a QR code or barcode",
                color = TextHi,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xAA000000))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        result?.let { r ->
            ResultCard(r, Modifier.align(Alignment.BottomCenter)) { result = null }
        }
    }
}

@Composable
private fun PillButton(label: String, highlight: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (highlight) Accent else Color(0xCC0E1822),
            contentColor = if (highlight) Color(0xFF00212B) else TextHi
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
    ) { Text(label, fontSize = 13.sp) }
}

@Composable
private fun ScanFrame(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "scan")
    val p by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "line"
    )
    Canvas(modifier) {
        val side = size.minDimension * 0.68f
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f - size.height * 0.04f
        val right = left + side
        val bottom = top + side
        val len = side * 0.16f
        val sw = 7f
        val c = Accent
        // four corners
        drawLine(c, Offset(left, top), Offset(left + len, top), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(c, Offset(left, top), Offset(left, top + len), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(c, Offset(right, top), Offset(right - len, top), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(c, Offset(right, top), Offset(right, top + len), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(c, Offset(left, bottom), Offset(left + len, bottom), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(c, Offset(left, bottom), Offset(left, bottom - len), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(c, Offset(right, bottom), Offset(right - len, bottom), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(c, Offset(right, bottom), Offset(right, bottom - len), strokeWidth = sw, cap = StrokeCap.Round)
        // moving scan line
        val y = top + side * p
        drawLine(Amber.copy(alpha = 0.9f), Offset(left + 14f, y), Offset(right - 14f, y), strokeWidth = 4f, cap = StrokeCap.Round)
    }
}

@Composable
private fun ResultCard(r: ScanResult, modifier: Modifier, onDone: () -> Unit) {
    val ctx = LocalContext.current
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(Panel)
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(r.kind, Accent)
            Chip(r.format, Amber)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            r.detail ?: r.value,
            color = TextHi,
            fontSize = 17.sp,
            maxLines = 8,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallAction("Copy", Modifier.weight(1f)) { Util.copy(ctx, r.value) }
            if (r.secret != null && r.secret.isNotEmpty()) {
                SmallAction("Password", Modifier.weight(1f)) { Util.copy(ctx, r.secret, "Password copied") }
            }
            SmallAction("Share", Modifier.weight(1f)) { Util.shareText(ctx, r.value) }
            if (r.openUri != null) {
                SmallAction("Open", Modifier.weight(1f)) { Util.open(ctx, r.openUri) }
            } else {
                SmallAction("Search", Modifier.weight(1f)) { Util.webSearch(ctx, r.value) }
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Scan again") }
    }
}

@Composable
private fun Chip(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(PanelHi)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun SmallAction(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)) {
        Text(label, fontSize = 12.sp, maxLines = 1)
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
        PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
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
                    .setTargetResolution(Size(1280, 720))
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
                Util.toast(ctx, "Camera not available")
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