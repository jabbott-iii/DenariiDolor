/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui.common

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.denariidolor.presentation.platform.rememberNotices
import kotlinx.coroutines.flow.Flow

/** The message in the device's language, from the string resource its key names (StringKeyResources.kt). */
fun UiMessage.resolve(context: Context): String = when (this) {
    is UiMessage.Resource -> if (arg == null) context.getString(key.resId) else context.getString(key.resId, arg)
    is UiMessage.Plural -> context.resources.getQuantityString(key.resId, count, count)
}

@Composable
fun UiMessageEffect(messages: Flow<UiMessage>) {
    val notices = rememberNotices()
    LaunchedEffect(messages) {
        messages.collect { message -> notices.show(message) }
    }
}
