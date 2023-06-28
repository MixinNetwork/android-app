package one.mixin.android.tip.wc

import android.annotation.SuppressLint
import com.google.firebase.messaging.RemoteMessage
import com.walletconnect.push.common.Push
import com.walletconnect.push.wallet.client.PushMessageService
import timber.log.Timber

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class WCFirebaseService : PushMessageService() {
    companion object {
        const val TAG = "WCFirebaseService"
    }

    override fun newToken(token: String) {
        Timber.d("$TAG newToken $token")
    }

    override fun onDefaultBehavior(message: RemoteMessage) {
        Timber.d("$TAG onDefaultBehavior $message")
    }

    override fun onError(throwable: Throwable, defaultMessage: RemoteMessage) {
        Timber.d("$TAG onError $defaultMessage, ${throwable.stackTraceToString()}")
    }

    override fun onMessage(message: Push.Model.Message, originalMessage: RemoteMessage) {
        Timber.d("$TAG onMessage $message")
    }

    override fun registeringFailed(token: String, throwable: Throwable) {
        Timber.d("$TAG registeringFailed $token, ${throwable.stackTraceToString()}")
    }
}
