/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.vault

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.denariidolor.data.local.preferences.MonotonicClock

/** Elapsed time since boot (unaffected by date changes) plus the boot count, for the sign-in lockout (CS-13). */
class AndroidMonotonicClock(context: Context) : MonotonicClock {
    private val contentResolver = context.applicationContext.contentResolver

    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()

    override fun bootCount(): Int? = try {
        Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT)
    } catch (_: Settings.SettingNotFoundException) {
        null
    }
}
