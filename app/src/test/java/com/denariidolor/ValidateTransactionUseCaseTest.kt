package com.denariidolor

import com.denariidolor.data.local.db.entity.BudgetEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.data.repository.BudgetRepository
import com.denariidolor.data.repository.CategoryRepository
import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.domain.model.SearchFilters
import com.denariidolor.domain.usecase.ValidateTransactionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateTransactionUseCaseTest {
    @Test
    fun expenseFailsWhenBudgetExceeded() = runBlocking {
        val budgetRepo = object : BudgetRepository {
            override suspend fun upsert(budget: BudgetEntity): Long = 0
            override fun getAll(): Flow<List<BudgetEntity>> = emptyFlow()
            override suspend fun getByCategoryId(categoryId: Long): BudgetEntity =
                BudgetEntity(categoryId = categoryId, monthlyLimit = 100.0)
        }
        val txnRepo = object : TransactionRepository {
            override suspend fun add(transaction: TransactionEntity): Long = 0
            override fun getAll(): Flow<List<TransactionEntity>> = emptyFlow()
            override fun search(filters: SearchFilters): Flow<List<TransactionEntity>> = emptyFlow()
            override suspend fun getByDateRange(startInclusive: Long, endInclusive: Long): List<TransactionEntity> = emptyList()
            override suspend fun getExpenseTotalForCategory(categoryId: Long, startInclusive: Long, endInclusive: Long): Double = 90.0
        }
        val categoryRepo = object : CategoryRepository {
            override suspend fun add(category: CategoryEntity): Long = 0
            override fun getAll(): Flow<List<CategoryEntity>> = emptyFlow()
            override suspend fun isDuplicateName(name: String): Boolean = false
        }

        val useCase = ValidateTransactionUseCase(categoryRepo, budgetRepo, txnRepo)
        val result = useCase(
            TransactionEntity(
                type = "EXPENSE",
                description = "Coffee",
                amount = 20.0,
                categoryId = 1,
                accountId = 1,
                dateEpochMillis = System.currentTimeMillis()
            )
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun transferFailsWhenDestinationMatchesSource() = runBlocking {
        val budgetRepo = object : BudgetRepository {
            override suspend fun upsert(budget: BudgetEntity): Long = 0
            override fun getAll(): Flow<List<BudgetEntity>> = emptyFlow()
            override suspend fun getByCategoryId(categoryId: Long): BudgetEntity? = null
        }
        val txnRepo = object : TransactionRepository {
            override suspend fun add(transaction: TransactionEntity): Long = 0
            override fun getAll(): Flow<List<TransactionEntity>> = emptyFlow()
            override fun search(filters: SearchFilters): Flow<List<TransactionEntity>> = emptyFlow()
            override suspend fun getByDateRange(startInclusive: Long, endInclusive: Long): List<TransactionEntity> = emptyList()
            override suspend fun getExpenseTotalForCategory(categoryId: Long, startInclusive: Long, endInclusive: Long): Double = 0.0
        }
        val categoryRepo = object : CategoryRepository {
            override suspend fun add(category: CategoryEntity): Long = 0
            override fun getAll(): Flow<List<CategoryEntity>> = emptyFlow()
            override suspend fun isDuplicateName(name: String): Boolean = false
        }

        val useCase = ValidateTransactionUseCase(categoryRepo, budgetRepo, txnRepo)
        val result = useCase(
            TransactionEntity(
                type = "TRANSFER",
                description = "Move money",
                amount = 20.0,
                categoryId = 3,
                accountId = 1,
                transferAccountId = 1,
                dateEpochMillis = System.currentTimeMillis()
            )
        )

        assertTrue(result.isFailure)
    }
}
