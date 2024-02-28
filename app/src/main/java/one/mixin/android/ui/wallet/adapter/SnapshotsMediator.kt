package one.mixin.android.ui.wallet.adapter

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import one.mixin.android.api.service.TokenService
import one.mixin.android.db.SafeSnapshotDao
import one.mixin.android.db.TokenDao
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshTokensJob
import one.mixin.android.ui.wallet.BaseTransactionsFragment.Companion.LIMIT
import one.mixin.android.vo.SnapshotItem

@OptIn(ExperimentalPagingApi::class)
class SnapshotsMediator(
    private val assetService: TokenService,
    private val safeSnapshotDao: SafeSnapshotDao,
    private val tokenDao: TokenDao,
    private val jobManager: MixinJobManager,
    private val assetId: String,
) : RemoteMediator<Int, SnapshotItem>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, SnapshotItem>,
    ): MediatorResult {
        return try {
            val offset =
                when (loadType) {
                    LoadType.REFRESH -> null
                    LoadType.APPEND -> getRemoteKeyForLastItem(state)
                    LoadType.PREPEND -> getRemoteKeyForFirstItem(state)
                }
            val resp = assetService.getSnapshots(assetId = assetId, offset = offset)
            if (!resp.isSuccess) {
                return MediatorResult.Error(IllegalStateException(resp.error?.toString()))
            }
            val list = resp.data
            val nextKey =
                if (list.isNullOrEmpty()) {
                    null
                } else {
                    safeSnapshotDao.insertListSuspend(list)
                    list.forEach { item ->
                        if (tokenDao.simpleAsset(item.assetId) == null) {
                            jobManager.addJobInBackground(RefreshTokensJob(item.assetId))
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
