/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.denariidolor.R
import com.denariidolor.presentation.platform.NoticeLength
import com.denariidolor.presentation.platform.rememberAppDetails
import com.denariidolor.presentation.platform.rememberExternalApps
import com.denariidolor.presentation.platform.rememberNotices
import com.denariidolor.presentation.platform.rememberShareSheet
import com.denariidolor.util.Constants
import com.denariidolor.util.StoreLinks

/**
 * Rate, share and feedback. Each hands off to another app (Google Play, the share sheet, an email app); Denarii Dolor itself
 * sends nothing and still needs no Internet permission.
 */
class SupportActions(val rateApp: () -> Unit, val shareApp: () -> Unit, val sendFeedback: () -> Unit)

@Composable
fun rememberSupportActions(): SupportActions {
    val app = rememberAppDetails()
    val externalApps = rememberExternalApps()
    val shareSheet = rememberShareSheet()
    val notices = rememberNotices()
    val shareUrl = remember(app) { StoreLinks.shareUrl(app.packageName) }
    // Strings are read during composition (Compose lint forbids Context.getString here) and captured by the actions.
    val shareTitle = stringResource(R.string.share_app_chooser)
    val shareMessage = stringResource(R.string.share_app_message, shareUrl)
    val feedbackSubject = stringResource(R.string.feedback_email_subject, app.versionName)
    val feedbackBody = stringResource(R.string.feedback_email_body, app.versionName, app.osVersion)
    val noEmailApp = stringResource(R.string.feedback_no_email_app, Constants.SUPPORT_EMAIL)
    val noStore = stringResource(R.string.rate_unavailable)
    return remember(externalApps, shareSheet, notices, shareTitle, shareMessage, feedbackSubject, feedbackBody, noEmailApp, noStore) {
        SupportActions(
            rateApp = { if (!externalApps.openStoreListing()) notices.show(noStore, NoticeLength.LONG) },
            shareApp = { shareSheet.shareText(shareTitle, shareMessage) },
            sendFeedback = {
                if (!externalApps.composeEmail(Constants.SUPPORT_EMAIL, feedbackSubject, feedbackBody)) {
                    notices.show(noEmailApp, NoticeLength.LONG)
                }
            }
        )
    }
}
