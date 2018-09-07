package one.mixin.android.util

import kotlinx.coroutines.experimental.newSingleThreadContext
import one.mixin.android.Constants.SINGLE_DB

val SINGLE_DB_THREAD by lazy {
    newSingleThreadContext(SINGLE_DB)
}
