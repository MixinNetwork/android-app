package one.mixin.android.util

class JwtResult(var isDelay: Boolean, var serverTime: Long? = null, var requestTime: Long? = null, var currentTime: Long? = null) {
    override fun toString(): String {
        return "isDelay: $isDelay, serverTime: $serverTime, requestTime: $requestTime, currentTime: $currentTime"
    }
}