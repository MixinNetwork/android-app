package one.mixin.android.job

import android.database.DatabaseUtils
import androidx.sqlite.db.SimpleSQLiteQuery
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.db.converter.DepositEntryListConverter
import one.mixin.android.db.runInTransaction
import one.mixin.android.util.debug.measureTimeMillis
import one.mixin.android.vo.Asset
import one.mixin.android.vo.Fiats
import timber.log.Timber

class RefreshAssetsJob(
    private val assetId: String? = null,
) : MixinJob(
    Params(PRIORITY_UI_HIGH)
        .singleInstanceBy(assetId ?: "all-assets").persist().requireNetwork(),
    assetId ?: "all-assets",
) {

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshAssetsJob"
    }

    override fun onRun() = runBlocking {
        if (assetId != null) {
            val response = assetService.getAssetByIdSuspend(assetId)
            if (response.isSuccess && response.data != null) {
                response.data?.let {
                    assetDao.upsertAsset(it)
                }
            }
        } else {

            val response = assetService.fetchAllAssetSuspend()
            if (response.isSuccess && response.data != null) {
                val list = response.data as List<Asset>
                response.data?.map {
                    it.assetId
                }?.let { ids ->
                    assetDao.findAllAssetIdSuspend().subtract(ids.toSet()).chunked(100).forEach {
                        assetDao.zeroClearSuspend(it)
                    }
                }
                val version = android.database.sqlite.SQLiteDatabase.create(null).use {
                    DatabaseUtils.stringForQuery(it, "SELECT sqlite_version()", null)
                }
                measureTimeMillis("asset raw upsert") {
                    list.map { a ->
                        SimpleSQLiteQuery(
                            """
                        INSERT INTO `assets` (`asset_id`,`symbol`,`name`,`icon_url`,`balance`,`destination`,`tag`,`price_btc`,`price_usd`,`chain_id`,`change_usd`,`change_btc`,`confirmations`,`asset_key`,`reserve`,`deposit_entries`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                        ON CONFLICT(asset_id) DO UPDATE 
                        SET symbol = EXCLUDED.symbol, 
                        name = EXCLUDED.name, 
                        icon_url = EXCLUDED.icon_url,
                        balance = EXCLUDED.balance,
                        destination = EXCLUDED.destination,
                        tag = EXCLUDED.tag,
                        price_btc = EXCLUDED.price_btc,
                        price_usd = EXCLUDED.price_usd,
                        chain_id = EXCLUDED.chain_id,
                        change_usd = EXCLUDED.change_usd,
                        change_btc = EXCLUDED.change_btc,
                        confirmations = EXCLUDED.confirmations,
                        asset_key = EXCLUDED.asset_key,
                        reserve = EXCLUDED.reserve,
                        deposit_entries = EXCLUDED.deposit_entries
                    """,
                            arrayOf(
                                a.assetId,
                                a.symbol,
                                a.name,
                                a.iconUrl,
                                a.balance,
                                a.destination,
                                a.tag,
                                a.priceBtc,
                                a.priceUsd,
                                a.chainId,
                                a.changeUsd,
                                a.changeBtc,
                                a.confirmations,
                                a.assetKey,
                                a.reserve,
                                DepositEntryListConverter().converterDate(a.depositEntries)
                            )
                        )
                    }.let { sqls ->
                        runInTransaction {
                            sqls.forEach { a ->
                                assetDao.rawQuery(a)
                            }
                        }
                    }
                }


                measureTimeMillis("asset upsert") {
                    assetDao.upsertAssets(list)
                }
                measureTimeMillis("asset insert or replace") {
                    assetDao.insertList(list)
                }
                Timber.e("version $version")
            }
            refreshFiats()
        }
    }

    private fun refreshFiats() = runBlocking {
        val resp = accountService.getFiats()
        if (resp.isSuccess) {
            resp.data?.let { fiatList ->
                Fiats.updateFiats(fiatList)
            }
        }
    }

    override fun cancel() {
    }
}
