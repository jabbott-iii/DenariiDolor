package com.denariidolor.presentation.ui.dashboard

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.denariidolor.R
import com.denariidolor.databinding.FragmentDashboardBinding
import com.denariidolor.presentation.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : BaseFragment(R.layout.fragment_dashboard) {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<DashboardViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDashboardBinding.bind(view)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.summary.collect {
                    binding.tvIncome.text = getString(R.string.dashboard_income, it.income)
                    binding.tvExpense.text = getString(R.string.dashboard_expense, it.expense)
                    binding.tvNet.text = getString(R.string.dashboard_net, it.net)
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
