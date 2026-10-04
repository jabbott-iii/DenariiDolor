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

package com.denariidolor.domain.usecase

/**
 * A business rule a use case rejected. The UI maps [error] to a string resource, formatted with [arg] when the rule has a
 * value to show; [message] is for tests and debugging only.
 */
class DomainException(val error: DomainError, message: String, val arg: Any? = null) : IllegalArgumentException(message)

enum class DomainError {
    ID_REQUIRED,
    INVALID_AMOUNT,
    BLANK_DESCRIPTION,
    DESCRIPTION_TOO_LONG,
    INVALID_DATE,
    INVALID_ACCOUNT,
    INVALID_CATEGORY,
    TRANSFER_DESTINATION_REQUIRED,
    TRANSFER_DESTINATION_SAME,
    CATEGORY_NOT_FOUND,
    ACCOUNT_NOT_FOUND,
    TRANSFER_DESTINATION_NOT_FOUND,
    BUDGET_EXCEEDED,
    INVALID_NAME,
    DUPLICATE_CATEGORY_NAME,
    DUPLICATE_ACCOUNT_NAME,
    DEFAULT_CATEGORY,
    DEFAULT_ACCOUNT,
    CATEGORY_IN_USE,
    ACCOUNT_IN_USE,
    INVALID_BUDGET_LIMIT,
    INVALID_WARNING_PERCENT,
    INVALID_SEARCH_RANGE
}

internal fun domainFailure(error: DomainError, message: String, arg: Any? = null): Nothing = throw DomainException(error, message, arg)
