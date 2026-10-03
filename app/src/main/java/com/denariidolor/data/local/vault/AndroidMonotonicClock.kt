/*
 * Copyright 2026 Joseph Anthony Abbott III
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
