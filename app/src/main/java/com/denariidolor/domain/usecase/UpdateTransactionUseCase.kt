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

import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.domain.model.Transaction
import com.denariidolor.domain.model.toEntity
import com.denariidolor.util.runSuspendCatching
import javax.inject.Inject

class UpdateTransactionUseCase @Inject constructor(
    private val validateTransactionUseCase: ValidateTransactionUseCase,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction): Result<Unit> {
        if (transaction.id <= 0L) return Result.failure(IllegalArgumentException("Transaction ID is required"))
        val entity = runSuspendCatching { transaction.toEntity() }.getOrElse { return Result.failure(it) }
        validateTransactionUseCase(entity).getOrElse { return Result.failure(it) }
        val updated = runSuspendCatching { transactionRepository.update(entity) }.getOrElse { return Result.failure(it) }
        return if (updated) Result.success(Unit) else Result.failure(NoSuchElementException("Transaction not found"))
    }
}
