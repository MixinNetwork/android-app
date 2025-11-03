package one.mixin.android.db.provider

import android.annotation.SuppressLint
import android.database.Cursor
import androidx.paging.DataSource
import androidx.room.RoomSQLiteQuery
import one.mixin.android.db.WalletDatabase
import one.mixin.android.db.converter.AssetChangeListConverter
import one.mixin.android.db.datasource.MixinLimitOffsetDataSource
import one.mixin.android.db.web3.vo.AssetChange
import one.mixin.android.db.web3.vo.TransactionStatus
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.tip.wc.SortOrder
import one.mixin.android.ui.wallet.Web3FilterParams

@SuppressLint("RestrictedApi")
class Web3DataProvider {
    companion object {
        fun allTransactions(
            database: WalletDatabase,
            filter: Web3FilterParams,
        ): DataSource.Factory<Int, Web3TransactionItem> {
            return object : DataSource.Factory<Int, Web3TransactionItem>() {
                override fun create(): DataSource<Int, Web3TransactionItem> {
                    val baseSelect = """
                        SELECT DISTINCT 
                            w.transaction_hash, 
                            w.transaction_type, 
                            w.status, 
                            w.block_number, 
                            w.chain_id, 
                            w.address, 
                            w.fee, 
                            w.senders, 
                            w.receivers, 
                            w.approvals, 
                            w.send_asset_id, 
                            w.receive_asset_id, 
                            w.transaction_at, 
                            w.updated_at, 
                            w.level,
                            c.symbol as chain_symbol,
                            c.icon_url as chain_icon_url,
                            s.icon_url as send_asset_icon_url,
                            s.symbol as send_asset_symbol,
                            r.icon_url as receive_asset_icon_url,
                            r.symbol as receive_asset_symbol
                        FROM transactions w
                        LEFT JOIN tokens c ON c.asset_id = w.chain_id
                        LEFT JOIN tokens s ON s.asset_id = w.send_asset_id
                        LEFT JOIN tokens r ON r.asset_id = w.receive_asset_id
                    """.trimIndent()

                    val filters = buildFilters(filter)
                    val whereSql = if (filters.isEmpty()) "" else "WHERE ${filters.joinToString(" AND ")}"
                    val orderSql = when (filter.order) {
                        SortOrder.Recent -> "ORDER BY w.transaction_at DESC"
                        SortOrder.Oldest -> "ORDER BY w.transaction_at ASC"
                        else -> "ORDER BY w.transaction_at DESC"
                    }

                    val countSql = "SELECT count(1) FROM transactions w $whereSql"
                    val countStmt = RoomSQLiteQuery.acquire(countSql, 0)

                    val offsetSql = """
                        SELECT w.rowid FROM transactions w
                        $whereSql
                        $orderSql
                        LIMIT ? OFFSET ?
                    """.trimIndent()
                    val offsetStmt = RoomSQLiteQuery.acquire(offsetSql, 2)

                    val sqlGenerator = fun(ids: String): RoomSQLiteQuery {
                        val querySql = StringBuilder()
                            .append(baseSelect)
                            .append('\n')
                            .append("WHERE w.rowid IN ($ids)")
                            .append('\n')
                            .append(orderSql)
                            .toString()
                        return RoomSQLiteQuery.acquire(querySql, 0)
                    }

                    return object : MixinLimitOffsetDataSource<Web3TransactionItem>(
                        database,
                        countStmt,
                        offsetStmt,
                        sqlGenerator,
                        arrayOf("transactions", "tokens", "addresses"),
                    ) {
                        private val assetChangeConverter = AssetChangeListConverter()

                        override fun convertRows(cursor: Cursor?): List<Web3TransactionItem> {
                            if (cursor == null) return emptyList()
                            val list = ArrayList<Web3TransactionItem>(cursor.count)
                            val idxHash = cursor.getColumnIndexOrThrow("transaction_hash")
                            val idxType = cursor.getColumnIndexOrThrow("transaction_type")
                            val idxStatus = cursor.getColumnIndexOrThrow("status")
                            val idxBlock = cursor.getColumnIndexOrThrow("block_number")
                            val idxChain = cursor.getColumnIndexOrThrow("chain_id")
                            val idxAddress = cursor.getColumnIndexOrThrow("address")
                            val idxFee = cursor.getColumnIndexOrThrow("fee")
                            val idxSenders = cursor.getColumnIndexOrThrow("senders")
                            val idxReceivers = cursor.getColumnIndexOrThrow("receivers")
                            val idxApprovals = cursor.getColumnIndexOrThrow("approvals")
                            val idxSendAssetId = cursor.getColumnIndexOrThrow("send_asset_id")
                            val idxReceiveAssetId = cursor.getColumnIndexOrThrow("receive_asset_id")
                            val idxAt = cursor.getColumnIndexOrThrow("transaction_at")
                            val idxUpdated = cursor.getColumnIndexOrThrow("updated_at")
                            val idxLevel = cursor.getColumnIndexOrThrow("level")
                            val idxChainSymbol = cursor.getColumnIndexOrThrow("chain_symbol")
                            val idxChainIcon = cursor.getColumnIndexOrThrow("chain_icon_url")
                            val idxSendIcon = cursor.getColumnIndexOrThrow("send_asset_icon_url")
                            val idxSendSymbol = cursor.getColumnIndexOrThrow("send_asset_symbol")
                            val idxRecvIcon = cursor.getColumnIndexOrThrow("receive_asset_icon_url")
                            val idxRecvSymbol = cursor.getColumnIndexOrThrow("receive_asset_symbol")

                            while (cursor.moveToNext()) {
                                val sendersJson = cursor.getString(idxSenders)
                                val receiversJson = cursor.getString(idxReceivers)
                                val approvalsJson = cursor.getString(idxApprovals)
                                val senders: List<AssetChange> = assetChangeConverter.toAssetChangeList(sendersJson) ?: emptyList()
                                val receivers: List<AssetChange> = assetChangeConverter.toAssetChangeList(receiversJson) ?: emptyList()
                                val approvals: List<AssetChange>? = approvalsJson?.let { assetChangeConverter.toAssetChangeList(it) }

                                val item = Web3TransactionItem(
                                    transactionHash = cursor.getString(idxHash),
                                    transactionType = cursor.getString(idxType),
                                    status = cursor.getString(idxStatus),
                                    blockNumber = cursor.getLong(idxBlock),
                                    chainId = cursor.getString(idxChain),
                                    address = cursor.getString(idxAddress),
                                    fee = cursor.getString(idxFee),
                                    senders = senders,
                                    receivers = receivers,
                                    approvals = approvals,
                                    sendAssetId = cursor.getString(idxSendAssetId),
                                    receiveAssetId = cursor.getString(idxReceiveAssetId),
                                    transactionAt = cursor.getString(idxAt),
                                    updatedAt = cursor.getString(idxUpdated),
                                    chainSymbol = cursor.getString(idxChainSymbol),
                                    chainIconUrl = cursor.getString(idxChainIcon),
                                    sendAssetIconUrl = cursor.getString(idxSendIcon),
                                    sendAssetSymbol = cursor.getString(idxSendSymbol),
                                    receiveAssetIconUrl = cursor.getString(idxRecvIcon),
                                    receiveAssetSymbol = cursor.getString(idxRecvSymbol),
                                    level = cursor.getInt(idxLevel),
                                )
                                list.add(item)
                            }
                            return list
                        }
                    }
                }
            }
        }

        private fun buildFilters(filter: Web3FilterParams): MutableList<String> {
            val filters = mutableListOf<String>()
            filter.tokenItems?.let { tokens ->
                if (tokens.isNotEmpty()) {
                    val tokenIds = tokens.joinToString(", ") { t -> "'${t.assetId}'" }
                    filters.add("(w.send_asset_id IN ($tokenIds) OR w.receive_asset_id IN ($tokenIds))")
                }
            }
            filters.add("w.address IN (SELECT destination FROM addresses WHERE wallet_id = '${filter.walletId}')")
            when (filter.tokenFilterType) {
                one.mixin.android.ui.wallet.Web3TokenFilterType.SEND -> filters.add("w.transaction_type = 'transfer_out'")
                one.mixin.android.ui.wallet.Web3TokenFilterType.RECEIVE -> filters.add("w.transaction_type = 'transfer_in'")
                one.mixin.android.ui.wallet.Web3TokenFilterType.APPROVAL -> filters.add("w.transaction_type = 'approval'")
                one.mixin.android.ui.wallet.Web3TokenFilterType.SWAP -> filters.add("w.transaction_type = 'swap'")
                one.mixin.android.ui.wallet.Web3TokenFilterType.PENDING -> filters.add("w.status = '${TransactionStatus.PENDING.value}'")
                one.mixin.android.ui.wallet.Web3TokenFilterType.ALL -> {}
            }
            filter.startTime?.let {
                filters.add("w.transaction_at >= '${org.threeten.bp.Instant.ofEpochMilli(it)}'")
            }
            filter.endTime?.let {
                filters.add("w.transaction_at <= '${org.threeten.bp.Instant.ofEpochMilli(it + 24 * 60 * 60 * 1000)}'")
            }
            when (filter.level and Web3FilterParams.FILTER_MASK) {
                Web3FilterParams.FILTER_GOOD_ONLY -> filters.add("w.level >= 11")
                Web3FilterParams.FILTER_GOOD_AND_UNKNOWN -> filters.add("w.level >= 10")
                Web3FilterParams.FILTER_GOOD_AND_SPAM -> filters.add("(w.level >= 11 OR w.level <= 1)")
                Web3FilterParams.FILTER_ALL -> {}
            }
            return filters
        }
    }
}
