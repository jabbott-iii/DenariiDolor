/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.domain.model

/** Persisted by name (Room's built-in enum support), so stored values stay `EXPENSE` / `INCOME` / `TRANSFER`. */
enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER;

    companion object {
        fun parse(value: String): TransactionType = entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
            ?: throw IllegalArgumentException("Unsupported transaction type: $value")
    }
}
