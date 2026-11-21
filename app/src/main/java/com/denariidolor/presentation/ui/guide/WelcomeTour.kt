/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui.guide

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.denariidolor.R
import kotlinx.coroutines.launch

const val WELCOME_TOUR_TAG = "welcomeTour"
const val WELCOME_TOUR_SKIP_TAG = "welcomeTourSkip"
const val WELCOME_TOUR_NEXT_TAG = "welcomeTourNext"
const val WELCOME_TOUR_INDICATOR_TAG = "welcomeTourIndicator"

private data class TourPage(val icon: ImageVector, @StringRes val title: Int, @StringRes val body: Int)

private val TOUR_PAGES = listOf(
    TourPage(Icons.Outlined.Lock, R.string.tour_private_title, R.string.tour_private_body),
    TourPage(Icons.Outlined.AddCircleOutline, R.string.tour_add_title, R.string.tour_add_body),
    TourPage(Icons.Outlined.Savings, R.string.tour_budget_title, R.string.tour_budget_body),
    TourPage(Icons.Outlined.Assessment, R.string.tour_reports_title, R.string.tour_reports_body)
)

/** A few swipeable pages shown once after a new profile is set up. Skip, Back and the last page's button all end it. */
@Composable
fun WelcomeTour(onFinish: () -> Unit) {
    val pagerState = rememberPagerState { TOUR_PAGES.size }
    val scope = rememberCoroutineScope()
    val onLastPage = pagerState.currentPage == TOUR_PAGES.lastIndex
    BackHandler(onBack = onFinish)

    Surface(modifier = Modifier.fillMaxSize().testTag(WELCOME_TOUR_TAG)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Fixed height so the pages don't jump when Skip disappears on the last one.
            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.CenterEnd) {
                if (!onLastPage) {
                    TextButton(onClick = onFinish, modifier = Modifier.testTag(WELCOME_TOUR_SKIP_TAG)) {
                        Text(stringResource(R.string.tour_skip))
                    }
                }
            }
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { index ->
                TourPageContent(TOUR_PAGES[index])
            }
            PageIndicator(count = TOUR_PAGES.size, current = pagerState.currentPage)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (onLastPage) onFinish() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier.fillMaxWidth().testTag(WELCOME_TOUR_NEXT_TAG)
            ) {
                Text(stringResource(if (onLastPage) R.string.tour_get_started else R.string.tour_next))
            }
        }
    }
}

@Composable
private fun TourPageContent(page: TourPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(96.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(page.title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = stringResource(page.body), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PageIndicator(count: Int, current: Int) {
    val description = stringResource(R.string.tour_page_indicator, current + 1, count)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(WELCOME_TOUR_INDICATOR_TAG)
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(count) { index ->
            val color = if (index == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (index == current) 10.dp else 8.dp)
                    .background(color, CircleShape)
            )
        }
    }
}
