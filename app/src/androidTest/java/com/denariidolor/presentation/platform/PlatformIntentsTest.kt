/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.platform

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.denariidolor.util.Constants
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The intents the app hands to other apps, exactly as the screens built them before they moved behind the platform
 * interfaces. Each one only hands off: the app itself sends nothing and has no Internet permission.
 */
@RunWith(AndroidJUnit4::class)
class PlatformIntentsTest {
    @Test
    fun sharingTheAppOffersPlainTextInAChooser() {
        val chooser = PlatformIntents.shareText("Share with", "Try Denarii Dolor")

        assertChooser("Share with", chooser)
        val target = target(chooser)
        assertEquals(Intent.ACTION_SEND, target.action)
        assertEquals("text/plain", target.type)
        assertEquals("Try Denarii Dolor", target.getStringExtra(Intent.EXTRA_TEXT))
        assertNull(target.clipData)
        assertEquals(0, target.flags)
    }

    /** The receiving app may read this one file and nothing else, and may not write it. */
    @Test
    fun sharingAReportGrantsReadAccessToThatFileOnly() {
        val chooser = PlatformIntents.shareFile("Share report", REPORT_URI, "text/csv")

        assertChooser("Share report", chooser)
        val target = target(chooser)
        assertEquals(Intent.ACTION_SEND, target.action)
        assertEquals("text/csv", target.type)
        assertEquals(REPORT_URI, IntentCompat.getParcelableExtra(target, Intent.EXTRA_STREAM, Uri::class.java))
        assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, target.flags)
        val clip = requireNotNull(target.clipData) { "The share has no clip data" }
        assertEquals("", clip.description.label.toString())
        assertEquals(1, clip.itemCount)
        assertEquals(REPORT_URI, clip.getItemAt(0).uri)
    }

    @Test
    fun theStoreListingOpensInThePlayStoreAppOrInABrowser() {
        val app = PlatformIntents.storeApp(PACKAGE)
        val web = PlatformIntents.storeWeb(PACKAGE)

        assertEquals(Intent.ACTION_VIEW, app.action)
        assertEquals("market://details?id=com.denariidolor", app.dataString)
        assertEquals("com.android.vending", app.`package`)
        assertEquals(Intent.ACTION_VIEW, web.action)
        assertEquals("https://play.google.com/store/apps/details?id=com.denariidolor", web.dataString)
        assertNull(web.`package`)
    }

    @Test
    fun feedbackOpensAnEmailToSupportWithItsSubjectAndBody() {
        val email = PlatformIntents.email(Constants.SUPPORT_EMAIL, "Feedback (version 1.3.0)", "Hello\nThé café & more")

        assertEquals(Intent.ACTION_SENDTO, email.action)
        assertEquals(
            "mailto:${Constants.SUPPORT_EMAIL}?subject=Feedback%20%28version%201.3.0%29" +
                "&body=Hello%0ATh%C3%A9%20caf%C3%A9%20%26%20more",
            email.dataString
        )
        assertArrayEquals(arrayOf(Constants.SUPPORT_EMAIL), email.getStringArrayExtra(Intent.EXTRA_EMAIL))
        assertEquals("Feedback (version 1.3.0)", email.getStringExtra(Intent.EXTRA_SUBJECT))
        assertEquals("Hello\nThé café & more", email.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun rateTriesThePlayStoreAppFirstThenTheBrowser() {
        val tried = mutableListOf<Intent>()
        val apps = AndroidExternalApps(PACKAGE) { intent ->
            tried += intent
            false
        }

        assertFalse(apps.openStoreListing())
        assertEquals(listOf("com.android.vending", null), tried.map { it.`package` })
        assertEquals(
            listOf("market://details?id=com.denariidolor", "https://play.google.com/store/apps/details?id=com.denariidolor"),
            tried.map { it.dataString }
        )
    }

    @Test
    fun rateStopsAtThePlayStoreAppWhenItOpens() {
        val tried = mutableListOf<Intent>()
        val apps = AndroidExternalApps(PACKAGE) { intent ->
            tried += intent
            true
        }

        assertTrue(apps.openStoreListing())
        assertEquals(listOf("com.android.vending"), tried.map { it.`package` })
    }

    @Test
    fun feedbackSaysWhetherAnEmailAppTookIt() {
        assertFalse(AndroidExternalApps(PACKAGE) { false }.composeEmail(Constants.SUPPORT_EMAIL, "Subject", "Body"))
        assertTrue(AndroidExternalApps(PACKAGE) { true }.composeEmail(Constants.SUPPORT_EMAIL, "Subject", "Body"))
    }

    private fun assertChooser(title: String, chooser: Intent) {
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        assertEquals(title, chooser.getCharSequenceExtra(Intent.EXTRA_TITLE).toString())
    }

    private fun target(chooser: Intent): Intent =
        requireNotNull(IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent::class.java)) { "The chooser has no target" }

    private companion object {
        const val PACKAGE = "com.denariidolor"
        val REPORT_URI: Uri = Uri.parse("content://com.denariidolor.fileprovider/reports/report-2026-11.csv")
    }
}
