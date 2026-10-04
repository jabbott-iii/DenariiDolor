/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.domain.usecase

import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.domain.model.SearchFilters
import com.denariidolor.util.Validators
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class SearchTransactionUseCase @Inject constructor(private val transactionRepository: TransactionRepository) {
    operator fun invoke(filters: SearchFilters): Flow<List<TransactionEntity>> {
        if (!Validators.isValidSearchRange(filters)) domainFailure(DomainError.INVALID_SEARCH_RANGE, "Invalid search ranges")
        return transactionRepository.search(filters)
    }
}
