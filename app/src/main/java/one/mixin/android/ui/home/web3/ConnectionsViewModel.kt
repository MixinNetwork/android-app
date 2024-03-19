package one.mixin.android.ui.home.web3

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import one.mixin.android.Constants
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.vo.ConnectionUI
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ConnectionsViewModel
    @Inject
    internal constructor() : ViewModel() {
    fun disconnect(
        version: WalletConnect.Version,
        topic: String,
    ) {
        when (version) {
            WalletConnect.Version.V2 -> {
                WalletConnectV2.disconnect(topic)
            }
            WalletConnect.Version.TIP -> {}
        }
    }

    fun getLatestActiveSignSessions(): List<ConnectionUI> {
            val v2List =
                WalletConnectV2.getListOfActiveSessions().filter { wcSession ->
                    wcSession.metaData != null && !Constants.InternalWeb3Wallet.any { it.name == wcSession.metaData?.name || it.uri == wcSession.metaData?.url }
                }.mapIndexed { index, wcSession ->
                    ConnectionUI(
                        index = index,
                        icon = wcSession.metaData?.icons?.firstOrNull(),
                        name = wcSession.metaData!!.name.takeIf { it.isNotBlank() } ?: "Dapp",
                        uri = wcSession.metaData!!.url.takeIf { it.isNotBlank() } ?: "Not provided",
                        data = wcSession.topic,
                    )
                }
            return v2List
        }
    }
