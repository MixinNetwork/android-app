package one.mixin.android.extension

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> =
    coroutineScope {
        map { async { f(it) } }.awaitAll()
    }

fun <T, K> mergeLocalAndRefreshed(
    localMatches: List<T>,
    refreshedMatches: List<T>,
    keySelector: (T) -> K,
): List<T> {
    val refreshedByKey = refreshedMatches.associateBy(keySelector)
    val localKeys = LinkedHashSet<K>()
    val merged = ArrayList<T>(localMatches.size + refreshedMatches.size)

    localMatches.forEach { local ->
        val key = keySelector(local)
        localKeys += key
        merged += refreshedByKey[key] ?: local
    }
    refreshedMatches.forEach { refreshed ->
        if (keySelector(refreshed) !in localKeys) {
            merged += refreshed
        }
    }

    return merged
}
