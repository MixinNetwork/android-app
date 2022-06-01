package one.mixin.android.api

class LocalJobException : RuntimeException() {

    fun shouldRetry() = true

    companion object {
        private var serialVersionUID: Long =1L
    }
}
