/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.denariidolor.R
import com.denariidolor.util.Constants
import com.denariidolor.util.StoreLinks

/**
 * Rate, share and feedback. Each hands off to another app (Google Play, the share sheet, an email app); Denarii Dolor itself
 * sends nothing and still needs no Internet permission.
 */
class SupportActions(val rateApp: () -> Unit, val shareApp: () -> Unit, val sendFeedback: () -> Unit)

@Composable
fun rememberSupportActions(): SupportActions {
    val context = LocalContext.current
    val version = remember(context) { context.appVersionName() }
    val shareUrl = remember(context) { StoreLinks.shareUrl(context.packageName) }
    // Strings are read during composition (Compose lint forbids Context.getString here) and captured by the actions.
    val shareTitle = stringResource(R.string.share_app_chooser)
    val shareMessage = stringResource(R.string.share_app_message, shareUrl)
    val feedbackSubject = stringResource(R.string.feedback_email_subject, version)
    val feedbackBody = stringResource(R.string.feedback_email_body, version, Build.VERSION.RELEASE)
    val noEmailApp = stringResource(R.string.feedback_no_email_app, Constants.SUPPORT_EMAIL)
    val noStore = stringResource(R.string.rate_unavailable)
    return remember(context, shareTitle, shareMessage, feedbackSubject, feedbackBody, noEmailApp, noStore) {
        SupportActions(
            rateApp = { if (!context.openStoreListing()) context.toast(noStore) },
            shareApp = {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareMessage)
                context.tryStart(Intent.createChooser(send, shareTitle))
            },
            sendFeedback = {
                val email = Intent(Intent.ACTION_SENDTO, StoreLinks.mailto(Constants.SUPPORT_EMAIL, feedbackSubject, feedbackBody).toUri())
                    .putExtra(Intent.EXTRA_EMAIL, arrayOf(Constants.SUPPORT_EMAIL))
                    .putExtra(Intent.EXTRA_SUBJECT, feedbackSubject)
                    .putExtra(Intent.EXTRA_TEXT, feedbackBody)
                if (!context.tryStart(email)) context.toast(noEmailApp)
            }
        )
    }
}

/** The Play Store app when it's installed, otherwise the listing in a browser. */
private fun Context.openStoreListing(): Boolean {
    val playStore = Intent(Intent.ACTION_VIEW, StoreLinks.marketUri(packageName).toUri()).setPackage(PLAY_STORE_PACKAGE)
    return tryStart(playStore) || tryStart(Intent(Intent.ACTION_VIEW, StoreLinks.webUrl(packageName).toUri()))
}

private fun Context.tryStart(intent: Intent): Boolean = try {
    startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
}

private fun Context.toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

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

private const val PLAY_STORE_PACKAGE = "com.android.vending"
