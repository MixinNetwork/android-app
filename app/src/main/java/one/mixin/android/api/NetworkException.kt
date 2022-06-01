package one.mixin.android.api

import java.io.IOException

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
