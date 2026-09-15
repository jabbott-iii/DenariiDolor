package com.denariidolor.domain.usecase

import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.domain.model.SearchFilters
import com.denariidolor.util.Validators
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(filters: SearchFilters): Flow<List<TransactionEntity>> {
        require(Validators.isValidSearchRange(filters)) { "Invalid search ranges" }
        return transactionRepository.search(filters)
    }
}
