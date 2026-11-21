/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import java.net.URLEncoder

/** Links for rating, sharing and feedback. Plain strings so they stay testable on the JVM; callers turn them into URIs. */
object StoreLinks {
    /**
     * A fixed campaign tag on shared links. Google Play only counts installs per campaign; nothing identifies the person who
     * shared the link or who installed from it.
     */
    private const val SHARE_REFERRER = "utm_source%3Dapp_share%26utm_medium%3Dshare_sheet%26utm_campaign%3Dshare_app"

    fun marketUri(packageName: String): String = "market://details?id=$packageName"

    fun webUrl(packageName: String): String = "https://play.google.com/store/apps/details?id=$packageName"

    fun shareUrl(packageName: String): String = "${webUrl(packageName)}&referrer=$SHARE_REFERRER"

    fun mailto(address: String, subject: String, body: String): String = "mailto:$address?subject=${encode(subject)}&body=${encode(body)}"

    // URLEncoder writes spaces as '+', which mail apps show literally; mailto needs %20.
    private fun encode(text: String): String = URLEncoder.encode(text, "UTF-8").replace("+", "%20")
}
