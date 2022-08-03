package one.mixin.android.util

import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

val SINGLE_DB_THREAD by lazy {
    Executors.newSingleThreadExecutor().asCoroutineDispatcher()
}

val SINGLE_THREAD by lazy {
    Executors.newSingleThreadExecutor().asCoroutineDispatcher()
}
