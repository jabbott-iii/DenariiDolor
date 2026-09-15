package com.denariidolor.presentation.ui.category

import androidx.lifecycle.ViewModel
import com.denariidolor.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    val repository: CategoryRepository
) : ViewModel()
