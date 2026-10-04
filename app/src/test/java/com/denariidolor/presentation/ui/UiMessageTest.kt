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

package com.denariidolor.presentation.ui

import com.denariidolor.R
import com.denariidolor.domain.usecase.DomainError
import com.denariidolor.domain.usecase.DomainException
import com.denariidolor.presentation.ui.common.UiMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class UiMessageTest {
    private val generic = UiMessage.Resource(R.string.generic_error)

    @Test
    fun everyDomainErrorHasItsOwnMessage() {
        DomainError.entries.forEach { error ->
            assertNotEquals(error.name, generic, UiMessage.fromError(DomainException(error, "raw text", 2)))
        }
    }

    @Test
    fun domainArgumentsFillTheMessage() {
        assertEquals(
            UiMessage.Resource(R.string.error_duplicate_category_name, "Dining"),
            UiMessage.fromError(DomainException(DomainError.DUPLICATE_CATEGORY_NAME, "raw text", "Dining"))
        )
        assertEquals(
            UiMessage.Plural(R.plurals.error_account_in_use, 3),
            UiMessage.fromError(DomainException(DomainError.ACCOUNT_IN_USE, "raw text", 3))
        )
    }

    @Test
    fun exceptionTextIsNeverShown() {
        assertEquals(generic, UiMessage.fromError(IllegalStateException("SQLiteException: no such table: transactions")))
        assertEquals(
            UiMessage.Resource(R.string.error_item_not_found),
            UiMessage.fromError(NoSuchElementException("Transaction not found"))
        )
    }

    @Test
    fun resultMapsSuccessAndFailure() {
        assertEquals(UiMessage.Resource(R.string.budget_saved), UiMessage.fromResult(Result.success(Unit), R.string.budget_saved))
        assertEquals(generic, UiMessage.fromResult(Result.failure<Unit>(RuntimeException("boom")), R.string.budget_saved))
    }
}
