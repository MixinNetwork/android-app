package one.mixin.android.ui.address.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.response.UserAddressView
import one.mixin.android.api.service.RouteService
import one.mixin.android.ui.address.components.FetchAddressState
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FetchAddressViewModel @Inject constructor(
    private val routeService: RouteService
) : ViewModel() {

    private val _state = MutableStateFlow(FetchAddressState.LOADING)
    val state: StateFlow<FetchAddressState> = _state.asStateFlow()

    private val _userAddress = MutableStateFlow<UserAddressView?>(null)
    val userAddress: StateFlow<UserAddressView?> = _userAddress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun fetchUserAddress(userId: String, chainId: String) {
        viewModelScope.launch {
            try {
                _state.value = FetchAddressState.LOADING
                _errorMessage.value = null

                val response = routeService.getUserAddress(userId, chainId)
                if (response.isSuccess) {
                    _userAddress.value = response.data
                    _state.value = FetchAddressState.SUCCESS
                } else if (response.errorCode == 404){
                    _errorMessage.value = MixinApplication.appContext.getString(R.string.Address_not_found)
                    _state.value = FetchAddressState.ERROR
                } else {
                    Timber.e("Fetch user address failed: ${response.errorDescription}")
                    _errorMessage.value = response.errorDescription
                    _state.value = FetchAddressState.ERROR
                }
            } catch (e: Exception) {
                Timber.e(e, "Fetch user address exception")
                _errorMessage.value = e.message
                _state.value = FetchAddressState.ERROR
            }
        }
    }

    fun retry(userId: String, chainId: String) {
        fetchUserAddress(userId, chainId)
    }
}
