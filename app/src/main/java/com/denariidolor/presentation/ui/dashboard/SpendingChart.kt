package com.denariidolor.presentation.ui.dashboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.denariidolor.presentation.ui.common.LocalVizColors
import com.denariidolor.util.formatMoney
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import kotlin.math.roundToLong

const val SpendingChartTag = "spendingChart"
private const val MAX_LABEL_CHARS = 9

/** Single-series column chart (categorical slot 1). The title names the series, so no legend; values are listed below it. */
@Composable
fun SpendingChart(bars: List<Pair<String, Double>>, description: String, modifier: Modifier = Modifier) {
    val viz = LocalVizColors.current
    val colors = MaterialTheme.colorScheme
    val model = remember(bars) { entryModelOf(bars.mapIndexed { index, (_, amount) -> entryOf(index, amount) }) }
    val labels = remember(bars) { bars.map { (label, _) -> shorten(label) } }

    ProvideChartStyle(
        m3ChartStyle(
            axisLabelColor = colors.onSurfaceVariant,
            axisGuidelineColor = colors.outlineVariant,
            axisLineColor = colors.outlineVariant,
            entityColors = listOf(viz.series1)
        )
    ) {
        Chart(
            chart = columnChart(
                columns = listOf(
                    lineComponent(
                        color = viz.series1,
                        thickness = 20.dp,
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                    )
                ),
                spacing = 16.dp
            ),
            model = model,
            startAxis = rememberStartAxis(
                valueFormatter = AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ -> compactMoney(value) }
            ),
            bottomAxis = rememberBottomAxis(
                valueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                    labels.getOrNull(value.roundToLong().toInt()).orEmpty()
                }
            ),
            isZoomEnabled = false,
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .semantics { contentDescription = description }
        )
    }
}

private fun shorten(label: String): String =
    if (label.length <= MAX_LABEL_CHARS) label else label.take(MAX_LABEL_CHARS - 1) + "…"

internal fun compactMoney(value: Float): String = when {
    value >= 1_000_000f -> "$" + String.format(java.util.Locale.US, "%.1fM", value / 1_000_000f)
    value >= 1_000f -> "$" + String.format(java.util.Locale.US, "%.1fK", value / 1_000f)
    else -> formatMoney(value.toDouble()).substringBefore(".00")
}
