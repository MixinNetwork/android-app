package one.mixin.android.codegen.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class GeneratedQuery(
    val sql: String,
    val binds: Array<String>,
    val callable: String,
    val database: String = "db",
    val cancellationSignal: String = "cancellationSignal",
)
