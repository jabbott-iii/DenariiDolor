/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.domain.model

data class Budget(val id: Long = 0, val categoryId: Long, val monthlyLimitCents: Long, val warningThresholdPercent: Int = 80)
