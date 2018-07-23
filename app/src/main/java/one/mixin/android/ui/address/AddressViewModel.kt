package one.mixin.android.ui.address

import android.arch.lifecycle.ViewModel
import one.mixin.android.repository.AssetRepository
import javax.inject.Inject

class AddressViewModel @Inject
internal constructor(private val assetRepository: AssetRepository): ViewModel() {

    fun addresses(id: String) = assetRepository.addresses(id)
}