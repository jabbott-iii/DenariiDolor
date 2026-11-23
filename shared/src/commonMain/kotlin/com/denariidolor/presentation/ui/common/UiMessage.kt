/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui.common

import com.denariidolor.domain.usecase.DomainError
import com.denariidolor.domain.usecase.DomainException

/**
 * Text for the user from a ViewModel: a key, never words, so it reads in the device's language when shown. On Android,
 * UiMessageEffect.kt turns it into the string resource of the same name.
 */
sealed interface UiMessage {
    /** A string, formatted with [arg] when it has a placeholder. */
    data class Resource(val key: StringKey, val arg: Any? = null) : UiMessage

    /** A plural string chosen by [count], which also fills its placeholder. */
    data class Plural(val key: PluralKey, val count: Int) : UiMessage

    companion object {
        fun fromResult(result: Result<*>, success: StringKey): UiMessage = result.fold(
            onSuccess = { Resource(success) },
            onFailure = ::fromError
        )

        /** Never shows an exception's own text: it is English only and can be raw SQLite output (BUG-11). */
        fun fromError(error: Throwable): UiMessage = when (error) {
            is DomainException -> fromDomainError(error)
            is NoSuchElementException -> Resource(StringKey.ERROR_ITEM_NOT_FOUND)
            else -> Resource(StringKey.GENERIC_ERROR)
        }

        private fun fromDomainError(exception: DomainException): UiMessage {
            val count = exception.arg as? Int ?: 0
            return when (exception.error) {
                DomainError.CATEGORY_IN_USE -> Plural(PluralKey.ERROR_CATEGORY_IN_USE, count)
                DomainError.ACCOUNT_IN_USE -> Plural(PluralKey.ERROR_ACCOUNT_IN_USE, count)
                else -> DOMAIN_ERROR_STRINGS[exception.error]?.let { Resource(it, exception.arg) } ?: Resource(StringKey.GENERIC_ERROR)
            }
        }

        private val DOMAIN_ERROR_STRINGS: Map<DomainError, StringKey> = mapOf(
            DomainError.ID_REQUIRED to StringKey.ERROR_ID_REQUIRED,
            DomainError.INVALID_AMOUNT to StringKey.INVALID_AMOUNT_MESSAGE,
            DomainError.BLANK_DESCRIPTION to StringKey.ERROR_BLANK_DESCRIPTION,
            DomainError.DESCRIPTION_TOO_LONG to StringKey.ERROR_DESCRIPTION_TOO_LONG,
            DomainError.INVALID_DATE to StringKey.ERROR_INVALID_DATE,
            DomainError.INVALID_ACCOUNT to StringKey.TRANSACTION_REFERENCE_REQUIRED_MESSAGE,
            DomainError.INVALID_CATEGORY to StringKey.TRANSACTION_REFERENCE_REQUIRED_MESSAGE,
            DomainError.TRANSFER_DESTINATION_REQUIRED to StringKey.ERROR_TRANSFER_DESTINATION_REQUIRED,
            DomainError.TRANSFER_DESTINATION_SAME to StringKey.ERROR_TRANSFER_DESTINATION_SAME,
            DomainError.CATEGORY_NOT_FOUND to StringKey.ERROR_CATEGORY_NOT_FOUND,
            DomainError.ACCOUNT_NOT_FOUND to StringKey.ERROR_ACCOUNT_NOT_FOUND,
            DomainError.TRANSFER_DESTINATION_NOT_FOUND to StringKey.ERROR_ACCOUNT_NOT_FOUND,
            DomainError.BUDGET_EXCEEDED to StringKey.ERROR_BUDGET_EXCEEDED,
            DomainError.INVALID_NAME to StringKey.ERROR_INVALID_NAME,
            DomainError.DUPLICATE_CATEGORY_NAME to StringKey.ERROR_DUPLICATE_CATEGORY_NAME,
            DomainError.DUPLICATE_ACCOUNT_NAME to StringKey.ERROR_DUPLICATE_ACCOUNT_NAME,
            DomainError.DEFAULT_CATEGORY to StringKey.ERROR_DEFAULT_CATEGORY,
            DomainError.DEFAULT_ACCOUNT to StringKey.ERROR_DEFAULT_ACCOUNT,
            DomainError.INVALID_BUDGET_LIMIT to StringKey.INVALID_BUDGET_INPUT,
            DomainError.INVALID_WARNING_PERCENT to StringKey.ERROR_INVALID_WARNING_PERCENT,
            DomainError.INVALID_SEARCH_RANGE to StringKey.INVALID_SEARCH_RANGE_MESSAGE
        )
    }
}
