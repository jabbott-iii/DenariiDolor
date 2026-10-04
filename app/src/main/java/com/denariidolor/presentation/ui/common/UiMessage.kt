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

package com.denariidolor.presentation.ui.common

import android.content.Context
import android.widget.Toast
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.denariidolor.R
import com.denariidolor.domain.usecase.DomainError
import com.denariidolor.domain.usecase.DomainException
import kotlinx.coroutines.flow.Flow

sealed interface UiMessage {
    data class Resource(@StringRes val resId: Int, val arg: Any? = null) : UiMessage
    data class Plural(@PluralsRes val resId: Int, val count: Int) : UiMessage

    fun resolve(context: Context): String = when (this) {
        is Resource -> if (arg == null) context.getString(resId) else context.getString(resId, arg)
        is Plural -> context.resources.getQuantityString(resId, count, count)
    }

    companion object {
        fun fromResult(result: Result<*>, @StringRes successResId: Int): UiMessage = result.fold(
            onSuccess = { Resource(successResId) },
            onFailure = ::fromError
        )

        /** Never shows an exception's own text: it is English only and can be raw SQLite output (BUG-11). */
        fun fromError(error: Throwable): UiMessage = when (error) {
            is DomainException -> fromDomainError(error)
            is NoSuchElementException -> Resource(R.string.error_item_not_found)
            else -> Resource(R.string.generic_error)
        }

        private fun fromDomainError(exception: DomainException): UiMessage {
            val count = exception.arg as? Int ?: 0
            return when (exception.error) {
                DomainError.CATEGORY_IN_USE -> Plural(R.plurals.error_category_in_use, count)
                DomainError.ACCOUNT_IN_USE -> Plural(R.plurals.error_account_in_use, count)
                else -> DOMAIN_ERROR_STRINGS[exception.error]?.let { Resource(it, exception.arg) } ?: Resource(R.string.generic_error)
            }
        }

        private val DOMAIN_ERROR_STRINGS: Map<DomainError, Int> = mapOf(
            DomainError.ID_REQUIRED to R.string.error_id_required,
            DomainError.INVALID_AMOUNT to R.string.invalid_amount_message,
            DomainError.BLANK_DESCRIPTION to R.string.error_blank_description,
            DomainError.DESCRIPTION_TOO_LONG to R.string.error_description_too_long,
            DomainError.INVALID_DATE to R.string.error_invalid_date,
            DomainError.INVALID_ACCOUNT to R.string.transaction_reference_required_message,
            DomainError.INVALID_CATEGORY to R.string.transaction_reference_required_message,
            DomainError.TRANSFER_DESTINATION_REQUIRED to R.string.error_transfer_destination_required,
            DomainError.TRANSFER_DESTINATION_SAME to R.string.error_transfer_destination_same,
            DomainError.CATEGORY_NOT_FOUND to R.string.error_category_not_found,
            DomainError.ACCOUNT_NOT_FOUND to R.string.error_account_not_found,
            DomainError.TRANSFER_DESTINATION_NOT_FOUND to R.string.error_account_not_found,
            DomainError.BUDGET_EXCEEDED to R.string.error_budget_exceeded,
            DomainError.INVALID_NAME to R.string.error_invalid_name,
            DomainError.DUPLICATE_CATEGORY_NAME to R.string.error_duplicate_category_name,
            DomainError.DUPLICATE_ACCOUNT_NAME to R.string.error_duplicate_account_name,
            DomainError.DEFAULT_CATEGORY to R.string.error_default_category,
            DomainError.DEFAULT_ACCOUNT to R.string.error_default_account,
            DomainError.INVALID_BUDGET_LIMIT to R.string.invalid_budget_input,
            DomainError.INVALID_WARNING_PERCENT to R.string.error_invalid_warning_percent,
            DomainError.INVALID_SEARCH_RANGE to R.string.invalid_search_range_message
        )
    }
}

@Composable
fun UiMessageEffect(messages: Flow<UiMessage>) {
    val context = LocalContext.current
    LaunchedEffect(messages) {
        messages.collect { message -> Toast.makeText(context, message.resolve(context), Toast.LENGTH_SHORT).show() }
    }
}
