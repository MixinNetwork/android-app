package one.mixin.android.api

import java.io.IOException

class ClientErrorException(val code: Int) : IOException() {

    fun shouldRetry(): Boolean {
        return false
    }
}
