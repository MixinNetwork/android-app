package one.mixin.android.api

import java.io.IOException

class DataErrorException : IOException() {
    fun shouldRetry(): Boolean {
        return true
    }

    companion object {
        private var serialVersionUID: Long = 1L
    }
}
