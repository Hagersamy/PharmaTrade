package com.pharmatrade.feature.seller.presentation.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.friendlyError
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.drugs.domain.InventoryRefreshBus
import com.pharmatrade.feature.drugs.domain.model.UploadHistory
import com.pharmatrade.feature.drugs.domain.usecase.GetInventoryUseCase
import com.pharmatrade.feature.drugs.domain.usecase.UploadInventoryUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val SUCCESS_STATUSES = setOf("success", "completed", "done")
private val FAILED_STATUSES = setOf("failed", "error")
private val FINAL_STATUSES = SUCCESS_STATUSES + FAILED_STATUSES
private const val POLL_INTERVAL_MS = 4_000L
private const val MAX_POLL_ATTEMPTS = 30

enum class ProcessingResult { NONE, SUCCESS, FAILED }

data class InventoryUploadUiState(
    val selectedFileUri: String = "",
    val selectedFileName: String = "",
    val isUploading: Boolean = false,
    val uploadQueued: Boolean = false,
    val lastUpload: UploadHistory? = null,
    val isRefreshingStatus: Boolean = false,
    val processingResult: ProcessingResult = ProcessingResult.NONE,
    val processedRows: Int = 0,
    val failedRows: Int = 0,
    val uploadError: String? = null
)

// This screen is upload-only: the resulting inventory list itself is shown on the seller Home
// screen. Here we only need enough state to drive the upload flow and reflect its status.
class InventoryUploadViewModel(
    private val uploadInventoryUseCase: UploadInventoryUseCase,
    private val getInventoryUseCase: GetInventoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUploadUiState())
    val uiState: StateFlow<InventoryUploadUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init {
        refreshStatus()
    }

    fun onFileSelected(uri: String, name: String) {
        _uiState.value = _uiState.value.copy(
            selectedFileUri = uri,
            selectedFileName = name,
            uploadError = null,
            uploadQueued = false,
            processingResult = ProcessingResult.NONE
        )
    }

    fun upload() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(
                isUploading = true,
                uploadError = null,
                uploadQueued = false,
                processingResult = ProcessingResult.NONE
            )
            when (val result =
                uploadInventoryUseCase(state.selectedFileUri, state.selectedFileName)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        uploadQueued = true,
                        selectedFileUri = "",
                        selectedFileName = ""
                    )
                    startPolling()
                }

                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    uploadError = LanguageManager.strings.friendlyError(result.message)
                )

                else -> _uiState.value = _uiState.value.copy(isUploading = false)
            }
        }
    }

    fun refreshStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshingStatus = true)
            when (val result = getInventoryUseCase()) {
                is Result.Success -> {
                    val lastUpload = result.data.lastUpload
                    val status = lastUpload?.status?.lowercase() ?: ""
                    val stillProcessing = lastUpload != null && status !in FINAL_STATUSES
                    _uiState.value = _uiState.value.copy(
                        isRefreshingStatus = false,
                        lastUpload = lastUpload,
                        uploadQueued = stillProcessing
                    )
                    // Resume polling if we land on this screen mid-processing (e.g. the app
                    // was relaunched while a previous upload was still being processed).
                    if (stillProcessing && pollJob?.isActive != true) {
                        startPolling()
                    }
                }

                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isRefreshingStatus = false,
                    uploadError = LanguageManager.strings.friendlyError(result.message)
                )

                else -> _uiState.value = _uiState.value.copy(isRefreshingStatus = false)
            }
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            var attempts = 0
            var resolved = false
            while (attempts < MAX_POLL_ATTEMPTS) {
                delay(POLL_INTERVAL_MS)
                attempts++
                when (val result = getInventoryUseCase()) {
                    is Result.Success -> {
                        val lastUpload = result.data.lastUpload
                        _uiState.value = _uiState.value.copy(lastUpload = lastUpload)
                        val status = lastUpload?.status?.lowercase() ?: ""
                        if (status in FINAL_STATUSES) {
                            val finalResult = if (status in SUCCESS_STATUSES)
                                ProcessingResult.SUCCESS else ProcessingResult.FAILED
                            _uiState.value = _uiState.value.copy(
                                processingResult = finalResult,
                                processedRows = lastUpload?.processedRows ?: 0,
                                failedRows = lastUpload?.failedRows ?: 0,
                                uploadQueued = false
                            )
                            // Even a "failed" batch can have partially committed rows
                            // (processedRows > 0), so tell Home to refresh either way.
                            InventoryRefreshBus.notifyChanged()
                            resolved = true
                            break
                        }
                    }

                    else -> break
                }
            }
            if (!resolved) {
                // Either the server never finished processing within the poll window, or a
                // poll request failed outright. Either way, don't leave the "please wait"
                // status stuck on screen forever — surface it and let the user retry manually.
                _uiState.value = _uiState.value.copy(
                    uploadQueued = false,
                    uploadError = LanguageManager.strings.errorStillProcessingUpload
                )
            }
        }
    }

    fun dismissResult() {
        _uiState.value = _uiState.value.copy(
            processingResult = ProcessingResult.NONE,
            uploadQueued = false
        )
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(uploadError = null)
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}
