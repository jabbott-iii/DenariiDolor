/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.platform

import android.net.Uri
import com.denariidolor.presentation.ui.common.UiMessage
import com.denariidolor.util.DateUtils
import javax.crypto.Cipher
import kotlinx.datetime.LocalDate

// What screens ask of the platform, as plain Kotlin. AndroidPlatformActions.kt implements it with toasts, intents and the
// system pickers, and no screen calls those directly (PlatformBoundaryTest). Uri stays the file handle, and Cipher the
// biometric key handle, for now.

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

/** The words on the system biometric prompt. */
data class BiometricPromptText(val title: String, val subtitle: String, val cancelLabel: String)

/** How a biometric prompt ended. */
sealed interface BiometricPromptResult {
    /** The cipher the prompt authorized: only it can use the biometric-bound key (CS-07). */
    class Authorized(val cipher: Cipher) : BiometricPromptResult

    /** The user backed out, or the prompt couldn't open because the app went to the background. */
    data object Canceled : BiometricPromptResult

    /** The prompt gave up, such as after too many attempts. */
    data object Failed : BiometricPromptResult
}

/**
 * The system biometric prompt, bound to a cipher so success unlocks a Keystore key instead of only firing a callback (CS-07).
 * [authenticate] reports to `onResult` exactly once.
 */
fun interface BiometricPrompter {
    fun authenticate(cipher: Cipher, text: BiometricPromptText, onResult: (BiometricPromptResult) -> Unit)
}

/** The day a date field's picker opens on: the ISO date the field holds, or [today] when it holds none. */
internal fun dateFieldStart(fieldText: String, today: LocalDate): LocalDate =
    fieldText.takeIf { it.isNotBlank() }?.let { runCatching { DateUtils.parseIsoDate(it) }.getOrNull() } ?: today

/** Opens the picker for a date field that holds ISO text, and hands the picked date back in the same form. */
internal fun DatePicker.pickForField(fieldText: String, today: LocalDate, onPicked: (String) -> Unit) {
    pick(dateFieldStart(fieldText, today)) { onPicked(it.toString()) }
}
