package one.mixin.android.job

import android.util.Log
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.MixinApplication
import one.mixin.android.api.request.SignalKeyRequest
import one.mixin.android.crypto.IdentityKeyUtil
import one.mixin.android.crypto.PreKeyUtil
import java.util.UUID

class RefreshOneTimePreKeysJob : MixinJob(
    Params(PRIORITY_UI_HIGH).requireNetwork()
        .groupBy("refresh_pre_keys"),
    UUID.randomUUID().toString()
) {
    override fun cancel() {
    }

    private val TAG = RefreshOneTimePreKeysJob::class.java.simpleName

    override fun onRun() {
        checkSignalKey()
    }

    private fun checkSignalKey() {
        val response = signalKeyService.getSignalKeyCount().execute().body()
        if (response != null && response.isSuccess && response.data != null) {
            val availableKeyCount = response.data!!.preKeyCount
            if (availableKeyCount >= PREKEY_MINI_NUM) {
                return
            }
            refresh()
        }
    }

    private fun refresh() = runBlocking {
        val signalKeysRequest = generateKeys()
        Log.w(TAG, "Registering new pre keys...")
        val response = signalKeyService.pushSignalKeys(signalKeysRequest).await()
        if (response.isSuccess) {
        }
    }

    companion object {
        private const val serialVersionUID = 1L
        const val PREKEY_MINI_NUM = 500
        fun generateKeys(): SignalKeyRequest {
            val identityKeyPair = IdentityKeyUtil.getIdentityKeyPair(MixinApplication.appContext)
            val oneTimePreKeys = PreKeyUtil.generatePreKeys(MixinApplication.appContext)
            val signedPreKeyRecord = PreKeyUtil.generateSignedPreKey(
                MixinApplication.appContext,
                identityKeyPair, false
            )
            return SignalKeyRequest(identityKeyPair.publicKey, signedPreKeyRecord, oneTimePreKeys)
        }
    }
}
