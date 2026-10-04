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

import android.icu.text.CompactDecimalFormat
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import java.util.Locale
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
    val locale = Locale.getDefault(Locale.Category.FORMAT)
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
