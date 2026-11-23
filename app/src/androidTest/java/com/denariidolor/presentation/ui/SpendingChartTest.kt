/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.denariidolor.presentation.ui.common.DenariiDolorTheme
import com.denariidolor.presentation.ui.common.LocalVizColors
import com.denariidolor.presentation.ui.dashboard.SPENDING_CHART_TAG
import com.denariidolor.presentation.ui.dashboard.SpendingChart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Dashboard's spending chart draws on a canvas, so only its pixels show what it drew: the bars, in the series color, with
 * the larger amount the taller. These guard upgrades of the chart library.
 */
@RunWith(AndroidJUnit4::class)
class SpendingChartTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun barsAreDrawnInTheSeriesColorAndScaleWithTheAmounts() {
        var series = Color.Unspecified
        composeRule.setContent {
            DenariiDolorTheme(darkTheme = false) {
                series = LocalVizColors.current.series1
                SpendingChart(listOf("Dining" to 12_000L, "Rent" to 80_000L), DESCRIPTION, Modifier.testTag(SPENDING_CHART_TAG))
            }
        }
        val chart = composeRule.onNodeWithTag(SPENDING_CHART_TAG).assertContentDescriptionEquals(DESCRIPTION)

        val bars = barHeights(chart.captureToImage(), series.toArgb())

        assertEquals(bars.toString(), 2, bars.size)
        // Rent is 6.7 times Dining; the rounded tops and the pixel grid take a little off each bar.
        assertTrue(bars.toString(), bars[1] > bars[0] * 5)
    }

    /** The height in pixels of each bar, left to right: runs of pixel columns that hold [argb], measured where tallest. */
    private fun barHeights(image: ImageBitmap, argb: Int): List<Int> {
        val pixels = IntArray(image.width * image.height).also { image.readPixels(it) }
        val columns = (0 until image.width).map { x -> (0 until image.height).count { y -> pixels[y * image.width + x] == argb } }
        val bars = mutableListOf<Int>()
        var tallest = 0
        columns.forEach { height ->
            if (height > 0) {
                tallest = maxOf(tallest, height)
            } else if (tallest > 0) {
                bars += tallest
                tallest = 0
            }
        }
        if (tallest > 0) bars += tallest
        return bars
    }

    private companion object {
        const val DESCRIPTION = "Spending by category"
    }
}
