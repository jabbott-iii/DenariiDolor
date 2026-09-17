package com.denariidolor.presentation.ui.transaction

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.denariidolor.R
import com.denariidolor.databinding.FragmentAddTransactionBinding
import com.denariidolor.presentation.ui.common.BaseFragment
import com.denariidolor.util.Constants
import com.denariidolor.util.DateUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate

@AndroidEntryPoint
class AddTransactionFragment : BaseFragment(R.layout.fragment_add_transaction) {
    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<TransactionViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddTransactionBinding.bind(view)
        binding.spinnerTransactionType.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            resources.getStringArray(R.array.transaction_types)
        )
        binding.spinnerTransactionType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateTransferAccountVisibility()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        setupDatePicker()
        updateTransferAccountVisibility()

        binding.btnSaveTransaction.setOnClickListener {
            val description = binding.etDescription.text?.toString().orEmpty()
            val amount = binding.etAmount.text?.toString()?.toDoubleOrNull() ?: 0.0
            val selectedType = binding.spinnerTransactionType.selectedItem?.toString().orEmpty()
            val categoryId = binding.etCategoryId.text?.toString()?.toLongOrNull() ?: defaultCategoryId(selectedType)
            val accountId = binding.etAccountId.text?.toString()?.toLongOrNull() ?: Constants.DEFAULT_CASH_ACCOUNT_ID
            val transferAccountId = binding.etTransferAccountId.text?.toString()?.toLongOrNull()
                ?: if (selectedType == "TRANSFER") Constants.DEFAULT_SAVINGS_ACCOUNT_ID else null
            val dateText = binding.btnTransactionDate.text.toString().takeUnless { it == getString(R.string.date_hint) }.orEmpty()
            val dateEpochMillis = if (dateText.isBlank()) {
                System.currentTimeMillis()
            } else {
                runCatching { DateUtils.parseIsoDateToStartOfDayEpochMillis(dateText) }.getOrElse {
                    Toast.makeText(requireContext(), getString(R.string.invalid_date_message), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            viewModel.addTransaction(
                type = selectedType,
                description = description,
                amount = amount,
                categoryId = categoryId,
                accountId = accountId,
                transferAccountId = transferAccountId,
                dateEpochMillis = dateEpochMillis
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.status.collect {
                    if (!it.isNullOrBlank()) {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        if (it == "Saved") {
                            binding.etDescription.text?.clear()
                            binding.etAmount.text?.clear()
                            binding.etCategoryId.text?.clear()
                            binding.etAccountId.text?.clear()
                            binding.etTransferAccountId.text?.clear()
                            binding.btnTransactionDate.text = getString(R.string.date_hint)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun defaultCategoryId(type: String): Long {
        return when (type) {
            "INCOME" -> Constants.DEFAULT_INCOME_CATEGORY_ID
            "TRANSFER" -> Constants.DEFAULT_TRANSFER_CATEGORY_ID
            else -> Constants.DEFAULT_EXPENSE_CATEGORY_ID
        }
    }

    private fun setupDatePicker() {
        binding.btnTransactionDate.setOnClickListener { showDatePicker { binding.btnTransactionDate.text = it } }
        binding.btnTransactionDate.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showDatePicker { binding.btnTransactionDate.text = it }
            }
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val placeholder = getString(R.string.date_hint)
        val selectedDate = binding.btnTransactionDate.text?.toString()?.takeIf { it.isNotBlank() && it != placeholder }?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        } ?: LocalDate.now()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                onDateSelected(LocalDate.of(year, month + 1, dayOfMonth).toString())
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        ).show()
    }

    private fun updateTransferAccountVisibility() {
        val isTransfer = binding.spinnerTransactionType.selectedItem?.toString() == "TRANSFER"
        binding.etTransferAccountId.visibility = if (isTransfer) View.VISIBLE else View.GONE
        binding.etTransferAccountId.isEnabled = isTransfer
    }
}
