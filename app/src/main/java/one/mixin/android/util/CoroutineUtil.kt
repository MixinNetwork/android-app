package one.mixin.android.util

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.isActive
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.selects.select
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