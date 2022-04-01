package one.mixin.android.crypto

import org.whispersystems.libsignal.logging.SignalProtocolLogger
import timber.log.Timber

class MixinSignalProtocolLogger : SignalProtocolLogger {
    override fun log(priority: Int, tag: String, message: String) {
        if (priority >= SignalProtocolLogger.ERROR) {
            Timber.e(message)
        }
    }
}
