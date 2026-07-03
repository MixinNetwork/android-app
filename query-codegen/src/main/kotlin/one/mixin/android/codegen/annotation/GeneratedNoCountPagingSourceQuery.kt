package one.mixin.android.codegen.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class GeneratedNoCountPagingSourceQuery(
    val sql: String,
    val binds: Array<String>,
    val count: String,
    val tables: Array<String>,
    val converter: String,
    val database: String = "database",
)
