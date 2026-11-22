/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.platform

import android.net.Uri
import com.denariidolor.presentation.ui.common.UiMessage
import com.denariidolor.util.DateUtils
import kotlinx.datetime.LocalDate

// What screens ask of the platform, as plain Kotlin. AndroidPlatformActions.kt implements it with toasts, intents and the
// system pickers, and no screen calls those directly (PlatformBoundaryTest). Uri stays the file handle for now.

enum class NoticeLength { SHORT, LONG }

/** A short message over the screen: a toast on Android. */
interface Notices {
    fun show(text: String, length: NoticeLength = NoticeLength.SHORT)

    fun show(message: UiMessage, length: NoticeLength = NoticeLength.SHORT)
}

/** The system share sheet. */
interface ShareSheet {
    fun shareText(chooserTitle: String, text: String)

    /** Offers the file at [uri]; the app the user picks may read that one file and nothing else. */
    fun shareFile(chooserTitle: String, uri: Uri, mimeType: String)
}

/** Hands off to other apps; the app itself sends nothing. Each call returns false when no app on the device takes it. */
interface ExternalApps {
    /** This app's store listing: in the store app when it's installed, otherwise in a browser. */
    fun openStoreListing(): Boolean

    /** A new email for the user to review and send from their own email app. */
    fun composeEmail(to: String, subject: String, body: String): Boolean
}

/** What the share link and the feedback email say about this install. */
data class AppDetails(val packageName: String, val versionName: String, val osVersion: String)

/** The system date picker. [onPicked] runs only when the user picks a date, not when they dismiss the picker. */
fun interface DatePicker {
    fun pick(initial: LocalDate, onPicked: (LocalDate) -> Unit)
}

/** A system file picker. The file the user chooses, or null when they back out, goes to the callback its factory was given. */
fun interface FilePicker<I> {
    fun launch(input: I)
}

/** The day a date field's picker opens on: the ISO date the field holds, or [today] when it holds none. */
internal fun dateFieldStart(fieldText: String, today: LocalDate): LocalDate =
    fieldText.takeIf { it.isNotBlank() }?.let { runCatching { DateUtils.parseIsoDate(it) }.getOrNull() } ?: today

/** Opens the picker for a date field that holds ISO text, and hands the picked date back in the same form. */
internal fun DatePicker.pickForField(fieldText: String, today: LocalDate, onPicked: (String) -> Unit) {
    pick(dateFieldStart(fieldText, today)) { onPicked(it.toString()) }
}
