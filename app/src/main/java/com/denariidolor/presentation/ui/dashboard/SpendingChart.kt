/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui.dashboard

import android.icu.text.CompactDecimalFormat
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.denariidolor.presentation.ui.common.LocalVizColors
import com.denariidolor.util.Money
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.shape.rounded
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import kotlin.math.roundToInt

const val SPENDING_CHART_TAG = "spendingChart"
private const val MAX_LABEL_CHARS = 9

/** Single-series column chart (categorical slot 1). The title names the series, so no legend; values are listed below it. */
@Composable
fun SpendingChart(bars: List<Pair<String, Long>>, description: String, modifier: Modifier = Modifier) {
    if (bars.isEmpty()) return // Vico's layer model rejects an empty series.
    val viz = LocalVizColors.current
    val colors = MaterialTheme.colorScheme
    val model = remember(bars) {
        CartesianChartModel(ColumnCartesianLayerModel.build { series(bars.map { (_, cents) -> Money.toDouble(cents) }) })
    }
    val labels = remember(bars) { bars.map { (label, _) -> shorten(label) } }
    // Axis labels in the language's short form (`1.5K`, `1,5 k`, `1.5万`); the amounts listed under the chart carry the currency.
    val locale = LocalLocale.current.platformLocale
    val axisFormat = remember(locale) { CompactDecimalFormat.getInstance(locale, CompactDecimalFormat.CompactStyle.SHORT) }
    val yFormatter = remember(axisFormat) { CartesianValueFormatter { _, value, _ -> axisFormat.format(value) } }
    // Vico requires non-empty axis labels.
    val xFormatter = remember(labels) { CartesianValueFormatter { _, x, _ -> labels.getOrNull(x.roundToInt()) ?: " " } }

    ProvideVicoTheme(
        rememberM3VicoTheme(
            columnCartesianLayerColors = listOf(viz.series1),
            lineColor = colors.outlineVariant,
            textColor = colors.onSurfaceVariant
        )
    ) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(
                    columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                        rememberLineComponent(
                            fill = fill(viz.series1),
                            thickness = 20.dp,
                            shape = CorneredShape.rounded(topLeft = 4.dp, topRight = 4.dp)
                        )
                    ),
                    columnCollectionSpacing = 16.dp
                ),
                startAxis = VerticalAxis.rememberStart(valueFormatter = yFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = xFormatter)
            ),
            model = model,
            scrollState = rememberVicoScrollState(scrollEnabled = false),
            zoomState = rememberVicoZoomState(zoomEnabled = false),
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .semantics { contentDescription = description }
        )
    }
}

private fun shorten(label: String): String = if (label.length <= MAX_LABEL_CHARS) label else label.take(MAX_LABEL_CHARS - 1) + "…"
