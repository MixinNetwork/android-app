package one.mixin.android.ui.tip.wc

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import one.mixin.android.repository.AssetRepository
import javax.inject.Inject

@HiltViewModel
class WalletConnectBottomSheetViewModel @Inject internal constructor(
    private val assetRepo: AssetRepository,
) : ViewModel() {

    suspend fun refreshAsset(assetId: String) = assetRepo.refreshAsset(assetId)
}
