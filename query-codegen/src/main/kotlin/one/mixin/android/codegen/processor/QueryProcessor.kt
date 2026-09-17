package one.mixin.android.codegen.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.symbol.Variance
import com.google.devtools.ksp.validate
import one.mixin.android.codegen.annotation.GeneratedQuery
import one.mixin.android.codegen.annotation.GeneratedQueryProvider
import one.mixin.android.codegen.annotation.GeneratedLimitOffsetPagingSourceQuery
import one.mixin.android.codegen.annotation.GeneratedNoCountPagingSourceQuery
import one.mixin.android.codegen.annotation.GeneratedPagingSourceQuery
import one.mixin.android.codegen.annotation.GeneratedRawCursorQuery
import one.mixin.android.codegen.annotation.GeneratedRoomRawQuery

class QueryProcessor(
    environment: SymbolProcessorEnvironment,
) : SymbolProcessor {
    private val codeGenerator: CodeGenerator = environment.codeGenerator
    private val logger: KSPLogger = environment.logger
    private val renderer = QueryFileRenderer()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols =
            resolver
                .getSymbolsWithAnnotation(GeneratedQueryProvider::class.qualifiedName.orEmpty())
                .filterIsInstance<KSClassDeclaration>()
                .toList()
        val invalid = symbols.filterNot { it.validate() }
        symbols.filter { it.validate() }.forEach(::generateProvider)
        return invalid
    }

    private fun generateProvider(provider: KSClassDeclaration) {
        val allFunctions = provider.getAllFunctions().toList()
        val queryFunctions = allFunctions.filter { it.hasAnnotation<GeneratedQuery>() }
        val limitOffsetFunctions = allFunctions.filter { it.hasAnnotation<GeneratedLimitOffsetPagingSourceQuery>() }
        val noCountFunctions = allFunctions.filter { it.hasAnnotation<GeneratedNoCountPagingSourceQuery>() }
        val rawCursorFunctions = allFunctions.filter { it.hasAnnotation<GeneratedRawCursorQuery>() }
        val simpleQueryFunctions = allFunctions.filter { it.hasAnnotation<GeneratedRoomRawQuery>() }
        val pagingSourceFunctions = allFunctions.filter { it.hasAnnotation<GeneratedPagingSourceQuery>() }
        if (queryFunctions.isEmpty() && limitOffsetFunctions.isEmpty() && noCountFunctions.isEmpty() && rawCursorFunctions.isEmpty() && simpleQueryFunctions.isEmpty() && pagingSourceFunctions.isEmpty()) return

        val providerAnnotation = provider.annotation<GeneratedQueryProvider>()
        val generatedName =
            providerAnnotation.stringArgument("generatedName")
                .takeUnless { it.isBlank() }
                ?: "${provider.simpleName.asString()}Generated"
        val model =
            QueryProviderModel(
                packageName = provider.packageName.asString(),
                generatedName = generatedName,
                functions = queryFunctions.map(::functionModel),
                limitOffsetFunctions = limitOffsetFunctions.map(::limitOffsetFunctionModel),
                noCountFunctions = noCountFunctions.map(::noCountFunctionModel),
                rawCursorFunctions = rawCursorFunctions.map(::rawCursorFunctionModel),
                simpleQueryFunctions = simpleQueryFunctions.map(::simpleQueryFunctionModel),
                pagingSourceFunctions = pagingSourceFunctions.map(::pagingSourceFunctionModel),
            )
        val files = (listOfNotNull(provider.containingFile) + (queryFunctions + limitOffsetFunctions + noCountFunctions + rawCursorFunctions + simpleQueryFunctions + pagingSourceFunctions).mapNotNull { it.containingFile }).distinct()
        codeGenerator
            .createNewFile(
                dependencies = Dependencies(aggregating = false, sources = files.toTypedArray()),
                packageName = model.packageName,
                fileName = model.generatedName,
            ).use { output ->
                output.write(renderer.render(model).toByteArray())
            }
    }

    private fun functionModel(function: KSFunctionDeclaration): QueryFunctionModel {
        val annotation = function.annotation<GeneratedQuery>()
        val importCollector = TypeImportCollector()
        val parameters = function.parameters(importCollector)
        val returnType = function.returnType?.resolve()
        if (returnType == null) {
            logger.error("GeneratedQuery functions must declare a return type", function)
        }
        return QueryFunctionModel(
            name = function.simpleName.asString(),
            returnType = returnType?.let { importCollector.render(it) } ?: "Unit",
            returnTypeImports = importCollector.imports,
            parameters = parameters,
            parameterImports = emptyList(),
            sql = annotation.stringArgument("sql"),
            bindParameters = annotation.stringListArgument("binds"),
            databaseParameter = annotation.stringArgument("database"),
            cancellationSignalParameter = annotation.stringArgument("cancellationSignal"),
            callableName = annotation.stringArgument("callable"),
        )
    }

    private fun limitOffsetFunctionModel(function: KSFunctionDeclaration): LimitOffsetPagingSourceFunctionModel {
        val annotation = function.annotation<GeneratedLimitOffsetPagingSourceQuery>()
        val importCollector = TypeImportCollector()
        val parameters = function.parameters(importCollector)
        val returnType = function.returnType?.resolve()
        if (returnType == null) {
            logger.error("GeneratedLimitOffsetPagingSourceQuery functions must declare a return type", function)
        }
        return LimitOffsetPagingSourceFunctionModel(
            name = function.simpleName.asString(),
            returnType = returnType?.let { importCollector.render(it) } ?: "Unit",
            returnTypeImports = importCollector.imports,
            parameters = parameters,
            parameterImports = emptyList(),
            countSql = annotation.stringArgument("countSql"),
            offsetSql = annotation.stringArgument("offsetSql"),
            querySql = annotation.stringArgument("querySql"),
            tables = annotation.stringListArgument("tables"),
            databaseParameter = annotation.stringArgument("database"),
            converterName = annotation.stringArgument("converter"),
        )
    }

    private fun noCountFunctionModel(function: KSFunctionDeclaration): NoCountPagingSourceFunctionModel {
        val annotation = function.annotation<GeneratedNoCountPagingSourceQuery>()
        val importCollector = TypeImportCollector()
        val parameters = function.parameters(importCollector)
        val returnType = function.returnType?.resolve()
        if (returnType == null) {
            logger.error("GeneratedNoCountPagingSourceQuery functions must declare a return type", function)
        }
        return NoCountPagingSourceFunctionModel(
            name = function.simpleName.asString(),
            returnType = returnType?.let { importCollector.render(it) } ?: "Unit",
            returnTypeImports = importCollector.imports,
            parameters = parameters,
            parameterImports = emptyList(),
            sql = annotation.stringArgument("sql"),
            bindParameters = annotation.stringListArgument("binds"),
            countParameter = annotation.stringArgument("count"),
            tables = annotation.stringListArgument("tables"),
            databaseParameter = annotation.stringArgument("database"),
            converterName = annotation.stringArgument("converter"),
        )
    }

    private fun rawCursorFunctionModel(function: KSFunctionDeclaration): RawCursorQueryFunctionModel {
        val annotation = function.annotation<GeneratedRawCursorQuery>()
        val importCollector = TypeImportCollector()
        val parameters = function.parameters(importCollector)
        val returnType = function.returnType?.resolve()
        if (returnType == null) {
            logger.error("GeneratedRawCursorQuery functions must declare a return type", function)
        }
        return RawCursorQueryFunctionModel(
            name = function.simpleName.asString(),
            returnType = returnType?.let { importCollector.render(it) } ?: "Unit",
            returnTypeImports = importCollector.imports,
            parameters = parameters,
            parameterImports = emptyList(),
            sql = annotation.stringArgument("sql"),
            bindParameters = annotation.stringListArgument("binds"),
            databaseParameter = annotation.stringArgument("database"),
            converterName = annotation.stringArgument("converter"),
        )
    }

    private fun simpleQueryFunctionModel(function: KSFunctionDeclaration): RoomRawQueryFunctionModel {
        val annotation = function.annotation<GeneratedRoomRawQuery>()
        val importCollector = TypeImportCollector()
        val parameters = function.parameters(importCollector)
        val returnType = function.returnType?.resolve()
        if (returnType == null) {
            logger.error("GeneratedRoomRawQuery functions must declare a return type", function)
        }
        return RoomRawQueryFunctionModel(
            name = function.simpleName.asString(),
            returnType = returnType?.let { importCollector.render(it) } ?: "Unit",
            returnTypeImports = importCollector.imports,
            parameters = parameters,
            parameterImports = emptyList(),
            sql = annotation.stringArgument("sql"),
        )
    }

    private fun pagingSourceFunctionModel(function: KSFunctionDeclaration): PagingSourceQueryFunctionModel {
        val annotation = function.annotation<GeneratedPagingSourceQuery>()
        val importCollector = TypeImportCollector()
        val parameters = function.parameters(importCollector)
        val returnType = function.returnType?.resolve()
        if (returnType == null) {
            logger.error("GeneratedPagingSourceQuery functions must declare a return type", function)
        }
        return PagingSourceQueryFunctionModel(
            name = function.simpleName.asString(),
            returnType = returnType?.let { importCollector.render(it) } ?: "Unit",
            returnTypeImports = importCollector.imports,
            parameters = parameters,
            parameterImports = emptyList(),
            countSql = annotation.stringArgument("countSql"),
            offsetSql = annotation.stringArgument("offsetSql"),
            querySql = annotation.stringArgument("querySql"),
            tables = annotation.stringListArgument("tables"),
            databaseParameter = annotation.stringArgument("database"),
            converterName = annotation.stringArgument("converter"),
        )
    }

    private fun KSFunctionDeclaration.parameters(importCollector: TypeImportCollector): List<QueryParameterModel> =
        parameters.map { parameter ->
            val name = parameter.name?.asString()
            if (name == null) {
                logger.error("Generated query parameters must be named", parameter)
                QueryParameterModel("", "Nothing")
            } else {
                QueryParameterModel(name, importCollector.render(parameter.type.resolve()))
            }
        }

    private inline fun <reified T : Annotation> KSAnnotated.hasAnnotation(): Boolean =
        annotations.any { it.shortName.asString() == T::class.simpleName }

    private inline fun <reified T : Annotation> KSAnnotated.annotation(): KSAnnotation =
        annotations.first { it.shortName.asString() == T::class.simpleName }
}

private class TypeImportCollector {
    private val mutableImports = linkedSetOf<String>()
    val imports: List<String>
        get() = mutableImports.toList()

    fun render(type: KSType): String {
        val declaration = type.declaration
        val (simpleName, importName) = declaration.names()
        if (importName.isNotEmpty() && !importName.startsWith("kotlin.")) {
            mutableImports += importName
        }
        val arguments =
            type.arguments
                .takeIf { it.isNotEmpty() }
                ?.joinToString(prefix = "<", postfix = ">") { renderArgument(it) }
                .orEmpty()
        val nullable = if (type.nullability == Nullability.NULLABLE) "?" else ""
        return "$simpleName$arguments$nullable"
    }

    private fun renderArgument(argument: KSTypeArgument): String {
        val type = argument.type?.resolve() ?: return "*"
        val rendered = render(type)
        return when (argument.variance) {
            Variance.COVARIANT -> "out $rendered"
            Variance.CONTRAVARIANT -> "in $rendered"
            Variance.STAR -> "*"
            else -> rendered
        }
    }
}

private fun KSDeclaration.names(): Pair<String, String> {
    val parent = parentDeclaration as? KSClassDeclaration
    return if (parent?.qualifiedName != null) {
        "${parent.simpleName.asString()}.${simpleName.asString()}" to parent.qualifiedName!!.asString()
    } else {
        simpleName.asString() to (qualifiedName?.asString().orEmpty())
    }
}

private fun KSAnnotation.stringArgument(name: String): String =
    arguments.firstValue(name) as? String ?: ""

private fun KSAnnotation.stringListArgument(name: String): List<String> {
    val value = arguments.firstValue(name)
    return when (value) {
        is List<*> -> value.filterIsInstance<String>()
        is Array<*> -> value.filterIsInstance<String>()
        else -> emptyList()
    }
}

private fun List<com.google.devtools.ksp.symbol.KSValueArgument>.firstValue(name: String): Any? =
    firstOrNull { it.name?.matches(name) == true }?.value

private fun KSName.matches(name: String): Boolean = asString() == name
