package one.mixin.android.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executor
import java.util.concurrent.Executors

val SINGLE_DB_EXECUTORS: Executor by lazy{
 Executors.newSingleThreadExecutor()
}

val SINGLE_DB_THREAD by lazy {
    SINGLE_DB_EXECUTORS.asCoroutineDispatcher()
}

val SINGLE_THREAD by lazy {
    Executors.newSingleThreadExecutor().asCoroutineDispatcher()
}
