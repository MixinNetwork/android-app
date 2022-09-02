package one.mixin.android.tip

import one.mixin.android.api.ResponseError

open class TipException : Exception {

    constructor(message: String) : super(message)

    constructor() : super()

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

class PinIncorrectException : TipException() {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

class TipNullException(message: String) : TipException(message) {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class TipNetworkException(val error: ResponseError) : TipException(error.description) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
