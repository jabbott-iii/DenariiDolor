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

package com.denariidolor.testutil

import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.local.db.entity.BudgetEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.data.repository.AccountRepository
import com.denariidolor.data.repository.BudgetRepository
import com.denariidolor.data.repository.CategoryRepository
import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.domain.model.SearchFilters
import com.denariidolor.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeTransactionRepository(initial: List<TransactionEntity> = emptyList()) : TransactionRepository {
    val items = initial.toMutableList()
    var expenseTotalCents = 0L
    var lastExcludedTransactionId: Long? = null
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override suspend fun add(transaction: TransactionEntity): Long {
        val id = nextId++
        items += transaction.copy(id = id)
        return id
    }

    override suspend fun update(transaction: TransactionEntity): Boolean {
        val index = items.indexOfFirst { it.id == transaction.id }
        if (index < 0) return false
        items[index] = transaction
        return true
    }

    override suspend fun delete(id: Long): Boolean = items.removeAll { it.id == id }
    override suspend fun getById(id: Long): TransactionEntity? = items.firstOrNull { it.id == id }
    override fun getAll(): Flow<List<TransactionEntity>> = flowOf(items.toList())
    override fun search(filters: SearchFilters): Flow<List<TransactionEntity>> = flowOf(items.toList())
    override suspend fun getByDateRange(startInclusive: Long, endInclusive: Long): List<TransactionEntity> =
        items.filter { it.dateEpochMillis in startInclusive..endInclusive }

    override suspend fun getExpenseTotalForCategory(
        categoryId: Long,
        startInclusive: Long,
        endInclusive: Long,
        excludeTransactionId: Long
    ): Long {
        lastExcludedTransactionId = excludeTransactionId
        return expenseTotalCents
    }

    override suspend fun countByCategory(categoryId: Long): Int = items.count { it.categoryId == categoryId }
    override suspend fun countByAccount(accountId: Long): Int =
        items.count { it.accountId == accountId || it.transferAccountId == accountId }
}

class FakeCategoryRepository(initial: List<CategoryEntity> = emptyList()) : CategoryRepository {
    val items = initial.toMutableList()
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override suspend fun add(category: CategoryEntity): Long {
        val id = nextId++
        items += category.copy(id = id)
        return id
    }

    override suspend fun update(category: CategoryEntity): Boolean {
        val index = items.indexOfFirst { it.id == category.id }
        if (index < 0) return false
        items[index] = category
        return true
    }

    override suspend fun delete(id: Long): Boolean = items.removeAll { it.id == id }
    override suspend fun getById(id: Long): CategoryEntity? = items.firstOrNull { it.id == id }
    override fun getAll(): Flow<List<CategoryEntity>> = flowOf(items.toList())
    override suspend fun isDuplicateName(name: String, excludeId: Long): Boolean =
        items.any { it.id != excludeId && it.name.equals(name, ignoreCase = true) }
}

class FakeAccountRepository(initial: List<AccountEntity> = emptyList()) : AccountRepository {
    val items = initial.toMutableList()
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override suspend fun add(account: AccountEntity): Long {
        val id = nextId++
        items += account.copy(id = id)
        return id
    }

    override suspend fun rename(id: Long, name: String): Boolean {
        val index = items.indexOfFirst { it.id == id }
        if (index < 0) return false
        items[index] = items[index].copy(name = name)
        return true
    }

    override suspend fun delete(id: Long): Boolean = items.removeAll { it.id == id }
    override suspend fun getById(id: Long): AccountEntity? = items.firstOrNull { it.id == id }
    override fun getAll(): Flow<List<AccountEntity>> = flowOf(items.toList())
    override suspend fun isDuplicateName(name: String, excludeId: Long): Boolean =
        items.any { it.id != excludeId && it.name.equals(name, ignoreCase = true) }
}

class FakeBudgetRepository(initial: List<BudgetEntity> = emptyList()) : BudgetRepository {
    val items = initial.toMutableList()
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override suspend fun upsert(budget: BudgetEntity): Long {
        items.removeAll { it.categoryId == budget.categoryId }
        val id = if (budget.id > 0L) budget.id else nextId++
        items += budget.copy(id = id)
        return id
    }

    override fun getAll(): Flow<List<BudgetEntity>> = flowOf(items.toList())
    override suspend fun getByCategoryId(categoryId: Long): BudgetEntity? = items.firstOrNull { it.categoryId == categoryId }
    override suspend fun deleteByCategoryId(categoryId: Long): Boolean = items.removeAll { it.categoryId == categoryId }
}

object TestData {
    val categories = listOf(
        CategoryEntity(id = 1, name = "General Expense"),
        CategoryEntity(id = 2, name = "General Income"),
        CategoryEntity(id = 3, name = "Transfer"),
        CategoryEntity(id = 4, name = "Groceries")
    )
    val accounts = listOf(
        AccountEntity(id = 1, name = "Cash", balanceCents = 0L),
        AccountEntity(id = 2, name = "Savings", balanceCents = 0L),
        AccountEntity(id = 3, name = "Checking", balanceCents = 0L)
    )

    fun expense(id: Long = 0, amountCents: Long = 2_000, categoryId: Long = 4, accountId: Long = 1) = TransactionEntity(
        id = id,
        type = TransactionType.EXPENSE,
        description = "Coffee",
        amountCents = amountCents,
        categoryId = categoryId,
        accountId = accountId,
        dateEpochMillis = System.currentTimeMillis()
    )
}
