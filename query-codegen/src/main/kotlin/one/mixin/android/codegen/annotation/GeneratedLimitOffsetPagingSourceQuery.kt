package one.mixin.android.codegen.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class GeneratedLimitOffsetPagingSourceQuery(
    val countSql: String,
    val offsetSql: String,
    val querySql: String,
    val tables: Array<String>,
    val converter: String,
    val database: String = "database",
)
