package com.driverapp.repartidor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driverapp.repartidor.R

/**
 * Selector día/semana/mes/año (pestaña Ganancias).
 * Se reutiliza el mismo componente para las dos pestañas de Órdenes.
 */
@Composable
fun PeriodPills(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val active = index == selected
            Surface(
                onClick = { onSelect(index) },
                shape = RoundedCornerShape(50),
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            ) {
                Box(Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (active) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

/**
 * Gráfica de ganancias: dona con la distribución (entregas / propinas / bonos)
 * y barras por periodo. Reemplaza al `PieView` en Canvas del XML.
 */
@Composable
fun EarningsChart(
    deliveries: Float,
    tips: Float,
    bonuses: Float,
    bars: List<Float>,
    modifier: Modifier = Modifier
) {
    val total = (deliveries + tips + bonuses).takeIf { it > 0f } ?: 1f
    val slices = listOf(
        deliveries / total to MaterialTheme.colorScheme.primary,
        tips / total to MaterialTheme.colorScheme.secondary,
        bonuses / total to MaterialTheme.colorScheme.onSurfaceVariant
    )

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(140.dp)) {
            var startAngle = -90f
            val stroke = Stroke(width = 26.dp.toPx())
            val inset = stroke.width / 2f
            val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
            slices.forEach { (fraction, color) ->
                val sweep = 360f * fraction
                if (sweep > 0f) {
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = stroke
                    )
                }
                startAngle += sweep
            }
        }

        Spacer(Modifier.height(16.dp))

        // Barras simples del periodo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            if (bars.isEmpty()) {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val max = bars.maxOrNull()?.takeIf { it > 0f } ?: 1f
                bars.forEach { value ->
                    val heightFraction = (value / max).coerceIn(0f, 1f)
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 3.dp)
                            .fillMaxSize()
                            .background(Color.Transparent),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height((80 * heightFraction).dp.coerceAtLeast(2.dp))
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

/** Leyenda de la dona (entregas / propinas / bonos / otros). */
@Composable
fun ChartLegendRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp)
    }
}
