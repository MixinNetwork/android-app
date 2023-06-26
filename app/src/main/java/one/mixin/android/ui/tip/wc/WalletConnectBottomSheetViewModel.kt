package one.mixin.android.ui.tip.wc

import androidx.lifecycle.ViewModel
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import one.mixin.android.api.service.TipService
import one.mixin.android.repository.AssetRepository
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectTIP
import one.mixin.android.tip.wc.WalletConnectV1
import one.mixin.android.tip.wc.WalletConnectV2
import javax.inject.Inject

@HiltViewModel
class WalletConnectBottomSheetViewModel @Inject internal constructor(
    private val assetRepo: AssetRepository,
    private val tipService: TipService,
) : ViewModel() {

    suspend fun refreshAsset(assetId: String) = assetRepo.refreshAsset(assetId)

    fun isTransaction(version: WalletConnect.Version, topic: String?): Boolean {
        return when (version) {
            WalletConnect.Version.V1 -> WalletConnectV1.currentSignData?.signMessage is WCEthereumTransaction
            WalletConnect.Version.V2 -> {
                val signData = WalletConnectV2.currentSignData as? WalletConnect.WCSignData.V2SignData<*> ?: return false
                return signData.sessionRequest.topic == topic
            }
            WalletConnect.Version.TIP -> WalletConnectTIP.currentSignData?.signMessage is WCEthereumTransaction
        }
    }

    fun sendTransaction(version: WalletConnect.Version, id: Long) {
        when (version) {
            WalletConnect.Version.V1 -> {
                WalletConnectV1.sendTransaction(id)
            }
            WalletConnect.Version.V2 -> {
                WalletConnectV2.sendTransaction(id)
            }
            WalletConnect.Version.TIP -> {}
        }
    }

    suspend fun getTipGas(assetId: String) = tipService.getTipGas(assetId)
}
