package one.mixin.android.db.provider

import android.annotation.SuppressLint
import android.os.CancellationSignal
import androidx.paging.DataSource
import kotlinx.coroutines.withContext
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.datasource.RoomDatabaseCompat
import one.mixin.android.fts.FtsDataSource
import one.mixin.android.fts.FtsDatabase
import one.mixin.android.fts.rawSearch
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.SearchBot
import one.mixin.android.vo.SearchMessageDetailItem
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import one.mixin.android.vo.market.Market
import one.mixin.android.vo.safe.SafeCollectible
import one.mixin.android.vo.safe.TokenItem

@SuppressLint("RestrictedApi")
class DataProvider {
    companion object {
        fun observeConversations(database: MixinDatabase): DataSource.Factory<Int, ConversationItem> =
            DataProviderGenerated.observeConversations(database)

        fun observeConversationsByCircleId(
            circleId: String,
            database: MixinDatabase,
        ): DataSource.Factory<Int, ConversationItem> =
            DataProviderGenerated.observeConversationsByCircleId(circleId, database)

        suspend fun fuzzySearchToken(
            name: String?,
            symbol: String?,
            db: MixinDatabase,
            cancellationSignal: CancellationSignal,
        ): List<TokenItem> =
            DataProviderGenerated.fuzzySearchToken(name, symbol, db, cancellationSignal)

        suspend fun fuzzySearchUser(
            username: String?,
            identityNumber: String?,
            phone: String?,
            id: String?,
            db: MixinDatabase,
            cancellationSignal: CancellationSignal,
        ): List<User> =
            DataProviderGenerated.fuzzySearchUser(username, identityNumber, phone, id, db, cancellationSignal)

        suspend fun fuzzySearchBots(
            username: String?,
            identityNumber: String?,
            id: String?,
            db: MixinDatabase,
            cancellationSignal: CancellationSignal,
        ): List<SearchBot> =
            DataProviderGenerated.fuzzySearchBots(username, identityNumber, id, db, cancellationSignal)

        suspend fun fuzzyMarkets(
            keyword: String,
            db: MixinDatabase,
            cancellationSignal: CancellationSignal,
        ): List<Market> =
            DataProviderGenerated.fuzzyMarkets(keyword, db, cancellationSignal)

        suspend fun fuzzyInscription(
            keyword: String?,
            db: MixinDatabase,
            cancellationSignal: CancellationSignal,
        ): List<SafeCollectible> =
            DataProviderGenerated.fuzzyInscription(keyword, db, cancellationSignal)

        suspend fun fuzzySearchChat(
            query: String?,
            db: MixinDatabase,
            cancellationSignal: CancellationSignal,
        ): List<ChatMinimal> =
            DataProviderGenerated.fuzzySearchChat(query, db, cancellationSignal)

        suspend fun fuzzySearchMessage(
            ftsDatabase: FtsDatabase,
            query: String,
            db: MixinDatabase,
            cancellationSignal: CancellationSignal,
        ): List<SearchMessageItem> =
            withContext(RoomDatabaseCompat.queryContext(db)) {
                val result = ftsDatabase.rawSearch(query, cancellationSignal)
                if (result.isEmpty()) return@withContext emptyList()
                val ids =
                    result.joinToString(
                        prefix = "'",
                        postfix = "'",
                        separator = "', '",
                    ) { it.messageId }
                return@withContext DataProviderGenerated.fuzzySearchMessageItems(db, ids).map {
                    val obtained = result.find { item -> item.conversationId == it.conversationId }
                    if (obtained != null) {
                        it.messageCount = obtained.messageCount
                    }
                    it
                }
            }

        fun fuzzySearchMessageDetail(
            ftsDatabase: FtsDatabase,
            query: String,
            conversationId: String,
            database: MixinDatabase,
            cancellationSignal: CancellationSignal,
        ) =
            object : DataSource.Factory<Int, SearchMessageDetailItem>() {
                override fun create(): DataSource<Int, SearchMessageDetailItem> {
                    return FtsDataSource(ftsDatabase, database, query, conversationId, cancellationSignal)
                }
            }

        fun getPinMessages(
            database: MixinDatabase,
            conversationId: String,
            count: Int,
        ): DataSource.Factory<Int, ChatHistoryMessageItem> =
            DataProviderGenerated.getPinMessages(database, conversationId, count)
    }
}
