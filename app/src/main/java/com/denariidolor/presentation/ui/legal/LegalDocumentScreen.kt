/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui.legal

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denariidolor.R
import com.denariidolor.data.legal.LegalBlock
import com.denariidolor.data.legal.LegalDocument
import com.denariidolor.presentation.ui.common.ScreenHeader
import java.util.Locale

internal const val LEGAL_DOCUMENT_TAG = "legalDocument"

@Composable
fun LegalDocumentRoute(onBack: () -> Unit, viewModel: LegalDocumentViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LegalDocumentScreen(document = viewModel.document, state = state, onBack = onBack)
}

@StringRes
fun LegalDocument.titleRes(): Int = when (this) {
    LegalDocument.PRIVACY_POLICY -> R.string.settings_privacy_policy
    LegalDocument.OPEN_SOURCE_LICENSES -> R.string.settings_licenses
}

@Composable
fun LegalDocumentScreen(document: LegalDocument, state: LegalUiState, onBack: () -> Unit) {
    val englishOnly = LocalLocale.current.platformLocale.language != Locale.ENGLISH.language
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag(LEGAL_DOCUMENT_TAG),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { ScreenHeader(title = stringResource(document.titleRes()), onBack = onBack) }
        if (englishOnly) {
            item {
                Text(
                    text = stringResource(R.string.legal_english_only),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        when (state) {
            LegalUiState.Loading -> item { CircularProgressIndicator() }
            LegalUiState.Failed -> item { Text(stringResource(R.string.legal_load_failed)) }
            is LegalUiState.Loaded -> items(state.blocks) { block ->
                // The documents are English, so they read left to right even when the app is in Arabic.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { LegalBlockText(block) }
            }
        }
    }
}

@Composable
private fun LegalBlockText(block: LegalBlock) {
    when (block) {
        is LegalBlock.Heading -> Text(
            text = block.text,
            style = if (block.level <= 1) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
        is LegalBlock.Paragraph -> Text(text = block.text, style = MaterialTheme.typography.bodyMedium)
        is LegalBlock.Bullet -> Row {
            Text(text = "•", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 8.dp))
            Text(text = block.text, style = MaterialTheme.typography.bodyMedium)
        }
        is LegalBlock.Row -> Column {
            Text(text = block.cells.first(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            if (block.cells.size > 1) {
                Text(text = block.cells.drop(1).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
