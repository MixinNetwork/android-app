package one.mixin.android.job

import android.util.Log
import com.birbit.android.jobqueue.Params
import one.mixin.android.api.request.SignalKeyRequest
import one.mixin.android.crypto.IdentityKeyUtil
import one.mixin.android.crypto.PreKeyUtil

class RotateSignedPreKeyJob : BaseJob(Params(PRIORITY_UI_HIGH).requireNetwork().groupBy("rotate_signed_pre_key")) {
    companion object {
        private const val serialVersionUID = 1L
        const val ROTATE_SIGNED_PRE_KEY = "rotate_signed_pre_key"
    }

    private val TAG = RotateSignedPreKeyJob::class.java.simpleName

    override fun onRun() {
        Log.w(TAG, "Rotating signed pre key...")

        val identityKeyPair = IdentityKeyUtil.getIdentityKeyPair(applicationContext)
        val signedPreKeyRecord = PreKeyUtil.generateSignedPreKey(applicationContext, identityKeyPair, false)

        val response = signalKeyService
            .pushSignalKeys(SignalKeyRequest(ik = identityKeyPair.publicKey, spk = signedPreKeyRecord))
            .execute().body()
        if (response != null && response.isSuccess) {
            PreKeyUtil.setActiveSignedPreKeyId(applicationContext, signedPreKeyRecord.id)
        }
    }
}
