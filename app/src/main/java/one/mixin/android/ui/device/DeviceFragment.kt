package one.mixin.android.ui.device

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.UrlQuerySanitizer
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.zxing.integration.android.IntentIntegrator
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_device.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.api.request.ProvisioningRequest
import one.mixin.android.api.service.ProvisioningService
import one.mixin.android.crypto.*
import one.mixin.android.crypto.Base64
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.qr.CaptureFragment
import one.mixin.android.util.Session
import one.mixin.android.util.UnescapeIgnorePlusUrlQuerySanitizer
import org.jetbrains.anko.doAsync
import org.whispersystems.libsignal.ecc.Curve
import javax.inject.Inject

class DeviceFragment : BaseFragment() {
    companion object {
        const val TAG = "DeviceFragment"

        fun newInstance() = DeviceFragment()
    }

    @Inject
    lateinit var provisioningService: ProvisioningService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_device, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        auth_tv.setOnClickListener {
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
            url?.let {
                provisioningService.provisionCode().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({ provisioningCode ->
                    if (provisioningCode.isSuccess && provisioningCode != null) {
                        doAsync {
                            encryptKey(requireContext(), url, provisioningCode.data!!.code)
                        }
                    }
                }, {
                    context?.toast(R.string.setting_desktop_sigin_failed)
                })
            }
        }
    }

    private val sanitizer = UnescapeIgnorePlusUrlQuerySanitizer().apply {
        allowUnregisteredParamaters = true
        unregisteredParameterValueSanitizer = UrlQuerySanitizer.IllegalCharacterValueSanitizer(
                UrlQuerySanitizer.IllegalCharacterValueSanitizer.ALL_OK)
    }

    private fun encryptKey(ctx: Context, url: String, verificationCode: String) {
        val account = Session.getAccount() ?: return
        val uri = Uri.parse(url)
        if (uri.scheme != "mixin") {
            return
        }
        val ephemeralId = uri.getQueryParameter("uuid")
        sanitizer.parseUrl(url)
        val publicKeyEncoded = sanitizer.getValue("pub_key")

        if (TextUtils.isEmpty(ephemeralId) || TextUtils.isEmpty(publicKeyEncoded)) {
            return
        }

        val publicKey = Curve.decodePoint(Base64.decode(publicKeyEncoded), 0)
        val identityKeyPair = IdentityKeyUtil.getIdentityKeyPair(ctx)
        val profileKey = ProfileKeyUtil.getProfileKey(ctx)
        val cipher = ProvisioningCipher(publicKey)
        val message = ProvisionMessage(identityKeyPair.publicKey.serialize(), identityKeyPair.privateKey.serialize(), account.userId, account.session_id, verificationCode, profileKey)
        val ciphertext = cipher.encrypt(message)
        val encoded = Base64.encodeBytes(ciphertext)
        provisioningService.updateProvisioning(ephemeralId, ProvisioningRequest(encoded)).subscribe({

        }, {
            context?.toast(R.string.setting_desktop_sigin_failed)
        })
    }
}
