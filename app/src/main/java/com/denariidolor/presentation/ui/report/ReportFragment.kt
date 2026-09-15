package com.denariidolor.presentation.ui.report

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.denariidolor.R
import com.denariidolor.databinding.FragmentReportBinding
import com.denariidolor.presentation.ui.common.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate

@AndroidEntryPoint
class ReportFragment : BaseFragment(R.layout.fragment_report) {
    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<ReportViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentReportBinding.bind(view)
        val now = LocalDate.now()
        viewModel.load(now.year, now.monthValue)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.report.collect { report ->
                    if (report != null) {
                        binding.tvReportSummary.text = getString(
                            R.string.report_summary,
                            report.monthLabel,
                            report.totalIncome,
                            report.totalExpense,
                            report.net
                        )
                        binding.tvReportCsv.text = report.csv
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
