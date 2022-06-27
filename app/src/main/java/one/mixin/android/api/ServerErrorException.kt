package one.mixin.android.api

import java.io.IOException

class ServerErrorException(val code: Int) : IOException() {

    fun shouldRetry(): Boolean {
        return true
    }

    companion object {
        private var serialVersionUID: Long = 1L
    }
}
