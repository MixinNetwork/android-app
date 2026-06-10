package one.mixin.android.tip.exception

import one.mixin.android.api.ResponseError

data class TipNetworkException(val error: ResponseError) : TipException(error.description) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
