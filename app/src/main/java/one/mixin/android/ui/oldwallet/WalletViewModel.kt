@file:Suppress("DEPRECATION")

package one.mixin.android.ui.oldwallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.RouteTickerRequest
import one.mixin.android.api.response.RouteTickerResponse
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAssetsJob
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.User
import one.mixin.android.vo.sumsub.ProfileResponse
import javax.inject.Inject

@HiltViewModel
class WalletViewModel
@Inject
internal constructor(
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val assetRepository: AssetRepository,
    private val jobManager: MixinJobManager,
) : ViewModel() {

    fun insertUser(user: User) = viewModelScope.launch(Dispatchers.IO) {
        userRepository.upsert(user)
    }

    suspend fun snapshotLocal(assetId: String, snapshotId: String) = assetRepository.snapshotLocal(assetId, snapshotId)

    fun assetItem(id: String): LiveData<AssetItem> = assetRepository.assetItem(id)

    suspend fun simpleAssetItem(id: String) = assetRepository.simpleAssetItem(id)

    suspend fun verifyPin(code: String) = withContext(Dispatchers.IO) {
        accountRepository.verifyPin(code)
    }

    fun addresses(id: String) = assetRepository.addresses(id)

    fun refreshAsset(assetId: String? = null) {
        jobManager.addJobInBackground(RefreshAssetsJob(assetId))
    }

    suspend fun errorCount() = accountRepository.errorCount()

    suspend fun ticker(assetId: String, offset: String?) = assetRepository.ticker(assetId, offset)

    suspend fun ticker(tickerRequest: RouteTickerRequest): MixinResponse<RouteTickerResponse> =
        assetRepository.ticker(tickerRequest)

    suspend fun refreshSnapshot(snapshotId: String): SnapshotItem? {
        return withContext(Dispatchers.IO) {
            assetRepository.refreshAndGetSnapshot(snapshotId)
        }
    }

    suspend fun profile(): MixinResponse<ProfileResponse> = assetRepository.profile()

    suspend fun fetchSessionsSuspend(ids: List<String>) = userRepository.fetchSessionsSuspend(ids)
}