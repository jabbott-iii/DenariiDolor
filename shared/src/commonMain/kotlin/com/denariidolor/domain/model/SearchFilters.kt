/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.domain.model

data class SearchFilters(
    val description: String? = null,
    val categoryId: Long? = null,
    val minAmountCents: Long? = null,
    val maxAmountCents: Long? = null,
    val startDateEpochMillis: Long? = null,
    val endDateEpochMillis: Long? = null
)
