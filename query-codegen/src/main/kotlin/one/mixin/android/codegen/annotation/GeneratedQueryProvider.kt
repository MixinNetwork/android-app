package one.mixin.android.codegen.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GeneratedQueryProvider(
    val generatedName: String = "",
)
