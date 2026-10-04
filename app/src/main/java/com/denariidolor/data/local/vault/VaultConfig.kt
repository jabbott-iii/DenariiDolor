/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.vault

import com.denariidolor.util.Constants

/** Where the vault keeps its data. Instrumented tests pass their own names so they never touch the app's real data. */
data class VaultConfig(
    val databaseName: String = Constants.APP_DB_NAME,
    val profilePrefsName: String = "vault_profile",
    val keyAliasPrefix: String = "denarii_vault_",
    val legacySecurePrefsName: String = "secure_prefs",
    val legacyDatabaseKeyPrefsName: String = "db_key_prefs",
    val legacyMasterKeyAlias: String = "_androidx_security_master_key_"
)
