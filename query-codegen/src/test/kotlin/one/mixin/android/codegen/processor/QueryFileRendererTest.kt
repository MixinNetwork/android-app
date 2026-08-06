package one.mixin.android.codegen.processor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QueryFileRendererTest {
    @Test
    fun rendersLimitOffsetParametersInEveryQuery() {
        val source =
            QueryFileRenderer().render(
                QueryProviderModel(
                    packageName = "one.mixin.android.db.provider",
                    generatedName = "GeneratedDataProvider",
                    functions = emptyList(),
                    limitOffsetFunctions =
                        listOf(
                            LimitOffsetPagingSourceFunctionModel(
                                name = "observeConversations",
                                returnType = "PagingSource<Int, ConversationItem>",
                                returnTypeImports = listOf("androidx.paging.PagingSource", "one.mixin.android.vo.ConversationItem"),
                                parameters =
                                    listOf(
                                        QueryParameterModel("circleId", "String"),
                                        QueryParameterModel("database", "MixinDatabase"),
                                    ),
                                parameterImports = listOf("one.mixin.android.db.MixinDatabase"),
                                countSql = "SELECT count(1) FROM conversations WHERE circle_id = '{{circleId}}'",
                                offsetSql = "SELECT rowid FROM conversations WHERE circle_id = '{{circleId}}' LIMIT ? OFFSET ?",
                                querySql = "SELECT * FROM conversations WHERE circle_id = '{{circleId}}' AND rowid IN ({{ids}})",
                                tables = listOf("conversations"),
                                databaseParameter = "database",
                                converterName = "convertToConversationItems",
                            ),
                        ),
                ),
            )

        assertFalse(source.contains("{{circleId}}"))
        assertTrue(source.contains("circle_id = '${'$'}circleId'"))
    }

    @Test
    fun rendersSuspendQueryWithNullableStringBinds() {
        val source =
            QueryFileRenderer().render(
                QueryProviderModel(
                    packageName = "one.mixin.android.db.provider",
                    generatedName = "GeneratedDataProvider",
                    functions =
                        listOf(
                            QueryFunctionModel(
                                name = "fuzzySearchToken",
                                returnType = "List<TokenItem>",
                                returnTypeImports = listOf("one.mixin.android.vo.safe.TokenItem"),
                                parameters =
                                    listOf(
                                        QueryParameterModel("name", "String?"),
                                        QueryParameterModel("symbol", "String?"),
                                        QueryParameterModel("db", "MixinDatabase"),
                                        QueryParameterModel("cancellationSignal", "CancellationSignal"),
                                    ),
                                parameterImports =
                                    listOf(
                                        "android.os.CancellationSignal",
                                        "one.mixin.android.db.MixinDatabase",
                                    ),
                                sql =
                                    """
                                    SELECT *
                                    FROM tokens
                                    WHERE symbol LIKE '%' || ? || '%'
                                    """.trimIndent(),
                                bindParameters = listOf("symbol", "name", "symbol", "name"),
                                databaseParameter = "db",
                                cancellationSignalParameter = "cancellationSignal",
                                callableName = "callableTokenItem",
                            ),
                        ),
                ),
            )

        assertEquals(
            """
            package one.mixin.android.db.provider

            import android.annotation.SuppressLint
            import android.os.CancellationSignal
            import kotlinx.coroutines.withContext
            import one.mixin.android.db.MixinDatabase
            import one.mixin.android.db.datasource.RoomDatabaseCompat
            import one.mixin.android.db.datasource.RoomQuery
            import one.mixin.android.vo.safe.TokenItem

            @SuppressLint("RestrictedApi")
            object GeneratedDataProvider {
                @Suppress("LocalVariableName", "JoinDeclarationAndAssignment")
                suspend fun fuzzySearchToken(
                    name: String?,
                    symbol: String?,
                    db: MixinDatabase,
                    cancellationSignal: CancellationSignal,
                ): List<TokenItem> {
                    val _sql = ""${'"'}
                    SELECT *
                    FROM tokens
                    WHERE symbol LIKE '%' || ? || '%'
                    ""${'"'}.trimIndent()
                    val _statement = RoomQuery.acquire(_sql, 4)
                    var _argIndex = 1
                    if (symbol == null) {
                        _statement.bindNull(_argIndex)
                    } else {
                        _statement.bindString(_argIndex, symbol)
                    }
                    _argIndex = 2
                    if (name == null) {
                        _statement.bindNull(_argIndex)
                    } else {
                        _statement.bindString(_argIndex, name)
                    }
                    _argIndex = 3
                    if (symbol == null) {
                        _statement.bindNull(_argIndex)
                    } else {
                        _statement.bindString(_argIndex, symbol)
                    }
                    _argIndex = 4
                    if (name == null) {
                        _statement.bindNull(_argIndex)
                    } else {
                        _statement.bindString(_argIndex, name)
                    }
                    return withContext(RoomDatabaseCompat.queryContext(db)) {
                        callableTokenItem(db, _statement, cancellationSignal).call()
                    }
                }
            }
            """.trimIndent(),
            source,
        )
    }

    @Test
    fun rendersLimitOffsetPagingSource() {
        val source =
            QueryFileRenderer().render(
                QueryProviderModel(
                    packageName = "one.mixin.android.db.provider",
                    generatedName = "GeneratedDataProvider",
                    functions = emptyList(),
                    limitOffsetFunctions =
                        listOf(
                            LimitOffsetPagingSourceFunctionModel(
                                name = "observeConversations",
                                returnType = "PagingSource<Int, ConversationItem>",
                                returnTypeImports =
                                    listOf(
                                        "androidx.paging.PagingSource",
                                        "one.mixin.android.vo.ConversationItem",
                                    ),
                                parameters = listOf(QueryParameterModel("database", "MixinDatabase")),
                                parameterImports = listOf("one.mixin.android.db.MixinDatabase"),
                                countSql = "SELECT count(1) FROM conversations",
                                offsetSql = "SELECT c.rowid FROM conversations c LIMIT ? OFFSET ?",
                                querySql = "SELECT * FROM conversations c WHERE c.rowid IN ({{ids}})",
                                tables = listOf("conversations", "users"),
                                databaseParameter = "database",
                                converterName = "convertToConversationItems",
                            ),
                        ),
                ),
            )

        assertEquals(
            """
            package one.mixin.android.db.provider

            import android.annotation.SuppressLint
            import android.database.Cursor
            import androidx.paging.PagingSource
            import one.mixin.android.db.MixinDatabase
            import one.mixin.android.db.datasource.MixinCountLimitOffsetDataSource
            import one.mixin.android.db.datasource.RoomQuery
            import one.mixin.android.db.datasource.query
            import one.mixin.android.vo.ConversationItem

            @SuppressLint("RestrictedApi")
            object GeneratedDataProvider {
                fun observeConversations(
                    database: MixinDatabase,
                ): PagingSource<Int, ConversationItem> {
                    val countStatement = RoomQuery.acquire(
                        ""${'"'}
                        SELECT count(1) FROM conversations
                        ""${'"'}.trimIndent(),
                        0,
                    )
                    val offsetStatement = RoomQuery.acquire(
                        ""${'"'}
                        SELECT c.rowid FROM conversations c LIMIT ? OFFSET ?
                        ""${'"'}.trimIndent(),
                        2,
                    )
                    val querySqlGenerator = fun(ids: String): RoomQuery {
                        return RoomQuery.acquire(
                            ""${'"'}
                            SELECT * FROM conversations c WHERE c.rowid IN (${'$'}ids)
                            ""${'"'}.trimIndent(),
                            0,
                        )
                    }
                    return object : MixinCountLimitOffsetDataSource<ConversationItem>(
                        offsetStatement,
                        { connection -> connection.query(countStatement).use { if (it.moveToFirst()) it.getInt(0) else 0 } },
                        querySqlGenerator,
                        database,
                        "conversations",
                        "users",
                    ) {
                        override fun convertRows(cursor: Cursor): List<ConversationItem> =
                            convertToConversationItems(cursor)
                    }
                }
            }
            """.trimIndent(),
            source,
        )
    }

    @Test
    fun rendersNoCountPagingSource() {
        val source =
            QueryFileRenderer().render(
                QueryProviderModel(
                    packageName = "one.mixin.android.db.provider",
                    generatedName = "GeneratedDataProvider",
                    functions = emptyList(),
                    noCountFunctions =
                        listOf(
                            NoCountPagingSourceFunctionModel(
                                name = "getPinMessages",
                                returnType = "PagingSource<Int, ChatHistoryMessageItem>",
                                returnTypeImports =
                                    listOf(
                                        "androidx.paging.PagingSource",
                                        "one.mixin.android.vo.ChatHistoryMessageItem",
                                    ),
                                parameters =
                                    listOf(
                                        QueryParameterModel("database", "MixinDatabase"),
                                        QueryParameterModel("conversationId", "String"),
                                        QueryParameterModel("count", "Int"),
                                    ),
                                parameterImports = listOf("one.mixin.android.db.MixinDatabase"),
                                sql = "SELECT * FROM messages WHERE conversation_id = ?",
                                bindParameters = listOf("conversationId"),
                                countParameter = "count",
                                tables = listOf("pin_messages", "messages"),
                                databaseParameter = "database",
                                converterName = "convertChatHistoryMessageItem",
                            ),
                        ),
                ),
            )

        assertEquals(
            """
            package one.mixin.android.db.provider

            import android.annotation.SuppressLint
            import android.database.Cursor
            import androidx.paging.PagingSource
            import one.mixin.android.db.MixinDatabase
            import one.mixin.android.db.datasource.MixinNonCountLimitOffsetDataSource
            import one.mixin.android.db.datasource.RoomQuery
            import one.mixin.android.vo.ChatHistoryMessageItem

            @SuppressLint("RestrictedApi")
            object GeneratedDataProvider {
                fun getPinMessages(
                    database: MixinDatabase,
                    conversationId: String,
                    count: Int,
                ): PagingSource<Int, ChatHistoryMessageItem> {
                    val _statement = RoomQuery.acquire(
                        ""${'"'}
                        SELECT * FROM messages WHERE conversation_id = ?
                        ""${'"'}.trimIndent(),
                        1,
                    )
                    var _argIndex = 1
                    _statement.bindString(_argIndex, conversationId)
                    return object : MixinNonCountLimitOffsetDataSource<ChatHistoryMessageItem>(
                        _statement,
                        count,
                        database,
                        "pin_messages",
                        "messages",
                    ) {
                        override fun convertRows(cursor: Cursor): List<ChatHistoryMessageItem> =
                            convertChatHistoryMessageItem(cursor)
                    }
                }
            }
            """.trimIndent(),
            source,
        )
    }

    @Test
    fun rendersRawCursorQuery() {
        val source =
            QueryFileRenderer().render(
                QueryProviderModel(
                    packageName = "one.mixin.android.db.fetcher",
                    generatedName = "MessageFetcherGenerated",
                    functions = emptyList(),
                    rawCursorFunctions =
                        listOf(
                            RawCursorQueryFunctionModel(
                                name = "loadBottomMessages",
                                returnType = "List<MessageItem>",
                                returnTypeImports = listOf("one.mixin.android.vo.MessageItem"),
                                parameters =
                                    listOf(
                                        QueryParameterModel("db", "MixinDatabase"),
                                        QueryParameterModel("conversationId", "String"),
                                        QueryParameterModel("limit", "Int"),
                                    ),
                                parameterImports = listOf("one.mixin.android.db.MixinDatabase"),
                                sql = "SELECT * FROM messages WHERE conversation_id = ? LIMIT ?",
                                bindParameters = listOf("conversationId", "limit"),
                                databaseParameter = "db",
                                converterName = "one.mixin.android.db.provider.convertToMessageItems",
                            ),
                        ),
                ),
            )

        assertEquals(
            """
            package one.mixin.android.db.fetcher

            import android.annotation.SuppressLint
            import one.mixin.android.db.MixinDatabase
            import one.mixin.android.db.datasource.query
            import one.mixin.android.db.provider.convertToMessageItems
            import one.mixin.android.vo.MessageItem

            @SuppressLint("RestrictedApi")
            object MessageFetcherGenerated {
                fun loadBottomMessages(
                    db: MixinDatabase,
                    conversationId: String,
                    limit: Int,
                ): List<MessageItem> {
                    val cursor = db.query(
                        ""${'"'}
                        SELECT * FROM messages WHERE conversation_id = ? LIMIT ?
                        ""${'"'}.trimIndent(),
                        arrayOf<Any?>(conversationId, limit),
                    )
                    return cursor.use {
                        convertToMessageItems(it)
                    }
                }
            }
            """.trimIndent(),
            source,
        )
    }

    @Test
    fun rendersRoomRawQueryBuilder() {
        val source =
            QueryFileRenderer().render(
                QueryProviderModel(
                    packageName = "one.mixin.android.ui.wallet",
                    generatedName = "WalletFilterQueryGenerated",
                    functions = emptyList(),
                    simpleQueryFunctions =
                        listOf(
                            RoomRawQueryFunctionModel(
                                name = "snapshotsQuery",
                                returnType = "RoomRawQuery",
                                returnTypeImports = listOf("androidx.room3.RoomRawQuery"),
                                parameters =
                                    listOf(
                                        QueryParameterModel("whereSql", "String"),
                                        QueryParameterModel("orderSql", "String"),
                                    ),
                                parameterImports = emptyList(),
                                sql = "SELECT * FROM snapshots {{whereSql}} {{orderSql}}",
                            ),
                        ),
                ),
            )

        assertEquals(
            """
            package one.mixin.android.ui.wallet

            import android.annotation.SuppressLint
            import androidx.room3.RoomRawQuery

            @SuppressLint("RestrictedApi")
            object WalletFilterQueryGenerated {
                fun snapshotsQuery(
                    whereSql: String,
                    orderSql: String,
                ): RoomRawQuery =
                    RoomRawQuery(
                        ""${'"'}
                        SELECT * FROM snapshots ${'$'}whereSql ${'$'}orderSql
                        ""${'"'}.trimIndent(),
                    )
            }
            """.trimIndent(),
            source,
        )
    }

    @Test
    fun rendersPagingSourceQuery() {
        val source =
            QueryFileRenderer().render(
                QueryProviderModel(
                    packageName = "one.mixin.android.db.provider",
                    generatedName = "LimitOrderDataProviderGenerated",
                    functions = emptyList(),
                    pagingSourceFunctions =
                        listOf(
                            PagingSourceQueryFunctionModel(
                                name = "allOrders",
                                returnType = "PagingSource<Int, OrderItem>",
                                returnTypeImports =
                                    listOf(
                                        "androidx.paging.PagingSource",
                                        "one.mixin.android.vo.route.OrderItem",
                                    ),
                                parameters =
                                    listOf(
                                        QueryParameterModel("database", "WalletDatabase"),
                                        QueryParameterModel("whereSql", "String"),
                                        QueryParameterModel("whereClauseSql", "String"),
                                        QueryParameterModel("orderBySql", "String"),
                                    ),
                                parameterImports = listOf("one.mixin.android.db.WalletDatabase"),
                                countSql = "SELECT COUNT(DISTINCT o.rowid) FROM orders o {{whereSql}}",
                                offsetSql = "SELECT DISTINCT o.rowid FROM orders o {{whereSql}} LIMIT ? OFFSET ?",
                                querySql = "SELECT * FROM orders o WHERE o.rowid IN ({{ids}}) {{whereClauseSql}} ORDER BY {{orderBySql}}",
                                tables = listOf("orders"),
                                databaseParameter = "database",
                                converterName = "convertToOrderItems",
                            ),
                        ),
                ),
            )

        assertEquals(
            """
            package one.mixin.android.db.provider

            import android.annotation.SuppressLint
            import androidx.paging.PagingSource
            import androidx.paging.PagingState
            import androidx.room3.PooledConnection
            import androidx.room3.paging.util.INITIAL_ITEM_COUNT
            import androidx.room3.paging.util.getClippedRefreshKey
            import androidx.room3.paging.util.getLimit
            import androidx.room3.paging.util.getOffset
            import androidx.room3.useReaderConnection
            import androidx.room3.withReadTransaction
            import java.util.concurrent.atomic.AtomicInteger
            import kotlinx.coroutines.withContext
            import one.mixin.android.db.WalletDatabase
            import one.mixin.android.db.datasource.RoomDatabaseCompat
            import one.mixin.android.db.datasource.RoomQuery
            import one.mixin.android.db.datasource.query
            import one.mixin.android.vo.route.OrderItem

            @SuppressLint("RestrictedApi")
            object LimitOrderDataProviderGenerated {
                fun allOrders(
                    database: WalletDatabase,
                    whereSql: String,
                    whereClauseSql: String,
                    orderBySql: String,
                ): PagingSource<Int, OrderItem> =
                    object : PagingSource<Int, OrderItem>() {
                        private val itemCount = AtomicInteger(INITIAL_ITEM_COUNT)

                        init {
                            RoomDatabaseCompat.observeInvalidation(database, this, "orders")
                        }

                        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, OrderItem> {
                            return withContext(RoomDatabaseCompat.queryContext(database)) {
                                val tempCount = itemCount.get()
                                if (tempCount == INITIAL_ITEM_COUNT) {
                                    database.withReadTransaction {
                                        val count = countItems(this)
                                        itemCount.set(count)
                                        queryData(this, params, count)
                                    }
                                } else {
                                    database.useReaderConnection { connection ->
                                        val loadResult = queryData(connection, params, tempCount)
                                        @Suppress("UNCHECKED_CAST")
                                        if (invalid) LoadResult.Invalid() else loadResult
                                    }
                                }
                            }
                        }

                        private suspend fun countItems(connection: PooledConnection): Int {
                            val countStatement = RoomQuery.acquire(
                                ""${'"'}
                                SELECT COUNT(DISTINCT o.rowid) FROM orders o ${'$'}whereSql
                                ""${'"'}.trimIndent(),
                                0,
                            )
                            val cursor = connection.query(countStatement)
                            return try {
                                if (cursor.moveToFirst()) cursor.getInt(0) else 0
                            } finally {
                                cursor.close()
                                countStatement.release()
                            }
                        }

                        private suspend fun queryData(connection: PooledConnection, params: LoadParams<Int>, itemCount: Int): LoadResult.Page<Int, OrderItem> {
                            val key = params.key ?: 0
                            val limit = getLimit(params, key)
                            val offset = getOffset(params, key, itemCount)
                            val offsetStatement = RoomQuery.acquire(
                                ""${'"'}
                                SELECT DISTINCT o.rowid FROM orders o ${'$'}whereSql LIMIT ? OFFSET ?
                                ""${'"'}.trimIndent(),
                                2,
                            )
                            offsetStatement.bindLong(1, limit.toLong())
                            offsetStatement.bindLong(2, offset.toLong())
                            val offsetCursor = connection.query(offsetStatement)
                            val ids = mutableListOf<String>()
                            try {
                                while (offsetCursor.moveToNext()) {
                                    ids.add("'${'$'}{offsetCursor.getLong(0)}'")
                                }
                            } finally {
                                offsetCursor.close()
                                offsetStatement.release()
                            }
                            val data = if (ids.isEmpty()) {
                                emptyList()
                            } else {
                                val queryStatement = querySqlGenerator(ids.joinToString())
                                val cursor = connection.query(queryStatement)
                                try {
                                    convertToOrderItems(cursor)
                                } finally {
                                    cursor.close()
                                    queryStatement.release()
                                }
                            }
                            val nextPosToLoad = offset + data.size
                            val nextKey = if (ids.isEmpty() || ids.size < limit || nextPosToLoad >= itemCount) null else nextPosToLoad
                            val prevKey = if (offset <= 0 || ids.isEmpty()) null else offset
                            return LoadResult.Page(
                                data = data,
                                prevKey = prevKey,
                                nextKey = nextKey,
                                itemsBefore = offset,
                                itemsAfter = maxOf(0, itemCount - nextPosToLoad),
                            )
                        }

                        private fun querySqlGenerator(ids: String): RoomQuery {
                            return RoomQuery.acquire(
                                ""${'"'}
                                SELECT * FROM orders o WHERE o.rowid IN (${'$'}ids) ${'$'}whereClauseSql ORDER BY ${'$'}orderBySql
                                ""${'"'}.trimIndent(),
                                0,
                            )
                        }

                        override fun getRefreshKey(state: PagingState<Int, OrderItem>): Int? {
                            return state.getClippedRefreshKey()
                        }

                        override val jumpingSupported: Boolean
                            get() = true
                    }
            }
            """.trimIndent(),
            source,
        )
    }
}
