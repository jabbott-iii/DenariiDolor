/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.di

import dev.zacsweers.metro.Qualifier

/** The Application as a [android.content.Context]. It lives as long as the process, so singletons may keep it; never an Activity. */
@Qualifier
annotation class ApplicationContext
