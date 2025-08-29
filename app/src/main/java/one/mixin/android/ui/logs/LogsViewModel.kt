package one.mixin.android.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.util.debug.FileLogTree
import java.io.IOException

sealed interface LogUiState {
    object Loading : LogUiState
    data class Success(val content: String) : LogUiState
    data class Error(val message: String) : LogUiState
}

class LogsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<LogUiState>(LogUiState.Loading)
    val uiState: StateFlow<LogUiState> = _uiState.asStateFlow()

    fun loadPreLoginLogs() {
        viewModelScope.launch {
            _uiState.value = LogUiState.Loading
            val logContent = readLogFile()
            if (logContent != null) {
                _uiState.value = LogUiState.Success(logContent)
            } else {
                _uiState.value = LogUiState.Error("Log file not found or is empty.")
            }
        }
    }

    private suspend fun readLogFile(): String? = withContext(Dispatchers.IO) {
        try {
            val file = FileLogTree.getPreLoginLogFile()
            file?.readText(Charsets.UTF_8)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}
