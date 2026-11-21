/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreLinksTest {
    private val pkg = "com.denariidolor"

    @Test
    fun storeLinksPointAtTheAppsListing() {
        assertEquals("market://details?id=com.denariidolor", StoreLinks.marketUri(pkg))
        assertEquals("https://play.google.com/store/apps/details?id=com.denariidolor", StoreLinks.webUrl(pkg))
    }

    @Test
    fun shareLinkCarriesOnlyAFixedCampaignTag() {
        val url = StoreLinks.shareUrl(pkg)

        assertTrue(url.startsWith(StoreLinks.webUrl(pkg) + "&referrer="))
        assertTrue(url.contains("utm_source%3Dapp_share"))
        // The referrer value is encoded once, so Play reads it as one parameter.
        assertFalse(url.substringAfter("&referrer=").contains('&'))
    }

    @Test
    fun mailtoEncodesSubjectAndBodyForMailApps() {
        val mailto = StoreLinks.mailto(Constants.SUPPORT_EMAIL, "Feedback (version 1.3.0)", "Hello\nThé café & more")

        assertEquals(
            "mailto:${Constants.SUPPORT_EMAIL}?subject=Feedback%20%28version%201.3.0%29" +
                "&body=Hello%0ATh%C3%A9%20caf%C3%A9%20%26%20more",
            mailto
        )
    }
}
