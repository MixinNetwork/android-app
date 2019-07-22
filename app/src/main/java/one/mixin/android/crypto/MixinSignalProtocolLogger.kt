package one.mixin.android.crypto

import android.util.Log
import org.whispersystems.libsignal.logging.SignalProtocolLogger

class MixinSignalProtocolLogger : SignalProtocolLogger {
    override fun log(priority: Int, tag: String, message: String) {
        if (priority >= SignalProtocolLogger.ERROR) {
            Log.e(tag, message)
        }
    }
}
