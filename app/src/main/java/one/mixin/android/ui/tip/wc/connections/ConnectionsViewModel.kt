package one.mixin.android.ui.tip.wc.connections

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
import one.mixin.android.tip.wc.Chain
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectV1
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.tip.wc.getChain
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ConnectionsViewModel @Inject internal constructor() : ViewModel() {

    private var _refreshFlow: MutableSharedFlow<Unit> = MutableSharedFlow(replay = 0, extraBufferCapacity = 1, BufferOverflow.DROP_OLDEST)
    private var refreshFlow: SharedFlow<Unit> = _refreshFlow.asSharedFlow()
    private val signConnectionsFlow = refreshFlow.map {
        getLatestActiveSignSessions()
    }

    var currentConnectionId: Int? = null
        set(value) {
            field = value
            refreshCurrentConnectionUI()
        }

    private fun getConnectionUI(): ConnectionUI? = connections.value.firstOrNull { it.index == currentConnectionId }

    val connections: StateFlow<List<ConnectionUI>> =
        signConnectionsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, getLatestActiveSignSessions())

    val currentConnectionUI: MutableState<ConnectionUI?> = mutableStateOf(getConnectionUI())

    fun disconnect(version: WalletConnect.Version, topic: String) {
        when (version) {
            WalletConnect.Version.V1 -> {
                WalletConnectV1.removeFromStore(topic)
            }
            WalletConnect.Version.V2 -> {
                WalletConnectV2.disconnect(topic)
            }
            WalletConnect.Version.TIP -> {}
        }
    }

    fun refreshConnections() {
        val res = _refreshFlow.tryEmit(Unit)
        Timber.d("Web3Wallet refreshConnections $res")
    }

    fun changeNetworkV1(chain: Chain) {
        val connectionUI = getConnectionUI() ?: return
        if (connectionUI.chain == chain) return

        WalletConnectV1.changeNetwork(chain)
        refreshConnections()
    }

    private fun refreshCurrentConnectionUI() {
        currentConnectionUI.value = getConnectionUI()
    }

    private fun getLatestActiveSignSessions(): List<ConnectionUI> {
        val v2List = WalletConnectV2.getListOfActiveSessions().filter { wcSession ->
            wcSession.metaData != null
        }.mapIndexed { index, wcSession ->
            ConnectionUI(
                index = index,
                icon = wcSession.metaData?.icons?.firstOrNull(),
                name = wcSession.metaData!!.name.takeIf { it.isNotBlank() } ?: "Dapp",
                uri = wcSession.metaData!!.url.takeIf { it.isNotBlank() } ?: "Not provided",
                data = wcSession.topic,
            )
        }

        val v1List = WalletConnectV1.getStoredSessions()?.sortedByDescending { wcV1Session ->
            wcV1Session.date
        }?.mapIndexed { index, item ->
            val peer = item.remotePeerMeta
            val connectionUI = ConnectionUI(
                index = index + v2List.size,
                icon = peer.icons.firstOrNull(),
                name = peer.name.takeIf { it.isNotBlank() } ?: "Dapp",
                uri = peer.url.takeIf { it.isNotBlank() } ?: "Not provided",
                data = item.session.topic,
                chain = item.chainId.getChain(),
            )
            connectionUI
        }
        return v2List.plus(v1List ?: emptyList())
    }
}
