package one.mixin.android.util

import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executor
import java.util.concurrent.Executors

val SINGLE_DB_EXECUTOR: Executor =
    Executors.newSingleThreadExecutor { r -> Thread(r, "SINGLE_DB_EXECUTOR") }

val SINGLE_DB_THREAD by lazy {
    SINGLE_DB_EXECUTOR.asCoroutineDispatcher()
}

val PENDING_DB_THREAD by lazy {
    Executors.newSingleThreadExecutor().asCoroutineDispatcher()
}

val SINGLE_THREAD by lazy {
    Executors.newSingleThreadExecutor().asCoroutineDispatcher()
}
