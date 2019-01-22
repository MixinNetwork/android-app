package one.mixin.android.ui.device

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.UrlQuerySanitizer
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import com.google.zxing.integration.android.IntentIntegrator
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_device.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.api.request.ProvisioningRequest
import one.mixin.android.api.service.ProvisioningService
import one.mixin.android.crypto.Base64
import one.mixin.android.crypto.IdentityKeyUtil
import one.mixin.android.crypto.ProfileKeyUtil
import one.mixin.android.crypto.ProvisionMessage
import one.mixin.android.crypto.ProvisioningCipher
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.AvatarActivity.Companion.ARGS_URL
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.qr.CaptureFragment
import one.mixin.android.util.Session
import one.mixin.android.util.UnescapeIgnorePlusUrlQuerySanitizer
import one.mixin.android.widget.BottomSheet
import org.jetbrains.anko.textColor
import org.whispersystems.libsignal.ecc.Curve
import java.util.Collections
import javax.inject.Inject

class DeviceFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "DeviceFragment"

        fun newInstance(url: String? = null) = DeviceFragment().withArgs {
            if (url != null) {
                putString(ARGS_URL, url)
            }
        }
    }

    @Inject
    lateinit var provisioningService: ProvisioningService

    private var disposable: Disposable? = null

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_device, null)
        contentView.ph.updateLayoutParams<ViewGroup.LayoutParams> {
            height = requireContext().statusBarHeight()
        }
        (dialog as BottomSheet).apply {
            fullScreen = true
            setCustomView(contentView)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.title_view.left_ib.setOnClickListener { dismiss() }
        contentView.auth_tv.setOnClickListener {
            val intentIntegrator = IntentIntegrator(activity)
            intentIntegrator.captureActivity = CaptureActivity::class.java
            intentIntegrator.setBeepEnabled(false)
            val intent = intentIntegrator.createScanIntent().putExtra(CaptureFragment.ARGS_FOR_ADDRESS, true)
            startActivityForResult(intent, IntentIntegrator.REQUEST_CODE)
            activity?.overridePendingTransition(R.anim.slide_in_bottom, 0)
        }

        val url = arguments!!.getString(ARGS_URL)
        if (url != null) {
            processUrl(url)
        } else {
            checkSession()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == IntentIntegrator.REQUEST_CODE && resultCode == CaptureFragment.RESULT_CODE) {
            val url = data?.getStringExtra(CaptureFragment.ARGS_ADDRESS_RESULT)
            url?.let {
                processUrl(it)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
    }

    private fun checkSession() {
        val accountId = Session.getAccountId() ?: return

        GlobalScope.launch {
            val response = bottomViewModel.getSessions(Collections.singletonList(accountId)).execute().body()
            if (response != null && response.isSuccess) {
                response.data?.let { list ->
                    if (list.size > 1) {
                        withContext(Dispatchers.Main) {
                            updateUI(true)
                        }
                    }
                }
            }
        }
    }

    private fun updateUI(loginSuccess: Boolean) {
        if (loginSuccess) {
            contentView.auth_tv.text = getString(R.string.setting_logout_desktop)
            contentView.desc_tv.text = getString(R.string.setting_desktop_signed)
            contentView.auth_tv.textColor = R.color.colorDarkBlue
            contentView.logo_iv.setImageResource(R.drawable.ic_desktop_online)
        } else {
            contentView.auth_tv.text = getString(R.string.setting_scan_qr_code)
            contentView.desc_tv.text = ""
            contentView.auth_tv.textColor = android.R.color.black
            contentView.logo_iv.setImageResource(R.drawable.ic_desktop_offline)
            context?.toast(R.string.setting_desktop_sigin_failed)
        }
    }

    private fun processUrl(url: String) {
        GlobalScope.launch {
            val provisioningCode = provisioningService.provisionCode().await()
            if (provisioningCode.isSuccess) {
                val success = encryptKey(requireContext(), url, provisioningCode.data!!.code)
                withContext(Dispatchers.Main) {
                    updateUI(success)
                }
            } else {
                withContext(Dispatchers.Main) {
                    context?.toast(R.string.setting_desktop_sigin_failed)
                }
            }
        }
    }

    private val sanitizer = UnescapeIgnorePlusUrlQuerySanitizer().apply {
        allowUnregisteredParamaters = true
        unregisteredParameterValueSanitizer = UrlQuerySanitizer.IllegalCharacterValueSanitizer(
            UrlQuerySanitizer.IllegalCharacterValueSanitizer.ALL_OK)
    }

    private suspend fun encryptKey(
        ctx: Context,
        url: String,
        verificationCode: String
    ): Boolean {
        val account = Session.getAccount() ?: return false
        val uri = Uri.parse(url)
        if (uri.scheme != "mixin") {
            return false
        }
        val ephemeralId = uri.getQueryParameter("uuid") ?: return false
        sanitizer.parseUrl(url)
        val publicKeyEncoded = sanitizer.getValue("pub_key")

        if (TextUtils.isEmpty(ephemeralId) || TextUtils.isEmpty(publicKeyEncoded)) {
            return false
        }

        val publicKey = Curve.decodePoint(Base64.decode(publicKeyEncoded), 0)
        val identityKeyPair = IdentityKeyUtil.getIdentityKeyPair(ctx)
        val profileKey = ProfileKeyUtil.getProfileKey(ctx)
        val cipher = ProvisioningCipher(publicKey)
        val message = ProvisionMessage(identityKeyPair.publicKey.serialize(), identityKeyPair.privateKey.serialize(), account.userId, account.session_id, verificationCode, profileKey)
        val cipherText = cipher.encrypt(message)
        val encoded = Base64.encodeBytes(cipherText)
        val response = provisioningService.updateProvisioning(ephemeralId, ProvisioningRequest(encoded)).await()
        return response.isSuccess
    }
}
