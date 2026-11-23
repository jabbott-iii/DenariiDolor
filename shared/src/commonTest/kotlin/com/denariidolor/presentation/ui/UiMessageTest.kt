/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui

import com.denariidolor.domain.usecase.DomainError
import com.denariidolor.domain.usecase.DomainException
import com.denariidolor.presentation.ui.common.PluralKey
import com.denariidolor.presentation.ui.common.StringKey
import com.denariidolor.presentation.ui.common.UiMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class UiMessageTest {
    private val generic = UiMessage.Resource(StringKey.GENERIC_ERROR)

    @Test
    fun everyDomainErrorHasItsOwnMessage() {
        DomainError.entries.forEach { error ->
            assertNotEquals(generic, UiMessage.fromError(DomainException(error, "raw text", 2)), error.name)
        }
    }

    @Test
    fun domainArgumentsFillTheMessage() {
        assertEquals(
            UiMessage.Resource(StringKey.ERROR_DUPLICATE_CATEGORY_NAME, "Dining"),
            UiMessage.fromError(DomainException(DomainError.DUPLICATE_CATEGORY_NAME, "raw text", "Dining"))
        )
        assertEquals(
            UiMessage.Plural(PluralKey.ERROR_ACCOUNT_IN_USE, 3),
            UiMessage.fromError(DomainException(DomainError.ACCOUNT_IN_USE, "raw text", 3))
        )
    }

    @Test
    fun exceptionTextIsNeverShown() {
        assertEquals(generic, UiMessage.fromError(IllegalStateException("SQLiteException: no such table: transactions")))
        assertEquals(
            UiMessage.Resource(StringKey.ERROR_ITEM_NOT_FOUND),
            UiMessage.fromError(NoSuchElementException("Transaction not found"))
        )
    }

    @Test
    fun resultMapsSuccessAndFailure() {
        assertEquals(UiMessage.Resource(StringKey.BUDGET_SAVED), UiMessage.fromResult(Result.success(Unit), StringKey.BUDGET_SAVED))
        assertEquals(generic, UiMessage.fromResult(Result.failure<Unit>(RuntimeException("boom")), StringKey.BUDGET_SAVED))
    }
}
