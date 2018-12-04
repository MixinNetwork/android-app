package one.mixin.android.ui.device

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.fragment_device.*
import one.mixin.android.R
import one.mixin.android.crypto.*
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.qr.CaptureFragment
import one.mixin.android.util.Session
import org.jetbrains.anko.doAsync
import org.whispersystems.libsignal.ecc.Curve

class DeviceActivityFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_device, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        button.setOnClickListener{
            val intentIntegrator = IntentIntegrator(activity)
            intentIntegrator.captureActivity = CaptureActivity::class.java
            intentIntegrator.setBeepEnabled(false)
            val intent = intentIntegrator.createScanIntent().putExtra(CaptureFragment.ARGS_FOR_ADDRESS, true)
            startActivityForResult(intent, IntentIntegrator.REQUEST_CODE)
            activity?.overridePendingTransition(R.anim.slide_in_bottom, 0)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == IntentIntegrator.REQUEST_CODE && resultCode == CaptureFragment.RESULT_CODE) {
            val url = data?.getStringExtra(CaptureFragment.ARGS_ADDRESS_RESULT)
            Log.e("hello", url)
            doAsync {
                encryptKey(requireContext(), url)
            }
        }
    }

    private fun encryptKey(ctx: Context, url: String?) {
        val verificationCode = "hello"
        val uri = Uri.parse(url)
        val ephemeralId      = uri.getQueryParameter("uuid")
        val deviceKey = uri.getQueryParameter("pub_key")

        if (TextUtils.isEmpty(ephemeralId) || TextUtils.isEmpty(deviceKey)) {
            return
        }

        val publicKey = Curve.decodePoint(deviceKey.hexStringToByteArray(), 0)
        val identityKeyPair = IdentityKeyUtil.getIdentityKeyPair(ctx)
        val profileKey = ProfileKeyUtil.getProfileKey(ctx)
        val userId = Session.getAccountId()!!

        val cipher = ProvisioningCipher(publicKey)
        val message = ProvisionMessage(identityKeyPair.publicKey.serialize(), identityKeyPair.privateKey.serialize(), userId, verificationCode, profileKey)
        val ciphertext = cipher.encrypt(message)

        Log.e("hello", Base64.encodeBytes(ciphertext))
    }
}
