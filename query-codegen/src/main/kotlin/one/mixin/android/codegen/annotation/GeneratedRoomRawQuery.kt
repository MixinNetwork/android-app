package one.mixin.android.codegen.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class GeneratedRoomRawQuery(
    val sql: String,
)
