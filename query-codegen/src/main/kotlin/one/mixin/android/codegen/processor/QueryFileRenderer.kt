package one.mixin.android.codegen.processor

data class QueryProviderModel(
    val packageName: String,
    val generatedName: String,
    val functions: List<QueryFunctionModel>,
    val limitOffsetFunctions: List<LimitOffsetDataSourceFunctionModel> = emptyList(),
    val noCountFunctions: List<NoCountDataSourceFunctionModel> = emptyList(),
    val rawCursorFunctions: List<RawCursorQueryFunctionModel> = emptyList(),
    val simpleQueryFunctions: List<RoomRawQueryFunctionModel> = emptyList(),
    val pagingSourceFunctions: List<PagingSourceQueryFunctionModel> = emptyList(),
)

data class QueryFunctionModel(
    val name: String,
    val returnType: String,
    val returnTypeImports: List<String>,
    val parameters: List<QueryParameterModel>,
    val parameterImports: List<String>,
    val sql: String,
    val bindParameters: List<String>,
    val databaseParameter: String,
    val cancellationSignalParameter: String,
    val callableName: String,
)

data class QueryParameterModel(
    val name: String,
    val type: String,
)

data class LimitOffsetDataSourceFunctionModel(
    val name: String,
    val returnType: String,
    val returnTypeImports: List<String>,
    val parameters: List<QueryParameterModel>,
    val parameterImports: List<String>,
    val countSql: String,
    val offsetSql: String,
    val querySql: String,
    val tables: List<String>,
    val databaseParameter: String,
    val converterName: String,
)

data class NoCountDataSourceFunctionModel(
    val name: String,
    val returnType: String,
    val returnTypeImports: List<String>,
    val parameters: List<QueryParameterModel>,
    val parameterImports: List<String>,
    val sql: String,
    val bindParameters: List<String>,
    val countParameter: String,
    val tables: List<String>,
    val databaseParameter: String,
    val converterName: String,
)

data class RawCursorQueryFunctionModel(
    val name: String,
    val returnType: String,
    val returnTypeImports: List<String>,
    val parameters: List<QueryParameterModel>,
    val parameterImports: List<String>,
    val sql: String,
    val bindParameters: List<String>,
    val databaseParameter: String,
    val converterName: String,
)

data class RoomRawQueryFunctionModel(
    val name: String,
    val returnType: String,
    val returnTypeImports: List<String>,
    val parameters: List<QueryParameterModel>,
    val parameterImports: List<String>,
    val sql: String,
)

data class PagingSourceQueryFunctionModel(
    val name: String,
    val returnType: String,
    val returnTypeImports: List<String>,
    val parameters: List<QueryParameterModel>,
    val parameterImports: List<String>,
    val countSql: String,
    val offsetSql: String,
    val querySql: String,
    val tables: List<String>,
    val databaseParameter: String,
    val converterName: String,
)

class QueryFileRenderer {
    fun render(model: QueryProviderModel): String {
        val imports =
            (
                listOf(
                    "android.annotation.SuppressLint",
                ) +
                    model.roomQueryImports() +
                    model.roomDatabaseCompatImports() +
                    model.coroutinesImports() +
                    model.functions.flatMap { it.parameterImports + it.returnTypeImports } +
                    model.limitOffsetFunctions.flatMap { it.parameterImports + it.returnTypeImports } +
                    model.noCountFunctions.flatMap { it.parameterImports + it.returnTypeImports } +
                    model.rawCursorFunctions.flatMap { it.parameterImports + it.returnTypeImports + it.converterImport() } +
                    model.simpleQueryFunctions.flatMap { it.parameterImports + it.returnTypeImports } +
                    model.pagingSourceFunctions.flatMap { it.parameterImports + it.returnTypeImports + it.converterImport() } +
                    model.queryExtensionImports() +
                    model.limitOffsetImports() +
                    model.noCountImports() +
                    model.pagingSourceImports()
            ).distinct().sorted()

        val lines = mutableListOf<String>()
        lines += "package ${model.packageName}"
        lines += ""
        imports.forEach { lines += "import $it" }
        lines += ""
        lines += "@SuppressLint(\"RestrictedApi\")"
        lines += "object ${model.generatedName} {"
        model.functions.forEachIndexed { index, function ->
            if (index > 0) {
                lines += ""
            }
            renderFunction(lines, function)
        }
        model.limitOffsetFunctions.forEachIndexed { index, function ->
            if (model.functions.isNotEmpty() || index > 0) {
                lines += ""
            }
            renderLimitOffsetFunction(lines, function)
        }
        model.noCountFunctions.forEachIndexed { index, function ->
            if (model.functions.isNotEmpty() || model.limitOffsetFunctions.isNotEmpty() || index > 0) {
                lines += ""
            }
            renderNoCountFunction(lines, function)
        }
        model.rawCursorFunctions.forEachIndexed { index, function ->
            if (model.functions.isNotEmpty() || model.limitOffsetFunctions.isNotEmpty() || model.noCountFunctions.isNotEmpty() || index > 0) {
                lines += ""
            }
            renderRawCursorFunction(lines, function)
        }
        model.simpleQueryFunctions.forEachIndexed { index, function ->
            if (model.functions.isNotEmpty() || model.limitOffsetFunctions.isNotEmpty() || model.noCountFunctions.isNotEmpty() || model.rawCursorFunctions.isNotEmpty() || index > 0) {
                lines += ""
            }
            renderSimpleQueryFunction(lines, function)
        }
        model.pagingSourceFunctions.forEachIndexed { index, function ->
            if (model.functions.isNotEmpty() || model.limitOffsetFunctions.isNotEmpty() || model.noCountFunctions.isNotEmpty() || model.rawCursorFunctions.isNotEmpty() || model.simpleQueryFunctions.isNotEmpty() || index > 0) {
                lines += ""
            }
            renderPagingSourceFunction(lines, function)
        }
        lines += "}"
        return lines.joinToString("\n")
    }

    private fun renderFunction(
        lines: MutableList<String>,
        function: QueryFunctionModel,
    ) {
        lines += "    @Suppress(\"LocalVariableName\", \"JoinDeclarationAndAssignment\")"
        lines += "    suspend fun ${function.name}("
        function.parameters.forEach { parameter ->
            lines += "        ${parameter.name}: ${parameter.type},"
        }
        lines += "    ): ${function.returnType} {"
        lines += "        val _sql = \"\"\""
        function.sql.lineSequence().forEach { sqlLine ->
            lines += "        $sqlLine"
        }
        lines += "        \"\"\".trimIndent()"
        lines += "        val _statement = RoomQuery.acquire(_sql, ${function.bindParameters.size})"
        function.bindParameters.forEachIndexed { index, bindParameter ->
            val argIndex = index + 1
            if (index == 0) {
                lines += "        var _argIndex = $argIndex"
            } else {
                lines += "        _argIndex = $argIndex"
            }
            renderBind(lines, function, bindParameter)
        }
        lines += "        return withContext(RoomDatabaseCompat.queryContext(${function.databaseParameter})) {"
        lines += "            ${function.callableName}(${function.databaseParameter}, _statement, ${function.cancellationSignalParameter}).call()"
        lines += "        }"
        lines += "    }"
    }

    private fun renderLimitOffsetFunction(
        lines: MutableList<String>,
        function: LimitOffsetDataSourceFunctionModel,
    ) {
        val itemType = function.returnType.dataSourceItemType()
        lines += "    fun ${function.name}("
        function.parameters.forEach { parameter ->
            lines += "        ${parameter.name}: ${parameter.type},"
        }
        lines += "    ): ${function.returnType} ="
        lines += "        object : DataSource.Factory<Int, $itemType>() {"
        lines += "            override fun create(): DataSource<Int, $itemType> {"
        renderRoomQueryAcquire(lines, "countStatement", function.countSql, 0, "                ")
        renderRoomQueryAcquire(lines, "offsetStatement", function.offsetSql, 2, "                ")
        lines += "                val querySqlGenerator = fun(ids: String): RoomQuery {"
        lines += "                    return RoomQuery.acquire("
        renderSqlLiteral(lines, function.querySql.renderSqlTemplate(function.parameters.map { it.name } + "ids"), "                        ")
        lines += "                        0,"
        lines += "                    )"
        lines += "                }"
        lines += "                return object : MixinLimitOffsetDataSource<$itemType>("
        lines += "                    ${function.databaseParameter},"
        lines += "                    countStatement,"
        lines += "                    offsetStatement,"
        lines += "                    querySqlGenerator,"
        lines += "                    arrayOf(${function.tables.joinToString { "\"$it\"" }}),"
        lines += "                ) {"
        lines += "                    override fun convertRows(cursor: Cursor?): List<$itemType> ="
        lines += "                        ${function.converterName}(cursor)"
        lines += "                }"
        lines += "            }"
        lines += "        }"
    }

    private fun renderRawCursorFunction(
        lines: MutableList<String>,
        function: RawCursorQueryFunctionModel,
    ) {
        lines += "    fun ${function.name}("
        function.parameters.forEach { parameter ->
            lines += "        ${parameter.name}: ${parameter.type},"
        }
        lines += "    ): ${function.returnType} {"
        lines += "        val cursor = ${function.databaseParameter}.query("
        renderSqlLiteral(lines, function.sql.renderSqlTemplate(function.parameters.map { it.name }), "            ")
        lines += "            arrayOf<Any?>(${function.bindParameters.joinToString { function.rawBindExpression(it) }}),"
        lines += "        )"
        lines += "        return cursor.use {"
        lines += "            ${function.converterCallName()}(it)"
        lines += "        }"
        lines += "    }"
    }

    private fun renderSimpleQueryFunction(
        lines: MutableList<String>,
        function: RoomRawQueryFunctionModel,
    ) {
        lines += "    fun ${function.name}("
        function.parameters.forEach { parameter ->
            lines += "        ${parameter.name}: ${parameter.type},"
        }
        lines += "    ): ${function.returnType} ="
        lines += "        RoomRawQuery("
        renderSqlLiteral(lines, function.sql.renderSqlTemplate(function.parameters.map { it.name }), "            ")
        lines += "        )"
    }

    private fun renderPagingSourceFunction(
        lines: MutableList<String>,
        function: PagingSourceQueryFunctionModel,
    ) {
        val itemType = function.returnType.dataSourceItemType()
        lines += "    fun ${function.name}("
        function.parameters.forEach { parameter ->
            lines += "        ${parameter.name}: ${parameter.type},"
        }
        lines += "    ): ${function.returnType} ="
        lines += "        object : PagingSource<Int, $itemType>() {"
        lines += "            private val itemCount = AtomicInteger(INITIAL_ITEM_COUNT)"
        lines += ""
        lines += "            init {"
        lines += "                RoomDatabaseCompat.observeInvalidation(${function.databaseParameter}, this, ${function.tables.joinToString { "\"$it\"" }})"
        lines += "            }"
        lines += ""
        lines += "            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, $itemType> {"
        lines += "                return withContext(RoomDatabaseCompat.queryContext(${function.databaseParameter})) {"
        lines += "                    val tempCount = itemCount.get()"
        lines += "                    if (tempCount == INITIAL_ITEM_COUNT) {"
        lines += "                        ${function.databaseParameter}.withReadTransaction {"
        lines += "                            val count = countItems()"
        lines += "                            itemCount.set(count)"
        lines += "                            queryData(params, count)"
        lines += "                        }"
        lines += "                    } else {"
        lines += "                        val loadResult = queryData(params, tempCount)"
        lines += "                        @Suppress(\"UNCHECKED_CAST\")"
        lines += "                        if (invalid) LoadResult.Invalid() else loadResult"
        lines += "                    }"
        lines += "                }"
        lines += "            }"
        lines += ""
        lines += "            private fun countItems(): Int {"
        renderRoomQueryAcquire(lines, "countStatement", function.countSql.renderSqlTemplate(function.parameters.map { it.name }), 0, "                ")
        lines += "                val cursor = ${function.databaseParameter}.query(countStatement)"
        lines += "                return try {"
        lines += "                    if (cursor.moveToFirst()) cursor.getInt(0) else 0"
        lines += "                } finally {"
        lines += "                    cursor.close()"
        lines += "                    countStatement.release()"
        lines += "                }"
        lines += "            }"
        lines += ""
        lines += "            private fun queryData(params: LoadParams<Int>, itemCount: Int): LoadResult.Page<Int, $itemType> {"
        lines += "                val key = params.key ?: 0"
        lines += "                val limit = getLimit(params, key)"
        lines += "                val offset = getOffset(params, key, itemCount)"
        renderRoomQueryAcquire(lines, "offsetStatement", function.offsetSql.renderSqlTemplate(function.parameters.map { it.name }), 2, "                ")
        lines += "                offsetStatement.bindLong(1, limit.toLong())"
        lines += "                offsetStatement.bindLong(2, offset.toLong())"
        lines += "                val offsetCursor = ${function.databaseParameter}.query(offsetStatement)"
        lines += "                val ids = mutableListOf<String>()"
        lines += "                try {"
        lines += "                    while (offsetCursor.moveToNext()) {"
        lines += "                        ids.add(\"'\${offsetCursor.getLong(0)}'\")"
        lines += "                    }"
        lines += "                } finally {"
        lines += "                    offsetCursor.close()"
        lines += "                    offsetStatement.release()"
        lines += "                }"
        lines += "                val data = if (ids.isEmpty()) {"
        lines += "                    emptyList()"
        lines += "                } else {"
        lines += "                    val queryStatement = querySqlGenerator(ids.joinToString())"
        lines += "                    val cursor = ${function.databaseParameter}.query(queryStatement)"
        lines += "                    try {"
        lines += "                        ${function.converterName}(cursor)"
        lines += "                    } finally {"
        lines += "                        cursor.close()"
        lines += "                        queryStatement.release()"
        lines += "                    }"
        lines += "                }"
        lines += "                val nextPosToLoad = offset + data.size"
        lines += "                val nextKey = if (ids.isEmpty() || ids.size < limit || nextPosToLoad >= itemCount) null else nextPosToLoad"
        lines += "                val prevKey = if (offset <= 0 || ids.isEmpty()) null else offset"
        lines += "                return LoadResult.Page("
        lines += "                    data = data,"
        lines += "                    prevKey = prevKey,"
        lines += "                    nextKey = nextKey,"
        lines += "                    itemsBefore = offset,"
        lines += "                    itemsAfter = maxOf(0, itemCount - nextPosToLoad),"
        lines += "                )"
        lines += "            }"
        lines += ""
        lines += "            private fun querySqlGenerator(ids: String): RoomQuery {"
        lines += "                return RoomQuery.acquire("
        renderSqlLiteral(lines, function.querySql.renderSqlTemplate(function.parameters.map { it.name } + "ids"), "                    ")
        lines += "                    0,"
        lines += "                )"
        lines += "            }"
        lines += ""
        lines += "            override fun getRefreshKey(state: PagingState<Int, $itemType>): Int? {"
        lines += "                return state.getClippedRefreshKey()"
        lines += "            }"
        lines += ""
        lines += "            override val jumpingSupported: Boolean"
        lines += "                get() = true"
        lines += "        }"
    }

    private fun renderNoCountFunction(
        lines: MutableList<String>,
        function: NoCountDataSourceFunctionModel,
    ) {
        val itemType = function.returnType.dataSourceItemType()
        lines += "    fun ${function.name}("
        function.parameters.forEach { parameter ->
            lines += "        ${parameter.name}: ${parameter.type},"
        }
        lines += "    ): ${function.returnType} ="
        lines += "        object : DataSource.Factory<Int, $itemType>() {"
        lines += "            override fun create(): DataSource<Int, $itemType> {"
        renderRoomQueryAcquire(lines, "_statement", function.sql, function.bindParameters.size, "                ")
        function.bindParameters.forEachIndexed { index, bindParameter ->
            val argIndex = index + 1
            if (index == 0) {
                lines += "                var _argIndex = $argIndex"
            } else {
                lines += "                _argIndex = $argIndex"
            }
            renderBind(lines, function, bindParameter, "                ")
        }
        lines += "                return object : NoCountLimitOffsetDataSource<$itemType>("
        lines += "                    ${function.databaseParameter},"
        lines += "                    _statement,"
        lines += "                    ${function.countParameter},"
        function.tables.forEach { table ->
            lines += "                    \"$table\","
        }
        lines += "                ) {"
        lines += "                    override fun convertRows(cursor: Cursor?): List<$itemType> ="
        lines += "                        ${function.converterName}(cursor)"
        lines += "                }"
        lines += "            }"
        lines += "        }"
    }

    private fun renderRoomQueryAcquire(
        lines: MutableList<String>,
        variableName: String,
        sql: String,
        argCount: Int,
        indent: String,
    ) {
        lines += "${indent}val $variableName = RoomQuery.acquire("
        renderSqlLiteral(lines, sql, "$indent    ")
        lines += "$indent    $argCount,"
        lines += "$indent)"
    }

    private fun renderSqlLiteral(
        lines: MutableList<String>,
        sql: String,
        indent: String,
    ) {
        lines += "$indent\"\"\""
        sql.lineSequence().forEach { sqlLine ->
            lines += "$indent$sqlLine"
        }
        lines += "$indent\"\"\".trimIndent(),"
    }

    private fun renderBind(
        lines: MutableList<String>,
        function: QueryFunctionModel,
        bindParameter: String,
    ) {
        val parameter =
            function.parameters.firstOrNull { it.name == bindParameter }
                ?: error("Missing parameter $bindParameter in ${function.name}")

        when (parameter.type.removeSuffix("?")) {
            "String" -> renderNullableBind(lines, parameter, "bindString", "        ")
            "Int", "Long" -> renderNullableLongBind(lines, parameter)
            else -> error("Unsupported bind type ${parameter.type} for ${parameter.name}")
        }
    }

    private fun renderBind(
        lines: MutableList<String>,
        function: NoCountDataSourceFunctionModel,
        bindParameter: String,
        indent: String,
    ) {
        val parameter =
            function.parameters.firstOrNull { it.name == bindParameter }
                ?: error("Missing parameter $bindParameter in ${function.name}")

        when (parameter.type.removeSuffix("?")) {
            "String" -> renderNullableBind(lines, parameter, "bindString", indent)
            "Int", "Long" -> renderNullableLongBind(lines, parameter, indent)
            else -> error("Unsupported bind type ${parameter.type} for ${parameter.name}")
        }
    }

    private fun renderNullableBind(
        lines: MutableList<String>,
        parameter: QueryParameterModel,
        bindMethod: String,
        indent: String,
    ) {
        if (parameter.type.endsWith("?")) {
            lines += "${indent}if (${parameter.name} == null) {"
            lines += "$indent    _statement.bindNull(_argIndex)"
            lines += "$indent} else {"
            lines += "$indent    _statement.$bindMethod(_argIndex, ${parameter.name})"
            lines += "$indent}"
        } else {
            lines += "${indent}_statement.$bindMethod(_argIndex, ${parameter.name})"
        }
    }

    private fun renderNullableLongBind(
        lines: MutableList<String>,
        parameter: QueryParameterModel,
        indent: String = "        ",
    ) {
        if (parameter.type.endsWith("?")) {
            lines += "${indent}if (${parameter.name} == null) {"
            lines += "$indent    _statement.bindNull(_argIndex)"
            lines += "$indent} else {"
            lines += "$indent    _statement.bindLong(_argIndex, ${parameter.name}.toLong())"
            lines += "$indent}"
        } else {
            lines += "${indent}_statement.bindLong(_argIndex, ${parameter.name}.toLong())"
        }
    }

    private fun QueryProviderModel.limitOffsetImports(): List<String> =
        if (limitOffsetFunctions.isEmpty()) {
            emptyList()
        } else {
            listOf(
                "android.database.Cursor",
                "one.mixin.android.db.datasource.MixinLimitOffsetDataSource",
            )
        }

    private fun QueryProviderModel.noCountImports(): List<String> =
        if (noCountFunctions.isEmpty()) {
            emptyList()
        } else {
            listOf(
                "android.database.Cursor",
                "one.mixin.android.db.datasource.NoCountLimitOffsetDataSource",
            )
        }

    private fun QueryProviderModel.roomQueryImports(): List<String> =
        if (functions.isEmpty() && limitOffsetFunctions.isEmpty() && noCountFunctions.isEmpty() && pagingSourceFunctions.isEmpty()) {
            emptyList()
        } else {
            listOf("one.mixin.android.db.datasource.RoomQuery")
        }

    private fun QueryProviderModel.queryExtensionImports(): List<String> =
        if (limitOffsetFunctions.isEmpty() && noCountFunctions.isEmpty() && rawCursorFunctions.isEmpty() && pagingSourceFunctions.isEmpty()) {
            emptyList()
        } else {
            listOf("one.mixin.android.db.datasource.query")
        }

    private fun QueryProviderModel.roomDatabaseCompatImports(): List<String> =
        if (functions.isEmpty() && pagingSourceFunctions.isEmpty()) {
            emptyList()
        } else {
            listOf("one.mixin.android.db.datasource.RoomDatabaseCompat")
        }

    private fun QueryProviderModel.coroutinesImports(): List<String> =
        if (functions.isEmpty() && pagingSourceFunctions.isEmpty()) {
            emptyList()
        } else {
            listOf(
                "kotlinx.coroutines.withContext",
            )
        }

    private fun QueryProviderModel.pagingSourceImports(): List<String> =
        if (pagingSourceFunctions.isEmpty()) {
            emptyList()
        } else {
            listOf(
                "androidx.paging.PagingState",
                "androidx.room3.paging.util.INITIAL_ITEM_COUNT",
                "androidx.room3.paging.util.getClippedRefreshKey",
                "androidx.room3.paging.util.getLimit",
                "androidx.room3.paging.util.getOffset",
                "androidx.room3.withReadTransaction",
                "one.mixin.android.db.datasource.RoomDatabaseCompat",
                "java.util.concurrent.atomic.AtomicInteger",
            )
        }

    private fun String.dataSourceItemType(): String =
        substringAfterLast(",").removeSuffix(">").trim()

    private fun String.renderSqlTemplate(parameterNames: List<String>): String {
        var rendered = this
        parameterNames.forEach { parameterName ->
            rendered = rendered.replace("{{$parameterName}}", "\$$parameterName")
        }
        return rendered
    }

    private fun RawCursorQueryFunctionModel.converterImport(): List<String> =
        if (converterName.contains(".")) listOf(converterName) else emptyList()

    private fun RawCursorQueryFunctionModel.converterCallName(): String =
        converterName.substringAfterLast(".")

    private fun PagingSourceQueryFunctionModel.converterImport(): List<String> =
        if (converterName.contains(".")) listOf(converterName) else emptyList()

    private fun RawCursorQueryFunctionModel.rawBindExpression(bindParameter: String): String {
        val parameter =
            parameters.firstOrNull { it.name == bindParameter }
                ?: error("Missing parameter $bindParameter in $name")
        return when (parameter.type.removeSuffix("?")) {
            "String" -> parameter.name
            "Int", "Long" -> parameter.name
            else -> error("Unsupported raw bind type ${parameter.type} for ${parameter.name}")
        }
    }
}
