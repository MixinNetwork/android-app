package one.mixin.android.api

import java.io.IOException

class NetworkException : IOException() {

    fun shouldRetry(): Boolean {
        return true
    }
}

class WebSocketException : IOException() {
    fun shouldRetry(): Boolean {
        return true
    }
}

class ChecksumException : IOException() {
    fun shouldRetry(): Boolean {
        return true
    }
}
