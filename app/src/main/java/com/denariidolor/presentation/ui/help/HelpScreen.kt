/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denariidolor.R
import com.denariidolor.presentation.ui.common.ScreenHeader
import com.denariidolor.util.Constants

const val HELP_LIST_TAG = "helpList"
const val HELP_QUESTION_TAG_PREFIX = "helpQuestion_"
const val HELP_ANSWER_TAG_PREFIX = "helpAnswer_"
const val HELP_FEEDBACK_TAG = "helpSendFeedback"

/** One question and its answer. Answers name other screens through their own string resources, so every language matches the UI. */
data class FaqEntry(val key: String, val question: String, val answer: String)

@Composable
fun HelpRoute(onBack: () -> Unit, onSendFeedback: () -> Unit) {
    HelpScreen(entries = faqEntries(), onBack = onBack, onSendFeedback = onSendFeedback)
}

@Composable
fun HelpScreen(entries: List<FaqEntry>, onBack: () -> Unit, onSendFeedback: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(HELP_LIST_TAG),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { ScreenHeader(title = stringResource(R.string.help_title), onBack = onBack) }
        items(entries, key = { it.key }) { entry -> FaqItem(entry) }
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(text = stringResource(R.string.help_contact_title), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.help_contact_body),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onSendFeedback, modifier = Modifier.fillMaxWidth().testTag(HELP_FEEDBACK_TAG)) {
                    Text(stringResource(R.string.settings_send_feedback))
                }
            }
        }
    }
}

@Composable
private fun FaqItem(entry: FaqEntry) {
    var expanded by rememberSaveable(entry.key) { mutableStateOf(false) }
    val state = stringResource(if (expanded) R.string.help_expanded else R.string.help_collapsed)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button) { expanded = !expanded }
                .semantics { stateDescription = state }
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag(HELP_QUESTION_TAG_PREFIX + entry.key),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = entry.question, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text = entry.answer,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .testTag(HELP_ANSWER_TAG_PREFIX + entry.key)
            )
        }
    }
}

@Composable
private fun faqEntries(): List<FaqEntry> = listOf(
    FaqEntry("data", stringResource(R.string.faq_data_q), stringResource(R.string.faq_data_a)),
    FaqEntry(
        "forgot_pin",
        stringResource(R.string.faq_forgot_pin_q),
        stringResource(R.string.faq_forgot_pin_a, stringResource(R.string.forgot_pin), stringResource(R.string.wipe_all_data))
    ),
    FaqEntry(
        "signed_out",
        stringResource(R.string.faq_signed_out_q),
        stringResource(
            R.string.faq_signed_out_a,
            Constants.SESSION_TIMEOUT_MILLIS / MILLIS_PER_MINUTE,
            stringResource(R.string.settings_biometric_sign_in)
        )
    ),
    FaqEntry(
        "backup",
        stringResource(R.string.faq_backup_q),
        stringResource(R.string.faq_backup_a, stringResource(R.string.settings_backup_restore))
    ),
    FaqEntry(
        "budgets",
        stringResource(R.string.faq_budgets_q),
        stringResource(R.string.faq_budgets_a, stringResource(R.string.manage_budgets))
    ),
    FaqEntry("transfer", stringResource(R.string.faq_transfer_q), stringResource(R.string.faq_transfer_a)),
    FaqEntry("delete", stringResource(R.string.faq_delete_q), stringResource(R.string.faq_delete_a)),
    FaqEntry(
        "currency",
        stringResource(R.string.faq_currency_q),
        stringResource(R.string.faq_currency_a, stringResource(R.string.settings_currency))
    ),
    FaqEntry("reports", stringResource(R.string.faq_reports_q), stringResource(R.string.faq_reports_a, stringResource(R.string.reports))),
    FaqEntry("biometric", stringResource(R.string.faq_biometric_q), stringResource(R.string.faq_biometric_a))
)

private const val MILLIS_PER_MINUTE = 60_000L
