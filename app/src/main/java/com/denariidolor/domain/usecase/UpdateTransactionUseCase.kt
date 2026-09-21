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
