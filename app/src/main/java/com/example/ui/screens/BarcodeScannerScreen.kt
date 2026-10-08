package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.InventoryItem
import com.example.ui.components.BarcodeLabelPreview
import com.example.ui.components.formatRupiah
import com.example.ui.theme.CyberTeal400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.LaserAmber500
import com.example.ui.theme.Rose400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

enum class ScannerActionMode(val label: String) {
    INSPECT("Inspeksi SKU"),
    QUICK_INBOUND("Quick Inbound (+1)"),
    QUICK_OUTBOUND("Quick Outbound (-1)"),
    OPNAME("Verifikasi Opname")
}

private fun triggerScanHaptic(context: Context) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(60L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(60L)
        }
    } catch (_: Exception) {
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BarcodeScannerScreen(
    items: List<InventoryItem>,
    scannedCode: String,
    onCodeDetected: (String) -> Unit,
    onOpenTransactionDialog: (InventoryItem, String) -> Unit,
    onQuickStockStep: (InventoryItem, String) -> Unit,
    onRegisterUnrecognizedBarcode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var manualBarcodeInput by remember(scannedCode) { mutableStateOf(scannedCode) }
    var scannerActionMode by remember { mutableStateOf(ScannerActionMode.INSPECT) }
    var torchEnabled by remember { mutableStateOf(false) }
    var lastAutoProcessedTimestamp by remember { mutableStateOf(0L) }

    val matchedItem = remember(items, scannedCode) {
        if (scannedCode.isBlank()) null
        else items.firstOrNull {
            it.barcode.equals(scannedCode.trim(), ignoreCase = true) ||
                it.sku.equals(scannedCode.trim(), ignoreCase = true)
        }
    }

    val handleDetectedCode: (String) -> Unit = { rawCode ->
        val clean = rawCode.trim()
        val now = System.currentTimeMillis()
        if (clean.isNotEmpty() && (clean != scannedCode || now - lastAutoProcessedTimestamp > 1800L)) {
            lastAutoProcessedTimestamp = now
            triggerScanHaptic(context)
            onCodeDetected(clean)
            manualBarcodeInput = clean

            val found = items.firstOrNull {
                it.barcode.equals(clean, ignoreCase = true) ||
                    it.sku.equals(clean, ignoreCase = true)
            }
            if (found != null) {
                when (scannerActionMode) {
                    ScannerActionMode.QUICK_INBOUND -> onQuickStockStep(found, "INBOUND")
                    ScannerActionMode.QUICK_OUTBOUND -> onQuickStockStep(found, "OUTBOUND")
                    else -> {}
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Scanner Mode Selector
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Mode Operasional Pemindai Laser",
                    style = MaterialTheme.typography.labelMedium,
                    color = LaserAmber500
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScannerActionMode.entries.forEach { mode ->
                        FilterChip(
                            selected = scannerActionMode == mode,
                            onClick = { scannerActionMode = mode },
                            label = { Text(mode.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LaserAmber500.copy(alpha = 0.25f),
                                selectedLabelColor = LaserAmber500
                            )
                        )
                    }
                }
            }
        }

        // 2. Live CameraX + ML Kit Barcode Viewfinder OR Permission Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(265.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900)
        ) {
            if (hasCameraPermission) {
                val cameraController = remember {
                    LifecycleCameraController(context).apply {
                        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    }
                }

                DisposableEffect(lifecycleOwner, cameraController) {
                    val options = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                            Barcode.FORMAT_EAN_13,
                            Barcode.FORMAT_EAN_8,
                            Barcode.FORMAT_UPC_A,
                            Barcode.FORMAT_CODE_128,
                            Barcode.FORMAT_CODE_39,
                            Barcode.FORMAT_QR_CODE
                        )
                        .build()
                    val barcodeScanner = BarcodeScanning.getClient(options)
                    val mainExecutor = ContextCompat.getMainExecutor(context)

                    cameraController.setImageAnalysisAnalyzer(
                        mainExecutor,
                        MlKitAnalyzer(
                            listOf(barcodeScanner),
                            CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                            mainExecutor
                        ) { result ->
                            val barcodes = result?.getValue(barcodeScanner)
                            val firstValue = barcodes?.firstOrNull()?.rawValue
                            if (!firstValue.isNullOrBlank()) {
                                handleDetectedCode(firstValue)
                            }
                        }
                    )
                    cameraController.bindToLifecycle(lifecycleOwner)

                    onDispose {
                        cameraController.unbind()
                        barcodeScanner.close()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                controller = cameraController
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Laser Reticle Overlay
                    ScannerReticleOverlay()

                    // Torch Toggle & Live Status Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Slate950.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Emerald400)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ML KIT LASER AKTIF",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                torchEnabled = !torchEnabled
                                cameraController.enableTorch(torchEnabled)
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Slate950.copy(alpha = 0.75f))
                        ) {
                            Icon(
                                imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Senter Kamera",
                                tint = if (torchEnabled) LaserAmber500 else Color.White
                            )
                        }
                    }
                }
            } else {
                // Camera Permission Request UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = LaserAmber500,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Aktifkan Kamera untuk Pemindaian Barcode Optik",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mendukung EAN-13, Code-128, UPC, dan QR Code secara real-time, atau gunakan tombol pindai cepat di bawah.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LaserAmber500,
                            contentColor = Slate950
                        ),
                        modifier = Modifier.testTag("grant_camera_permission_button")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Berikan Izin Kamera", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Hardware Wedge / Manual Input & Quick Barcode Trigger Chips
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = null,
                        tint = CyberTeal400,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Input Barcode Manual / Pindai Cepat SKU Gudang",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualBarcodeInput,
                        onValueChange = { manualBarcodeInput = it },
                        placeholder = { Text("Masukkan Barcode EAN / SKU...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_barcode_input")
                    )
                    Button(
                        onClick = { handleDetectedCode(manualBarcodeInput) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberTeal400,
                            contentColor = Slate950
                        ),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("lookup_barcode_button")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Cari Barcode")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cek", fontWeight = FontWeight.Bold)
                    }
                }

                if (items.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Ketuk Barcode Aktif di Gudang (Simulasi Trigger Laser):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items.take(6).forEach { skuItem ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { handleDetectedCode(skuItem.barcode) },
                                color = Slate800,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = LaserAmber500,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${skuItem.sku} (${skuItem.barcode.takeLast(4)})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Scanned Item Result Card
        if (scannedCode.isNotBlank()) {
            if (matchedItem != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Emerald400,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SKU TERIDENTIFIKASI",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Emerald400
                                )
                            }
                            Text(
                                text = matchedItem.zone,
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberTeal400
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = matchedItem.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = "SKU: ${matchedItem.sku} • Rak: ${matchedItem.aisle} • Batch: ${matchedItem.batchNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Stok Tersedia",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${matchedItem.quantity} ${matchedItem.unit}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = LaserAmber500
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Harga Modal / Unit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatRupiah(matchedItem.unitCost),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        BarcodeLabelPreview(
                            barcodeValue = matchedItem.barcode,
                            skuValue = matchedItem.sku,
                            itemName = matchedItem.name,
                            zoneText = "${matchedItem.zone} (${matchedItem.aisle})"
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onOpenTransactionDialog(matchedItem, "INBOUND") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Emerald400,
                                    contentColor = Slate950
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+ Inbound", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { onOpenTransactionDialog(matchedItem, "OUTBOUND") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Rose400,
                                    contentColor = Slate950
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("- Outbound", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onOpenTransactionDialog(matchedItem, "ADJUSTMENT") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Opname", color = LaserAmber500)
                            }
                        }
                    }
                }
            } else {
                // Unrecognized barcode -> Offer instant SKU registration
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Barcode Belum Terdaftar: $scannedCode",
                            style = MaterialTheme.typography.titleMedium,
                            color = LaserAmber500
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Kode barcode ini belum ada di database gudang. Anda dapat langsung mendaftarkannya sebagai SKU baru.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onRegisterUnrecognizedBarcode(scannedCode) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LaserAmber500,
                                contentColor = Slate950
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddBox, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Daftarkan '$scannedCode' Sebagai SKU Baru",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerReticleOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_line")
    val laserOffsetRatio by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val boxWidth = size.width * 0.76f
        val boxHeight = size.height * 0.58f
        val left = (size.width - boxWidth) / 2f
        val top = (size.height - boxHeight) / 2f

        // Reticle frame
        drawRoundRect(
            color = CyberTeal400.copy(alpha = 0.85f),
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(24f, 24f),
            style = Stroke(width = 3.dp.toPx())
        )

        // Animated laser line
        val laserY = top + (boxHeight * laserOffsetRatio)
        drawLine(
            color = LaserAmber500,
            start = Offset(left + 12.dp.toPx(), laserY),
            end = Offset(left + boxWidth - 12.dp.toPx(), laserY),
            strokeWidth = 3.dp.toPx()
        )
    }
}
