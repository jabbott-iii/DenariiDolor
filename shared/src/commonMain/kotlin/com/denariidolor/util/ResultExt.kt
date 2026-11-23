/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import kotlin.coroutines.cancellation.CancellationException

/** Like [runCatching] but rethrows [CancellationException] so coroutine cancellation keeps working. */
@Suppress("TooGenericExceptionCaught") // Intentional: converts any failure into a Result for the caller.
inline fun <T> runSuspendCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
