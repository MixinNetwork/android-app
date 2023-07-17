package one.mixin.android.ui.wallet.adapter

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import one.mixin.android.api.service.AssetService
import one.mixin.android.db.AssetDao
import one.mixin.android.db.SnapshotDao
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAssetsJob
import one.mixin.android.ui.wallet.BaseTransactionsFragment.Companion.LIMIT
import one.mixin.android.vo.SnapshotItem

@OptIn(ExperimentalPagingApi::class)
class SnapshotsMediator(
    private val assetService: AssetService,
    private val snapshotDao: SnapshotDao,
    private val assetDao: AssetDao,
    private val jobManager: MixinJobManager,
    private val assetId: String,
) : RemoteMediator<Int, SnapshotItem>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, SnapshotItem>,
    ): MediatorResult {
        return try {
            val offset = when (loadType) {
                LoadType.REFRESH -> null
                LoadType.APPEND -> getRemoteKeyForLastItem(state)
                LoadType.PREPEND -> getRemoteKeyForFirstItem(state)
            }
            val resp = assetService.getSnapshotsByAssetId(assetId, offset = offset)
            if (!resp.isSuccess) {
                return MediatorResult.Error(IllegalStateException(resp.error?.toString()))
            }
            val list = resp.data
            val nextKey = if (list.isNullOrEmpty()) {
                null
            } else {
                snapshotDao.insertListSuspend(list)
                list.forEach { item ->
                    if (assetDao.simpleAsset(item.assetId) == null) {
                        jobManager.addJobInBackground(RefreshAssetsJob(item.assetId))
                    }
                }
                if (list.size < LIMIT) {
                    null
                } else {
                    list.last().createdAt
                }
            }
            return MediatorResult.Success(endOfPaginationReached = nextKey == null)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }

    private fun getRemoteKeyForFirstItem(state: PagingState<Int, SnapshotItem>): String? {
        return state.pages.lastOrNull {
            it.data.isNotEmpty()
        }?.data?.firstOrNull()?.createdAt
    }

    private fun getRemoteKeyForLastItem(state: PagingState<Int, SnapshotItem>): String? {
        return state.pages.lastOrNull {
            it.data.isNotEmpty()
        }?.data?.lastOrNull()?.createdAt
    }
}
