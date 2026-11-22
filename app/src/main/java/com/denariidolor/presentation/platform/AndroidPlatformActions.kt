/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.platform

import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.denariidolor.presentation.ui.common.UiMessage
import com.denariidolor.util.StoreLinks
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

// Android's side of PlatformActions.kt, and the one presentation file besides the activities that touches the Context,
// toasts, intents or the system pickers. Each remember function binds its object to the activity the screen runs in.

@Composable
fun rememberNotices(): Notices {
    val context = LocalContext.current
    return remember(context) { ToastNotices(context) }
}

@Composable
fun rememberShareSheet(): ShareSheet {
    val context = LocalContext.current
    return remember(context) { AndroidShareSheet(context) }
}

@Composable
fun rememberExternalApps(): ExternalApps {
    val context = LocalContext.current
    return remember(context) { AndroidExternalApps(context.packageName, context::tryStart) }
}

@Composable
fun rememberAppDetails(): AppDetails {
    val context = LocalContext.current
    return remember(context) { AppDetails(context.packageName, context.appVersionName(), Build.VERSION.RELEASE) }
}

@Composable
fun rememberDatePicker(): DatePicker {
    val context = LocalContext.current
    return remember(context) {
        DatePicker { initial, onPicked ->
            // DatePickerDialog counts months from 0.
            DatePickerDialog(
                context,
                { _, year, month, day -> onPicked(LocalDate(year, month + 1, day)) },
                initial.year,
                initial.month.number - 1,
                initial.day
            ).show()
        }
    }
}

/**
 * Asks where to save a new file of [mimeType], suggesting the name it's launched with. A device with no document picker (some
 * managed or TV builds) can't open one, so the picker calls [onUnavailable] instead of crashing the app (BUG-13).
 */
@Composable
fun rememberSaveFilePicker(mimeType: String, onUnavailable: () -> Unit, onPicked: (Uri?) -> Unit): FilePicker<String> {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(mimeType), onPicked)
    return rememberFilePicker(launcher, onUnavailable) { it }
}

/** Asks which file to open, among the MIME types it's launched with. A missing picker is handled as in [rememberSaveFilePicker]. */
@Composable
fun rememberOpenFilePicker(onUnavailable: () -> Unit, onPicked: (Uri?) -> Unit): FilePicker<List<String>> {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), onPicked)
    return rememberFilePicker(launcher, onUnavailable) { it.toTypedArray() }
}

@Composable
private fun <I, L> rememberFilePicker(launcher: ActivityResultLauncher<L>, onUnavailable: () -> Unit, toLaunch: (I) -> L): FilePicker<I> {
    val currentOnUnavailable by rememberUpdatedState(onUnavailable)
    return remember(launcher) {
        FilePicker { input ->
            try {
                launcher.launch(toLaunch(input))
            } catch (_: ActivityNotFoundException) {
                currentOnUnavailable()
            }
        }
    }
}

/** The intents handed to other apps, built apart from starting them so a device test can check each one. */
internal object PlatformIntents {
    private const val PLAY_STORE_PACKAGE = "com.android.vending"

    fun shareText(chooserTitle: String, text: String): Intent {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        return Intent.createChooser(send, chooserTitle)
    }

    fun shareFile(chooserTitle: String, uri: Uri, mimeType: String): Intent {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, chooserTitle)
    }

    fun storeApp(packageName: String): Intent =
        Intent(Intent.ACTION_VIEW, StoreLinks.marketUri(packageName).toUri()).setPackage(PLAY_STORE_PACKAGE)

    fun storeWeb(packageName: String): Intent = Intent(Intent.ACTION_VIEW, StoreLinks.webUrl(packageName).toUri())

    fun email(to: String, subject: String, body: String): Intent =
        Intent(Intent.ACTION_SENDTO, StoreLinks.mailto(to, subject, body).toUri())
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, body)
}

internal class AndroidExternalApps(private val packageName: String, private val tryStart: (Intent) -> Boolean) : ExternalApps {
    override fun openStoreListing(): Boolean =
        tryStart(PlatformIntents.storeApp(packageName)) || tryStart(PlatformIntents.storeWeb(packageName))

    override fun composeEmail(to: String, subject: String, body: String): Boolean = tryStart(PlatformIntents.email(to, subject, body))
}

private class ToastNotices(private val context: Context) : Notices {
    override fun show(text: String, length: NoticeLength) {
        val duration = when (length) {
            NoticeLength.SHORT -> Toast.LENGTH_SHORT
            NoticeLength.LONG -> Toast.LENGTH_LONG
        }
        Toast.makeText(context, text, duration).show()
    }

    override fun show(message: UiMessage, length: NoticeLength) = show(message.resolve(context), length)
}

private class AndroidShareSheet(private val context: Context) : ShareSheet {
    override fun shareText(chooserTitle: String, text: String) {
        context.tryStart(PlatformIntents.shareText(chooserTitle, text))
    }

    override fun shareFile(chooserTitle: String, uri: Uri, mimeType: String) =
        context.startActivity(PlatformIntents.shareFile(chooserTitle, uri, mimeType))
}

private fun Context.tryStart(intent: Intent): Boolean = try {
    startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
}

private fun Context.appVersionName(): String = try {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION") // The flags overload above needs API 33.
        packageManager.getPackageInfo(packageName, 0)
    }
    info.versionName.orEmpty()
} catch (_: PackageManager.NameNotFoundException) {
    ""
}
