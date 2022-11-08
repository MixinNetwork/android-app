package one.mixin.android.ui.setting.ui.page

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.api.service.AuthorizationService
import one.mixin.android.util.ErrorHandler
import javax.inject.Inject

@SuppressLint("CheckResult")
@HiltViewModel
class AuthenticationsViewModel @Inject constructor(
    authorizationService: AuthorizationService,
) : ViewModel() {

    private val authenticationsState = MutableStateFlow<Result<List<AuthorizationResponse>>?>(null)

    val authentications
        get() = authenticationsState.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            initialValue = authenticationsState.value
        )

    init {
        authorizationService
            .authorizations().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    viewModelScope.launch {
                        authenticationsState.emit(it.toResult())
                    }
                },
                {
                    viewModelScope.launch {
                        authenticationsState.emit(Result.failure(it))
                    }
                    ErrorHandler.handleError(it)
                },
            )
    }

    fun onDeAuthorize(appId: String) {
        viewModelScope.launch {
            authenticationsState.emit(
                authentications.value?.map { list ->
                    list.filter { it.app.appId != appId }
                }
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
