package one.mixin.android.ui.setting.ui.page

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.api.service.AuthorizationService
import one.mixin.android.util.ErrorHandler
import javax.inject.Inject

@HiltViewModel
class AuthenticationsViewModel
    @Inject
    constructor(
        authorizationService: AuthorizationService,
    ) : ViewModel() {
        private val authenticationsState = MutableStateFlow<Result<List<AuthorizationResponse>>?>(null)

        val authentications
            get() =
                authenticationsState.stateIn(
                    viewModelScope,
                    SharingStarted.Eagerly,
                    initialValue = authenticationsState.value,
                )

        init {
            viewModelScope.launch {
                val result =
                    try {
                        authorizationService.authorizations().toResult()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        ErrorHandler.handleError(e)
                        Result.failure(e)
                    }
                authenticationsState.emit(result)
            }
        }

        fun onDeAuthorize(appId: String) {
            viewModelScope.launch {
                authenticationsState.emit(
                    authentications.value?.map { list ->
                        list.filter { it.app.appId != appId }
                    },
                )
            }
        }
    }

private fun <T : Any> MixinResponse<T>.toResult(): Result<T> {
    return if (isSuccess) {
        Result.success(data!!)
    } else {
        Result.failure(Exception())
    }
}
