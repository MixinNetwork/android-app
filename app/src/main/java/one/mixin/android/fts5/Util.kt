package one.mixin.android.fts5

import java.util.concurrent.ThreadLocalRandom
import kotlin.streams.asSequence

val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
const val STRING_LENGTH = 10

fun randomStringByJavaRandom() = ThreadLocalRandom.current()
    .ints(STRING_LENGTH.toLong(), 0, charPool.size)
    .asSequence()
    .map(charPool::get)
    .joinToString("")
