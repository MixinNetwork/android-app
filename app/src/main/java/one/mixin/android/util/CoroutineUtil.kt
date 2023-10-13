package one.mixin.android.util

import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.time.Duration
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

val SINGLE_DB_EXECUTOR: Executor =
    Executors.newSingleThreadExecutor { r -> Thread(r, "SINGLE_DB_EXECUTOR") }

val SINGLE_DB_THREAD by lazy {
    SINGLE_DB_EXECUTOR.asCoroutineDispatcher()
}

val PENDING_DB_THREAD by lazy {
    Executors.newSingleThreadExecutor().asCoroutineDispatcher()
}

val FLOOD_THREAD by lazy {
    Executors.newSingleThreadExecutor().asCoroutineDispatcher()
}

val SINGLE_THREAD by lazy {
    Executors.newSingleThreadExecutor().asCoroutineDispatcher()
}

val FTS_THREAD by lazy {
    Executors.newSingleThreadExecutor().asCoroutineDispatcher()
}

val SINGLE_SOCKET_THREAD by lazy {
    Executors.newSingleThreadExecutor { r -> Thread(r, "SINGLE_SOCKET_THREAD") }.asCoroutineDispatcher()
}

val SINGLE_FETCHER_THREAD by lazy {
    Executors.newSingleThreadExecutor().asCoroutineDispatcher()
}

fun tickerFlow(period: Duration, initialDelay: Duration = Duration.ZERO) = flow {
    delay(initialDelay)
    while (true) {
        emit(Unit)
        delay(period)
    }
}
