package one.mixin.android.util

import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher

val SINGLE_DB_THREAD by lazy {
    Executors.newSingleThreadExecutor().asCoroutineDispatcher()
}
