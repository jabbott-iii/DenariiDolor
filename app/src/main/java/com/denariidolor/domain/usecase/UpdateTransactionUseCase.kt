/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
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
    /** [allowOverBudget] is true once the user has confirmed an expense that goes past its category's budget. */
    suspend operator fun invoke(transaction: Transaction, allowOverBudget: Boolean = false): Result<Unit> {
        if (transaction.id <= 0L) return Result.failure(DomainException(DomainError.ID_REQUIRED, "Transaction ID is required"))
        val entity = runSuspendCatching { transaction.toEntity() }.getOrElse { return Result.failure(it) }
        validateTransactionUseCase(entity, allowOverBudget).getOrElse { return Result.failure(it) }
        val updated = runSuspendCatching { transactionRepository.update(entity) }.getOrElse { return Result.failure(it) }
        return if (updated) Result.success(Unit) else Result.failure(NoSuchElementException("Transaction not found"))
    }
}
