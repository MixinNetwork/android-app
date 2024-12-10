package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.vo.PriceAndChange
import one.mixin.android.vo.TopAsset
import one.mixin.android.vo.toPriceAndChange

class RefreshTopAssetsJob : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(GROUP).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshTopAssetsJob"
    }

    override fun onRun() =
        runBlocking {
            val response = tokenService.topAssets().execute().body()
            if (response != null && response.isSuccess && response.data != null) {
                val assetList = response.data as List<TopAsset>
                assetList.map { it.assetId }.chunked(200) {
                    launch { topAssetDao().deleteNotInIds(it) }
                }
                topAssetDao().insertListSuspend(assetList)

                val recentArray =
                    MixinApplication.appContext.defaultSharedPreferences
                        .getString(Constants.Account.PREF_RECENT_SEARCH_ASSETS, null)?.split("=")
                if (recentArray.isNullOrEmpty()) return@runBlocking
                val recentList = tokenDao().suspendFindAssetsByIds(recentArray.take(2))
                if (recentList.isNullOrEmpty()) return@runBlocking
                val needUpdatePrice = arrayListOf<PriceAndChange>()
                assetList.forEach { t ->
                    val needUpdate =
                        recentList.find { r ->
                            r.assetId == t.assetId && r.priceUsd != t.priceUsd
                        }
                    if (needUpdate != null) {
                        needUpdatePrice.add(t.toPriceAndChange())
                    }
                }
                if (needUpdatePrice.isNotEmpty()) {
                    tokenDao().suspendUpdatePrices(needUpdatePrice)
                }
            }
        }
}
