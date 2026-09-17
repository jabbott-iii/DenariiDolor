package com.denariidolor

import com.denariidolor.data.local.db.entity.TransactionEntity
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
            type = "TRANSFER",
            description = "Move to savings",
            amount = 100.0,
            categoryId = 3,
            accountId = 1,
            transferAccountId = 2,
            dateEpochMillis = 1234L
        ).toDomainTransaction() as Transfer

        assertEquals(2L, transaction.transferAccountId)
        assertEquals(0.0, transaction.balanceImpact(), 0.0001)
    }

    @Test
    fun transferEntityWithoutDestinationFailsFast() {
        val result = runCatching {
            TransactionEntity(
                id = 7,
                type = "TRANSFER",
                description = "Move to savings",
                amount = 100.0,
                categoryId = 3,
                accountId = 1,
                transferAccountId = null,
                dateEpochMillis = 1234L
            ).toDomainTransaction()
        }

        assertTrue(result.isFailure)
    }
}
