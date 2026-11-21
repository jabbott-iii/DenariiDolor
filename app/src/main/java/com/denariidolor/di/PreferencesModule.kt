/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.di

import com.denariidolor.data.local.preferences.GuidancePreferences
import com.denariidolor.data.local.preferences.OnboardingStore
import com.denariidolor.data.local.preferences.RatePromptStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Both interfaces resolve to the one `@Singleton` [GuidancePreferences], so every screen sees the same state. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {
    @Binds
    abstract fun bindOnboardingStore(impl: GuidancePreferences): OnboardingStore

    @Binds
    abstract fun bindRatePromptStore(impl: GuidancePreferences): RatePromptStore
}
