package com.denariidolor.di

import com.denariidolor.domain.usecase.AddTransactionUseCase
import com.denariidolor.domain.usecase.GenerateReportUseCase
import com.denariidolor.domain.usecase.SearchTransactionUseCase
import com.denariidolor.domain.usecase.ValidateTransactionUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides fun provideAddTransactionUseCase(useCase: AddTransactionUseCase): AddTransactionUseCase = useCase
    @Provides fun provideSearchTransactionUseCase(useCase: SearchTransactionUseCase): SearchTransactionUseCase = useCase
    @Provides fun provideGenerateReportUseCase(useCase: GenerateReportUseCase): GenerateReportUseCase = useCase
    @Provides fun provideValidateTransactionUseCase(useCase: ValidateTransactionUseCase): ValidateTransactionUseCase = useCase
}
