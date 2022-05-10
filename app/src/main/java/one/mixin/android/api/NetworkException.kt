package one.mixin.android.api

import java.io.IOException
import java.net.SocketTimeoutException

class NetworkException : IOException() {

    fun shouldRetry(): Boolean {
        return true
    }

    companion object {
        private var serialVersionUID: Long = 1L
    }
}

class WebSocketException : IOException() {
    fun shouldRetry(): Boolean {
        return true
    }
    companion object {
        private var serialVersionUID: Long = 1L
    }
}
class ExpiredTokenException : IOException() {
    fun shouldRetry(): Boolean {
        return true
    }
    companion object {
        private var serialVersionUID: Long = 1L
    }
}

class ChecksumException : IOException() {
    fun shouldRetry(): Boolean {
        return true
    }
    companion object {
        private var serialVersionUID: Long = 1L
    }
}

fun Throwable.worthRetrying(): Boolean {
    if (this is SocketTimeoutException) {
        return true
    }
    if (this is IOException) {
        return true
    }
    if (this is InterruptedException) {
        return true
    }
    return (this as? ServerErrorException)?.shouldRetry()
        ?: (this as? ExpiredTokenException)?.shouldRetry()
        ?: (
            (this as? ClientErrorException)?.shouldRetry()
                ?: (
                    (this as? NetworkException)?.shouldRetry()
                        ?: (
                            (this as? WebSocketException)?.shouldRetry()
                                ?: ((this as? LocalJobException)?.shouldRetry() ?: false)
                            )
                    )
            )
}