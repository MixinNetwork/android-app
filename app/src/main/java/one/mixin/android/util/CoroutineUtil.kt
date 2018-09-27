package one.mixin.android.util

import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.isActive
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.selects.select
import one.mixin.android.Constants.SINGLE_DB

val SINGLE_DB_THREAD by lazy {
    newSingleThreadContext(SINGLE_DB)
}

fun <T : Any?> onlyLast(input: ReceiveChannel<Deferred<T>>) = GlobalScope.produce {
    var current = input.receive()
    while (isActive) {
        val next = select<Deferred<T>?> {
            input.onReceiveOrNull { update ->
                update
            }
            current.onAwait { value ->
                send(value)
                input.receiveOrNull()
            }
        }
        if (next == null) {
            break
        } else {
            current = next
        }
    }
}