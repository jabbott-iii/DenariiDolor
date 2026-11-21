/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui.guide

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.denariidolor.R

const val RATE_PROMPT_TAG = "ratePrompt"
const val RATE_PROMPT_RATE_TAG = "ratePromptRate"
const val RATE_PROMPT_LATER_TAG = "ratePromptLater"
const val RATE_PROMPT_NEVER_TAG = "ratePromptNever"

/** Asks once whether the user would like to rate the app. Dismissing it counts as "Not now". */
@Composable
fun RatePromptDialog(onRate: () -> Unit, onLater: () -> Unit, onNever: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        modifier = Modifier.testTag(RATE_PROMPT_TAG),
        icon = { Icon(imageVector = Icons.Outlined.StarOutline, contentDescription = null) },
        title = { Text(stringResource(R.string.rate_prompt_title)) },
        text = { Text(stringResource(R.string.rate_prompt_message)) },
        confirmButton = {
            TextButton(onClick = onRate, modifier = Modifier.testTag(RATE_PROMPT_RATE_TAG)) {
                Text(stringResource(R.string.rate_prompt_rate))
            }
        },
        // Two buttons in one slot: the dialog's button row wraps them when a language needs the room.
        dismissButton = {
            TextButton(onClick = onNever, modifier = Modifier.testTag(RATE_PROMPT_NEVER_TAG)) {
                Text(stringResource(R.string.rate_prompt_never))
            }
            TextButton(onClick = onLater, modifier = Modifier.testTag(RATE_PROMPT_LATER_TAG)) {
                Text(stringResource(R.string.rate_prompt_later))
            }
        }
    )
}
