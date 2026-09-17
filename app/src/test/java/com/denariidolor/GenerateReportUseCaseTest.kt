package com.denariidolor

import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.domain.model.SearchFilters
import com.denariidolor.domain.usecase.GenerateReportUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateReportUseCaseTest {
    @Test
    fun reportCalculatesIncomeExpenseAndCsv() = runBlocking {
        val repo = object : TransactionRepository {
            override suspend fun add(transaction: TransactionEntity): Long = 0
            override fun getAll(): Flow<List<TransactionEntity>> = emptyFlow()
            override fun search(filters: SearchFilters): Flow<List<TransactionEntity>> = emptyFlow()
            override suspend fun getByDateRange(startInclusive: Long, endInclusive: Long): List<TransactionEntity> = listOf(
                TransactionEntity(type = "INCOME", description = "Salary", amount = 2000.0, categoryId = 1, accountId = 1, dateEpochMillis = startInclusive),
                TransactionEntity(type = "EXPENSE", description = "Rent", amount = 900.0, categoryId = 2, accountId = 1, dateEpochMillis = startInclusive),
                TransactionEntity(type = "TRANSFER", description = "Savings", amount = 300.0, categoryId = 3, accountId = 1, transferAccountId = 2, dateEpochMillis = startInclusive)
            )

            override suspend fun getExpenseTotalForCategory(categoryId: Long, startInclusive: Long, endInclusive: Long): Double = 0.0
        }

        val report = GenerateReportUseCase(repo).invoke(2026, 9)

        assertEquals(2000.0, report.totalIncome, 0.0001)
        assertEquals(900.0, report.totalExpense, 0.0001)
        assertEquals(1100.0, report.net, 0.0001)
        assertTrue(report.csv.contains("Salary"))
        assertTrue(report.csv.contains("Rent"))
        assertTrue(report.csv.contains("Savings"))
    }
}
