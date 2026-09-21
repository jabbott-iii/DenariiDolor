package com.denariidolor.domain.usecase

import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.domain.model.Transaction
import com.denariidolor.domain.model.toEntity
import com.denariidolor.util.runSuspendCatching
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val validateTransactionUseCase: ValidateTransactionUseCase,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction): Result<Long> {
        val entity = runSuspendCatching { transaction.toEntity().copy(id = 0L) }.getOrElse { return Result.failure(it) }
        validateTransactionUseCase(entity).getOrElse { return Result.failure(it) }
        return runSuspendCatching { transactionRepository.add(entity) }
    }
}
