package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Slate950
import kotlin.math.abs

/**
 * Renders a deterministic high-contrast 1D industrial barcode graphic from any barcode or SKU string,
 * suitable for warehouse rack labels and optical verification.
 */
@Composable
fun BarcodeLabelPreview(
    barcodeValue: String,
    skuValue: String,
    itemName: String,
    zoneText: String,
    modifier: Modifier = Modifier
) {
    val cleanCode = barcodeValue.ifBlank { "8990001234567" }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = skuValue,
                    style = MaterialTheme.typography.labelMedium,
                    color = Slate950
                )
                Text(
                    text = zoneText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate950.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
            ) {
                val totalModules = 76
                val moduleWidth = size.width / totalModules
                var currentX = moduleWidth * 2

                // Start guard pattern
                drawRect(Color.Black, Offset(currentX, 0f), Size(moduleWidth * 1.5f, size.height))
                currentX += moduleWidth * 3f
                drawRect(Color.Black, Offset(currentX, 0f), Size(moduleWidth * 1.5f, size.height))
                currentX += moduleWidth * 3f

                cleanCode.forEachIndexed { idx, ch ->
                    val seed = abs(ch.code * 31 + idx * 17)
                    val barThickness = ((seed % 3) + 1) * moduleWidth * 0.85f
                    val gapThickness = (((seed / 3) % 2) + 1) * moduleWidth * 0.9f

                    if (currentX + barThickness < size.width - moduleWidth * 8) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(currentX, 0f),
                            size = Size(barThickness, size.height * 0.88f)
                        )
                        currentX += barThickness + gapThickness

                        // Secondary sub-bar for dense Code-128 look
                        val subBar = ((seed % 2) + 1) * moduleWidth * 0.65f
                        if (currentX + subBar < size.width - moduleWidth * 8) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(currentX, 0f),
                                size = Size(subBar, size.height * 0.88f)
                            )
                            currentX += subBar + moduleWidth
                        }
                    }
                }

                // End guard pattern
                val endX = size.width - moduleWidth * 6
                drawRect(Color.Black, Offset(endX, 0f), Size(moduleWidth * 1.5f, size.height))
                drawRect(Color.Black, Offset(endX + moduleWidth * 3f, 0f), Size(moduleWidth * 1.5f, size.height))
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = cleanCode.chunked(4).joinToString(" "),
                style = MaterialTheme.typography.labelLarge,
                color = Slate950
            )
            Text(
                text = itemName,
                style = MaterialTheme.typography.bodySmall,
                color = Slate950.copy(alpha = 0.75f),
                maxLines = 1
            )
        }
    }
}
