/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.di

import com.denariidolor.data.local.preferences.CryptoPrimitives
import com.denariidolor.data.local.preferences.JcaCryptoPrimitives
import com.denariidolor.data.local.vault.SettingsVault
import com.denariidolor.data.local.vault.SignInVault
import com.denariidolor.data.local.vault.Vault
import com.denariidolor.presentation.ui.auth.BiometricCapability
import com.denariidolor.presentation.ui.auth.DeviceBiometricCapability
import com.denariidolor.util.DeviceClock
import com.denariidolor.util.ZonedClock
import com.denariidolor.util.asZonedClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = DeviceClock

    @Provides
    @Singleton
    fun provideZonedClock(clock: Clock): ZonedClock = clock.asZonedClock()

    @Provides
    @Singleton
    fun provideCryptoPrimitives(): CryptoPrimitives = JcaCryptoPrimitives()

    @Provides
    fun provideSignInVault(vault: Vault): SignInVault = vault

    @Provides
    fun provideSettingsVault(vault: Vault): SettingsVault = vault

    @Provides
    fun provideBiometricCapability(capability: DeviceBiometricCapability): BiometricCapability = capability
}
