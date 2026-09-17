package com.denariidolor.presentation.ui.search

import android.app.DatePickerDialog
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate

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
        setupDatePickers()

        binding.btnSearch.setOnClickListener {
            val filters = buildFilters() ?: return@setOnClickListener
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
        val startDate = binding.btnSearchStartDate.text.toString().takeUnless { it == getString(R.string.search_start_date_hint) }.orEmpty()
        val endDate = binding.btnSearchEndDate.text.toString().takeUnless { it == getString(R.string.search_end_date_hint) }.orEmpty()
        return runCatching {
            SearchFilterParser.parse(
                description = binding.etSearchDescription.text?.toString().orEmpty(),
                categoryId = binding.etSearchCategoryId.text?.toString().orEmpty(),
                minAmount = binding.etSearchMinAmount.text?.toString().orEmpty(),
                maxAmount = binding.etSearchMaxAmount.text?.toString().orEmpty(),
                startDate = startDate,
                endDate = endDate
            )
        }.getOrElse {
            Toast.makeText(requireContext(), getString(R.string.invalid_search_filters_message), Toast.LENGTH_SHORT).show()
            return null
        }
    }

    private fun setupDatePickers() {
        binding.btnSearchStartDate.setOnClickListener { showDatePicker(binding.btnSearchStartDate, R.string.search_start_date_hint) }
        binding.btnSearchEndDate.setOnClickListener { showDatePicker(binding.btnSearchEndDate, R.string.search_end_date_hint) }
        binding.btnSearchStartDate.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showDatePicker(binding.btnSearchStartDate, R.string.search_start_date_hint)
        }
        binding.btnSearchEndDate.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showDatePicker(binding.btnSearchEndDate, R.string.search_end_date_hint)
        }
    }

    private fun showDatePicker(target: android.widget.Button, placeholderTextRes: Int) {
        val placeholder = getString(placeholderTextRes)
        val selectedDate = target.text?.toString()?.takeIf { it.isNotBlank() && it != placeholder }?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        } ?: LocalDate.now()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                target.setText(LocalDate.of(year, month + 1, dayOfMonth).toString())
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        ).show()
    }
}
