package one.mixin.android.codegen.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class GeneratedRawCursorQuery(
    val sql: String,
    val binds: Array<String>,
    val converter: String,
    val database: String = "db",
)
