package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AbcSummary
import com.example.data.model.StockMovement
import com.example.data.model.ZoneStat
import com.example.ui.theme.CyberTeal400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Indigo500
import com.example.ui.theme.LaserAmber500
import com.example.ui.theme.Rose400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Violet500
import java.text.NumberFormat
import java.util.Locale

fun formatRupiah(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}

fun formatCompactRupiah(amount: Double): String {
    return when {
        amount >= 1_000_000_000 -> String.format(Locale("id", "ID"), "Rp %.2f M", amount / 1_000_000_000.0)
        amount >= 1_000_000 -> String.format(Locale("id", "ID"), "Rp %.1f Jt", amount / 1_000_000.0)
        amount >= 1_000 -> String.format(Locale("id", "ID"), "Rp %.0f Rb", amount / 1_000.0)
        else -> formatRupiah(amount)
    }
}

@Composable
fun StockHealthDonutChart(
    healthyCount: Int,
    lowStockCount: Int,
    outOfStockCount: Int,
    overstockCount: Int,
    modifier: Modifier = Modifier
) {
    val total = (healthyCount + lowStockCount + outOfStockCount + overstockCount).coerceAtLeast(1)
    val segments = listOf(
        Triple("Optimal", healthyCount, Emerald400),
        Triple("Menipis", lowStockCount, LaserAmber500),
        Triple("Habis (0)", outOfStockCount, Rose400),
        Triple("Overstock", overstockCount, CyberTeal400)
    )

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "donut_anim"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Kesehatan Stok & Distribusi Status",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Pemantauan ambang batas min/maks waktu nyata",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = Slate800,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$total SKU",
                        style = MaterialTheme.typography.labelMedium,
                        color = LaserAmber500,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 20.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                        val arcSize = Size(diameter, diameter)

                        drawArc(
                            color = Slate800,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth)
                        )

                        var currentStartAngle = -90f
                        segments.forEach { (_, count, color) ->
                            if (count > 0) {
                                val sweep = (count.toFloat() / total.toFloat()) * 360f * animProgress
                                drawArc(
                                    color = color,
                                    startAngle = currentStartAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                                currentStartAngle += sweep
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val healthyPct = ((healthyCount.toFloat() / total.toFloat()) * 100f).toInt()
                        Text(
                            text = "$healthyPct%",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Emerald400
                        )
                        Text(
                            text = "Optimal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    segments.forEach { (label, count, color) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "$count SKU",
                                style = MaterialTheme.typography.labelMedium,
                                color = color
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ZoneCapacityHeatmapCard(
    zoneStats: Map<String, ZoneStat>,
    modifier: Modifier = Modifier
) {
    val defaultZones = listOf(
        "Zona A - Fast Moving",
        "Zona B - Bulk Storage",
        "Zona C - Cold Chain",
        "Zona D - Hazmat/Khusus"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Kapasitas & Utilisasi Zona Gudang",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = "Kepadatan rak penyimpanan terhadap kapasitas maksimum SKU",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            val zonesToRender = if (zoneStats.isNotEmpty()) zoneStats.keys.toList() else defaultZones

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                zonesToRender.forEach { zoneName ->
                    val stat = zoneStats[zoneName] ?: ZoneStat(zoneName, 0, 0L, 100L, 0.0)
                    val pct = stat.utilizationPercent
                    val barColor = when {
                        pct >= 90f -> Rose400
                        pct >= 70f -> LaserAmber500
                        pct > 0f -> CyberTeal400
                        else -> Slate700
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = zoneName,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${stat.currentUnits}/${stat.maxCapacityUnits} Unit (${pct.toInt()}%)",
                                style = MaterialTheme.typography.labelMedium,
                                color = barColor
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Slate800)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = (pct / 100f).coerceIn(0.02f, 1f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(barColor.copy(alpha = 0.7f), barColor)
                                        )
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${stat.skuCount} SKU Aktif",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Valuasi: ${formatCompactRupiah(stat.totalValue)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AbcParetoCard(
    abcSummary: AbcSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Klasifikasi Inventaris Pareto (ABC)",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Segmentasi otomatis berdasarkan kontribusi nilai modal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = LaserAmber500.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "PARETO 70/20/10",
                        style = MaterialTheme.typography.labelSmall,
                        color = LaserAmber500,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AbcTierPill(
                    tier = "KELAS A",
                    subtitle = "Nilai Tinggi",
                    skuCount = abcSummary.classAItems.size,
                    valueShare = abcSummary.classAValueShare,
                    accentColor = LaserAmber500,
                    modifier = Modifier.weight(1f)
                )
                AbcTierPill(
                    tier = "KELAS B",
                    subtitle = "Nilai Menengah",
                    skuCount = abcSummary.classBItems.size,
                    valueShare = abcSummary.classBValueShare,
                    accentColor = CyberTeal400,
                    modifier = Modifier.weight(1f)
                )
                AbcTierPill(
                    tier = "KELAS C",
                    subtitle = "Volume Massal",
                    skuCount = abcSummary.classCItems.size,
                    valueShare = abcSummary.classCValueShare,
                    accentColor = Indigo500,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AbcTierPill(
    tier: String,
    subtitle: String,
    skuCount: Int,
    valueShare: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Slate800.copy(alpha = 0.7f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = tier,
                style = MaterialTheme.typography.labelMedium,
                color = accentColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${String.format(Locale.US, "%.1f", valueShare)}%",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$skuCount SKU",
                style = MaterialTheme.typography.labelSmall,
                color = accentColor
            )
        }
    }
}

@Composable
fun MovementFlowVelocityChart(
    movements: List<StockMovement>,
    modifier: Modifier = Modifier
) {
    val inboundTotal = movements.filter { it.type == "INBOUND" }.sumOf { it.quantityDelta }
    val outboundTotal = movements.filter { it.type == "OUTBOUND" }.sumOf { it.quantityDelta }
    val adjustmentTotal = movements.filter { it.type == "ADJUSTMENT" }.sumOf { it.quantityDelta }
    val transferTotal = movements.filter { it.type == "TRANSFER" }.sumOf { it.quantityDelta }

    val maxVal = listOf(inboundTotal, outboundTotal, adjustmentTotal, transferTotal).maxOrNull()?.coerceAtLeast(1L) ?: 1L

    val bars = listOf(
        Triple("Inbound", inboundTotal, Emerald400),
        Triple("Outbound", outboundTotal, Rose400),
        Triple("Opname/Adj", adjustmentTotal, LaserAmber500),
        Triple("Transfer", transferTotal, Violet500)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Arus Kecepatan Mutasi Gudang (Real-Time)",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = "Akumulasi pergerakan unit berdasarkan tipe transaksi",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val barCount = bars.size
                val slotWidth = size.width / barCount
                val barWidth = (slotWidth * 0.48f).coerceAtMost(64.dp.toPx())
                val maxBarHeight = size.height - 24.dp.toPx()

                bars.forEachIndexed { index, (_, value, color) ->
                    val ratio = (value.toFloat() / maxVal.toFloat()).coerceIn(0.06f, 1f)
                    val barHeight = maxBarHeight * ratio
                    val x = (index * slotWidth) + (slotWidth - barWidth) / 2f
                    val y = size.height - barHeight

                    // Background track
                    drawRoundRect(
                        color = Slate800,
                        topLeft = Offset(x, size.height - maxBarHeight),
                        size = Size(barWidth, maxBarHeight),
                        cornerRadius = CornerRadius(12f, 12f)
                    )

                    // Active bar
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                bars.forEach { (label, value, color) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$value",
                            style = MaterialTheme.typography.labelLarge,
                            color = color
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
