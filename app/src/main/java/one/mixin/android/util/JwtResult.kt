package one.mixin.android.util

class JwtResult(var isExpire: Boolean, var serverTime: Long? = null, var requestTime: Long? = null, var currentTime: Long? = null) {
    override fun toString(): String {
        return "isExpire: $isExpire, serverTime: $serverTime, requestTime: $requestTime, currentTime: $currentTime"
    }
}