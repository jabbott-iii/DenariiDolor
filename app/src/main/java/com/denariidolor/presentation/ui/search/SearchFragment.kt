package com.denariidolor.presentation.ui.search

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.denariidolor.R
import com.denariidolor.databinding.FragmentSearchBinding
import com.denariidolor.domain.model.SearchFilters
import com.denariidolor.presentation.ui.common.BaseFragment
import com.denariidolor.util.DateUtils
import com.denariidolor.util.Validators
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchFragment : BaseFragment(R.layout.fragment_search) {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<SearchViewModel>()
    private val adapter = SimpleTextAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchBinding.bind(view)
        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSearchResults.adapter = adapter

        binding.btnSearch.setOnClickListener {
            val filters = buildFilters() ?: return@setOnClickListener
            if (!Validators.isValidSearchRange(filters)) {
                Toast.makeText(requireContext(), getString(R.string.invalid_search_range_message), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.search(filters)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.results.collect { adapter.submit(it) }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun buildFilters(): SearchFilters? {
        val startDate = parseStartDate(binding.etSearchStartDate.text?.toString().orEmpty()) ?: return null
        val endDate = parseEndDate(binding.etSearchEndDate.text?.toString().orEmpty()) ?: return null
        return SearchFilters(
            description = binding.etSearchDescription.text?.toString()?.trim()?.takeIf { it.isNotEmpty() },
            categoryId = binding.etSearchCategoryId.text?.toString()?.toLongOrNull(),
            minAmount = binding.etSearchMinAmount.text?.toString()?.toDoubleOrNull(),
            maxAmount = binding.etSearchMaxAmount.text?.toString()?.toDoubleOrNull(),
            startDateEpochMillis = startDate,
            endDateEpochMillis = endDate
        )
    }

    private fun parseStartDate(value: String): Long? {
        if (value.isBlank()) return null
        return runCatching {
            DateUtils.parseIsoDateToStartOfDayEpochMillis(value)
        }.getOrElse {
            Toast.makeText(requireContext(), getString(R.string.invalid_date_message), Toast.LENGTH_SHORT).show()
            return null
        }
    }

    private fun parseEndDate(value: String): Long? {
        if (value.isBlank()) return null
        return runCatching {
            DateUtils.parseIsoDateToEndOfDayEpochMillis(value)
        }.getOrElse {
            Toast.makeText(requireContext(), getString(R.string.invalid_date_message), Toast.LENGTH_SHORT).show()
            return null
        }
    }
}
