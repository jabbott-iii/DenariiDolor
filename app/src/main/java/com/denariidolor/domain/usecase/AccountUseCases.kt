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

import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.repository.AccountRepository
import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.util.Constants
import com.denariidolor.util.Validators
import com.denariidolor.util.runSuspendCatching
import javax.inject.Inject

class AccountUseCases @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend fun add(name: String, openingBalanceCents: Long = 0L): Result<Long> = runSuspendCatching {
        val validName = requireValidName(name, excludeId = 0L)
        accountRepository.add(AccountEntity(name = validName, balanceCents = openingBalanceCents))
    }

    suspend fun rename(id: Long, name: String): Result<Unit> = runSuspendCatching {
        if (id <= 0L) domainFailure(DomainError.ID_REQUIRED, "Account ID is required")
        val validName = requireValidName(name, excludeId = id)
        if (!accountRepository.rename(id, validName)) throw NoSuchElementException("Account not found")
    }

    suspend fun delete(id: Long): Result<Unit> = runSuspendCatching {
        if (id in PROTECTED_IDS) domainFailure(DomainError.DEFAULT_ACCOUNT, "Default accounts cannot be deleted")
        val usage = transactionRepository.countByAccount(id)
        if (usage != 0) domainFailure(DomainError.ACCOUNT_IN_USE, "Account is used by $usage transaction(s)", usage)
        if (!accountRepository.delete(id)) throw NoSuchElementException("Account not found")
    }

    private suspend fun requireValidName(name: String, excludeId: Long): String {
        val trimmed = name.trim()
        if (!Validators.isValidName(trimmed)) {
            domainFailure(DomainError.INVALID_NAME, "Name must be 1-${Validators.MAX_NAME_LENGTH} characters", Validators.MAX_NAME_LENGTH)
        }
        if (accountRepository.isDuplicateName(trimmed, excludeId)) {
            domainFailure(DomainError.DUPLICATE_ACCOUNT_NAME, "An account named \"$trimmed\" already exists", trimmed)
        }
        return trimmed
    }

    private companion object {
        val PROTECTED_IDS = Constants.DEFAULT_ACCOUNTS.map { it.id }.toSet()
    }
}
