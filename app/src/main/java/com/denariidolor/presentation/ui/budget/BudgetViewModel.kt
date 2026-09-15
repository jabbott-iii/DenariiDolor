package com.denariidolor.presentation.ui.budget

import androidx.lifecycle.ViewModel
import com.denariidolor.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    val repository: BudgetRepository
) : ViewModel()
