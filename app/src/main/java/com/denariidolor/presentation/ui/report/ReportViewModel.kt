/*
 * Copyright 2026 Joseph Anthony Abbott III
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.denariidolor.presentation.ui.report

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denariidolor.R
import com.denariidolor.data.export.ReportExporter
import com.denariidolor.data.export.ReportFormat
import com.denariidolor.domain.model.MonthlyReport
import com.denariidolor.domain.usecase.GenerateReportUseCase
import com.denariidolor.presentation.ui.common.UiMessage
import com.denariidolor.util.runSuspendCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportUiState(
    val period: YearMonth,
    val report: MonthlyReport? = null,
    val loading: Boolean = false,
    val failed: Boolean = false
)

sealed interface ReportEvent {
    data class Message(val message: UiMessage) : ReportEvent
    data class Share(val uri: Uri, val mimeType: String) : ReportEvent
}

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val generateReportUseCase: GenerateReportUseCase,
    private val reportExporter: ReportExporter,
    clock: Clock
) : ViewModel() {
    private val _state = MutableStateFlow(ReportUiState(period = YearMonth.now(clock)))
    val state: StateFlow<ReportUiState> = _state.asStateFlow()

    private val _events = Channel<ReportEvent>(Channel.BUFFERED)
    val events: Flow<ReportEvent> = _events.receiveAsFlow()

    private var loadJob: Job? = null

    fun refresh() {
        val period = _state.value.period
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, failed = false) }
            val result = runSuspendCatching { generateReportUseCase(period) }
            _state.update {
                it.copy(
                    report = result.getOrNull() ?: it.report.takeIf { report ->
                        report?.period == period
                    },
                    loading = false,
                    failed = result.isFailure
                )
            }
        }
    }

    fun previousMonth() = changePeriod(_state.value.period.minusMonths(1))

    fun nextMonth() = changePeriod(_state.value.period.plusMonths(1))

    fun save(uri: Uri, format: ReportFormat) {
        val report = _state.value.report ?: return
        viewModelScope.launch {
            val result = runSuspendCatching { reportExporter.writeTo(uri, report, format) }
            _events.send(ReportEvent.Message(exportMessage(result, R.string.report_saved)))
        }
    }

    fun share(format: ReportFormat) {
        val report = _state.value.report ?: return
        viewModelScope.launch {
            runSuspendCatching { reportExporter.createShareUri(report, format) }
                .onSuccess { _events.send(ReportEvent.Share(it, format.mimeType)) }
                .onFailure { _events.send(ReportEvent.Message(UiMessage.Resource(R.string.report_export_failed))) }
        }
    }

    private fun changePeriod(period: YearMonth) {
        _state.update { it.copy(period = period, report = null) }
        refresh()
    }

    private fun exportMessage(result: Result<*>, successResId: Int): UiMessage =
        if (result.isSuccess) UiMessage.Resource(successResId) else UiMessage.Resource(R.string.report_export_failed)
}
