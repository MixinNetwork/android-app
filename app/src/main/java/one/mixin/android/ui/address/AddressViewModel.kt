package one.mixin.android.ui.address

import androidx.lifecycle.ViewModel
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAddressJob
import one.mixin.android.repository.AssetRepository
import javax.inject.Inject

class AddressViewModel @Inject
internal constructor(
    private val assetRepository: AssetRepository,
    private val jobManager: MixinJobManager
) : ViewModel() {

    fun addresses(id: String) = assetRepository.addresses(id)

    fun refreshAddressesByAssetId(assetId: String) {
        jobManager.addJobInBackground(RefreshAddressJob(assetId))
    }
}
