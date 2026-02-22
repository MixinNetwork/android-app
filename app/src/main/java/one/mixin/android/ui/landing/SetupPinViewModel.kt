package one.mixin.android.ui.landing

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.PREF_LOGIN_OR_SIGN_UP
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.ui.landing.vo.SetupState
import one.mixin.android.ui.tip.TipBundle
import one.mixin.android.ui.tip.TipFlowInteractor
import one.mixin.android.ui.tip.TipType
import one.mixin.android.ui.tip.TryConnecting
import javax.inject.Inject

@HiltViewModel
class SetupPinViewModel @Inject internal constructor(
    private val tipFlowInteractor: TipFlowInteractor,
) : ViewModel() {
    private val _setupState: MutableLiveData<SetupState> = MutableLiveData<SetupState>(SetupState.Loading)
    val setupState: LiveData<SetupState> get() = _setupState
    private val _errorMessage: MutableLiveData<String> = MutableLiveData("")
    val errorMessage: LiveData<String> get() = _errorMessage

    fun executeCreatePin(context: Context, pin: String) {
        _setupState.value = SetupState.Loading
        context.defaultSharedPreferences.putBoolean(PREF_LOGIN_OR_SIGN_UP, true)
        _errorMessage.value = ""
        viewModelScope.launch {
            val deviceId: String = requireNotNull(
                context.defaultSharedPreferences.getString(Constants.DEVICE_ID, null),
            ) { "required deviceId can not be null" }
            val tipBundle = TipBundle(
                tipType = TipType.Create,
                deviceId = deviceId,
                tipStep = TryConnecting,
                pin = pin,
            )
            val success: Boolean = tipFlowInteractor.process(
                context = context,
                tipBundle = tipBundle,
                shouldOpenMainActivity = true,
                onStepChanged = { _ -> },
                onShowMessage = { message: String ->
                    _errorMessage.postValue(message)
                },
            )
            _setupState.postValue(if (success) SetupState.Success else SetupState.Failure)
        }
    }
}
