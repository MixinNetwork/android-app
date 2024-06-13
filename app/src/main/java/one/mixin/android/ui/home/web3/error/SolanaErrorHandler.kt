package one.mixin.android.ui.home.web3.error

import android.content.Context

class SolanaErrorHandler {
    private val handlers = mutableListOf<Handler>()

    fun addHandler(handler: Handler): SolanaErrorHandler {
        handlers.add(handler)
        return this
    }

    fun start(ctx: Context): String? {
        val chain = Handler.Chain(handlers, 0, ctx)
        return chain.process()
    }

    fun reset(): SolanaErrorHandler {
        handlers.clear()
        return this
    }
}

abstract class Handler(
    open val log: String,
) {
    abstract fun parse(chain: Chain): String?

    class Chain(
        private val handlers: List<Handler>,
        private val index: Int,
        val ctx: Context,
    ) {
        fun process(): String? {
            if (index >= handlers.size) return null

            val handler = handlers[index]
            val next = Chain(handlers, index + 1, ctx)
            return handler.parse(next)
        }
    }
}