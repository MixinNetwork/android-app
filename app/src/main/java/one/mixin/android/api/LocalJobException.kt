package one.mixin.android.api

class LocalJobException : RuntimeException() {

    fun shouldRetry() = true
}
