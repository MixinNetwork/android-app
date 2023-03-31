package one.mixin.android.ui.transfer

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import java.net.SocketException
import java.net.UnknownHostException

val transferExceptionHandler = CoroutineExceptionHandler { _, exception ->
    // todo
    when (exception) {
        is UnknownHostException -> {
        }

        is SocketException -> {
        }

        else -> {
        }
    }
    Timber.e(exception)
}
