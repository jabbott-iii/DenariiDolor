/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor

import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.domain.model.TransactionType
import com.denariidolor.domain.model.Transfer
import com.denariidolor.domain.model.isMalformedTransfer
import com.denariidolor.domain.model.toDomainTransaction
import com.denariidolor.domain.model.toDomainTransactionOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun readPathMappingSkipsMalformedTransfer() {
        val transfer = TransactionEntity(
            id = 7,
            type = TransactionType.TRANSFER,
            description = "Move to savings",
            amountCents = 10_000L,
            categoryId = 3,
            accountId = 1,
            transferAccountId = null,
            dateEpochMillis = 1234L
        )

        assertTrue(transfer.isMalformedTransfer)
        assertNull(transfer.toDomainTransactionOrNull())
        assertTrue(transfer.copy(transferAccountId = 2).toDomainTransactionOrNull() is Transfer)
    }
}
