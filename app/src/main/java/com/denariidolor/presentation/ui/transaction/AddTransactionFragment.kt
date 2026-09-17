package com.denariidolor.presentation.ui.transaction

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.denariidolor.R
import com.denariidolor.databinding.FragmentAddTransactionBinding
import com.denariidolor.presentation.ui.common.BaseFragment
import com.denariidolor.util.DateUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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

        binding.btnSaveTransaction.setOnClickListener {
            val description = binding.etDescription.text?.toString().orEmpty()
            val amount = binding.etAmount.text?.toString()?.toDoubleOrNull() ?: 0.0
            val selectedType = binding.spinnerTransactionType.selectedItem?.toString().orEmpty()
            val categoryId = binding.etCategoryId.text?.toString()?.toLongOrNull() ?: defaultCategoryId(selectedType)
            val accountId = binding.etAccountId.text?.toString()?.toLongOrNull() ?: 1L
            val transferAccountId = binding.etTransferAccountId.text?.toString()?.toLongOrNull()
                ?: if (selectedType == "TRANSFER") 2L else null
            val dateText = binding.etTransactionDate.text?.toString().orEmpty()
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
            "INCOME" -> 2L
            "TRANSFER" -> 3L
            else -> 1L
        }
    }
}
