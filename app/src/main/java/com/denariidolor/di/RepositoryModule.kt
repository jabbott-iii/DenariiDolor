package com.denariidolor.di

import com.denariidolor.data.repository.AccountRepository
import com.denariidolor.data.repository.AccountRepositoryImpl
import com.denariidolor.data.repository.BudgetRepository
import com.denariidolor.data.repository.BudgetRepositoryImpl
import com.denariidolor.data.repository.CategoryRepository
import com.denariidolor.data.repository.CategoryRepositoryImpl
import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.data.repository.TransactionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository
    @Binds @Singleton abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository
    @Binds @Singleton abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository
    @Binds @Singleton abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository
}
