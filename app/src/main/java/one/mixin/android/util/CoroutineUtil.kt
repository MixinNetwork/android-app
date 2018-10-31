package one.mixin.android.util

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import java.util.concurrent.Executors

val SINGLE_DB_THREAD by lazy {
    Executors.newSingleThreadExecutor().asCoroutineDispatcher()
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