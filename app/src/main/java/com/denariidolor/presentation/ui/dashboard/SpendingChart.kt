/*
 * Copyright 2026 Joseph Anthony Abbott III
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import com.denariidolor.util.Money
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
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.roundToLong

const val SpendingChartTag = "spendingChart"
private const val MAX_LABEL_CHARS = 9

/** Single-series column chart (categorical slot 1). The title names the series, so no legend; values are listed below it. */
@Composable
fun SpendingChart(bars: List<Pair<String, Long>>, description: String, modifier: Modifier = Modifier) {
    val viz = LocalVizColors.current
    val colors = MaterialTheme.colorScheme
    val model = remember(bars) { entryModelOf(bars.mapIndexed { index, (_, cents) -> entryOf(index, Money.toDouble(cents)) }) }
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

/** Axis labels in dollars: `$5`, `$12.5`, `$1.5K`, `$2.0M`. */
internal fun compactMoney(dollars: Float): String = when {
    dollars >= 1_000_000f -> "$" + String.format(Locale.US, "%.1fM", dollars / 1_000_000f)
    dollars >= 1_000f -> "$" + String.format(Locale.US, "%.1fK", dollars / 1_000f)
    else -> "$" + BigDecimal(dollars.toString()).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}
