package com.denariidolor.domain.usecase

import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.util.runSuspendCatching
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        if (id <= 0L) return Result.failure(IllegalArgumentException("Transaction ID is required"))
        val deleted = runSuspendCatching { transactionRepository.delete(id) }.getOrElse { return Result.failure(it) }
        return if (deleted) Result.success(Unit) else Result.failure(NoSuchElementException("Transaction not found"))
    }
}
