package com.denariidolor.domain.usecase

import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.domain.model.Expense
import com.denariidolor.domain.model.Income
import com.denariidolor.domain.model.Transaction
import com.denariidolor.domain.model.Transfer
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val validateTransactionUseCase: ValidateTransactionUseCase,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction): Result<Long> {
        val entity = transaction.toEntity()
        val validation = validateTransactionUseCase(entity)
        if (validation.isFailure) return Result.failure(validation.exceptionOrNull()!!)
        return Result.success(transactionRepository.add(entity))
    }

    private fun Transaction.toEntity(): TransactionEntity {
        return when (this) {
            is Expense -> TransactionEntity(
                id = id,
                type = "EXPENSE",
                description = description,
                amount = amount,
                categoryId = categoryId,
                accountId = accountId,
                dateEpochMillis = dateEpochMillis
            )

            is Income -> TransactionEntity(
                id = id,
                type = "INCOME",
                description = description,
                amount = amount,
                categoryId = categoryId,
                accountId = accountId,
                dateEpochMillis = dateEpochMillis
            )

            is Transfer -> TransactionEntity(
                id = id,
                type = "TRANSFER",
                description = description,
                amount = amount,
                categoryId = categoryId,
                accountId = accountId,
                transferAccountId = transferAccountId,
                dateEpochMillis = dateEpochMillis
            )

            else -> error("Unsupported transaction type")
        }
    }
}
