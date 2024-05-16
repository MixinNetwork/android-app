package one.mixin.android.web3

class Web3Exception(val code: Int, message: String) : RuntimeException(message) {
    companion object {
        private const val serialVersionUID: Long = 1L
    }

    object ErrorCode {
        const val InvalidWeb3Token = 40001
    }
}
