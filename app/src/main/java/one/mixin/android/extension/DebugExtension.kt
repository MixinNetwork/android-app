package one.mixin.android.extension

import timber.log.Timber
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun <T> measureTimeMillis(tag: String, block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val start = System.currentTimeMillis()
    val result = block()
    Timber.d("tag ${System.currentTimeMillis() - start}")
    return result
}
