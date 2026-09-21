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

package com.denariidolor

import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.domain.model.TransactionType
import com.denariidolor.domain.model.Transfer
import com.denariidolor.domain.model.toDomainTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionEntityMappingsTest {
    @Test
    fun transferEntityMapsToTransferDomainModel() {
        val transaction = TransactionEntity(
            id = 7,
            type = TransactionType.TRANSFER,
            description = "Move to savings",
            amountCents = 10_000L,
            categoryId = 3,
            accountId = 1,
            transferAccountId = 2,
            dateEpochMillis = 1234L
        ).toDomainTransaction() as Transfer

        assertEquals(2L, transaction.transferAccountId)
        assertEquals(0L, transaction.balanceImpact())
    }

    @Test
    fun transferEntityWithoutDestinationFailsFast() {
        val result = runCatching {
            TransactionEntity(
                id = 7,
                type = TransactionType.TRANSFER,
                description = "Move to savings",
                amountCents = 10_000L,
                categoryId = 3,
                accountId = 1,
                transferAccountId = null,
                dateEpochMillis = 1234L
            ).toDomainTransaction()
        }

        assertTrue(result.isFailure)
    }
}
