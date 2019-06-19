package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import kotlinx.android.synthetic.main.fragment_verification_emergency.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.Constants.ARGS_USER
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.EmergencyPurpose
import one.mixin.android.api.request.EmergencyRequest
import one.mixin.android.crypto.Base64
import one.mixin.android.crypto.CryptoPreference
import one.mixin.android.crypto.SignalProtocol
import one.mixin.android.crypto.generateRSAKeyPair
import one.mixin.android.crypto.getPublicKey
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.enqueueOneTimeNetworkWorkRequest
import one.mixin.android.extension.putInt
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.PinCodeFragment
import one.mixin.android.ui.landing.LandingActivity.Companion.ARGS_PIN
import one.mixin.android.util.Session
import one.mixin.android.util.encryptPin
import one.mixin.android.vo.User
import one.mixin.android.worker.RefreshAccountWorker
import java.security.KeyPair

class VerificationEmergencyFragment : PinCodeFragment<EmergencyViewModel>() {
    companion object {
        const val TAG = "VerificationEmergencyFragment"
        const val ARGS_VERIFICATION_ID = "args_verification_id"
        const val ARGS_FROM = "args_from"
        const val ARGS_IDENTITY_NUMBER = "args_identity_number"

        const val FROM_CONTACT = 0
        const val FROM_SESSION = 1

        fun newInstance(
            user: User? = null,
            pin: String? = null,
            verificationId: String? = null,
            from: Int,
            userIdentityNumber: String? = null
        ) = VerificationEmergencyFragment().withArgs {
            putParcelable(ARGS_USER, user)
            putString(ARGS_PIN, pin)
            putString(ARGS_VERIFICATION_ID, verificationId)
            putInt(ARGS_FROM, from)
            putString(ARGS_IDENTITY_NUMBER, userIdentityNumber)
        }
    }

    private val user: User? by lazy { arguments!!.getParcelable<User>(ARGS_USER) }
    private val pin: String? by lazy { arguments!!.getString(ARGS_PIN) }
    private val verificationId by lazy { arguments!!.getString(ARGS_VERIFICATION_ID) }
    private val from by lazy { arguments!!.getInt(ARGS_FROM) }
    private val userIdentityNumber: String? by lazy { arguments!!.getString(ARGS_IDENTITY_NUMBER) }

    override fun getModelClass() = EmergencyViewModel::class.java

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_verification_emergency, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pin_verification_title_tv.text =
            getString(R.string.setting_emergency_send_code, user?.identityNumber ?: userIdentityNumber)
    }

    override fun clickNextFab() {
        val sessionKey = generateRSAKeyPair()
        if (from == FROM_CONTACT) {
            createVerify(sessionKey)
        } else {
            loginVerify(sessionKey)
        }
    }

    override fun insertUser(u: User) {
        viewModel.upsertUser(u)
    }

    private fun createVerify(sessionKey: KeyPair) = lifecycleScope.launch {
        showLoading()
        handleMixinResponse(
            invokeNetwork = { viewModel.createVerifyEmergency(verificationId, buildEmergencyRequest(sessionKey)) },
            switchContext = Dispatchers.IO,
            successBlock = { _ ->
                AlertDialog.Builder(requireContext())
                    .setTitle(getString(
                        if (Session.hasEmergencyContact())
                            R.string.setting_emergency_change_success
                        else
                            R.string.setting_emergency_create_success))
                    .setPositiveButton(R.string.group_ok) { dialog, _ ->
                        requireFragmentManager().popBackStackImmediate()
                        requireFragmentManager().popBackStackImmediate()

                        WorkManager.getInstance(requireContext()).enqueueOneTimeNetworkWorkRequest<RefreshAccountWorker>()
                        dialog.dismiss()
                    }
                    .show()
            },
            doAfterNetworkSuccess = { hideLoading() },
            defaultErrorHandle = {
                handleFailure(it)
            },
            defaultExceptionHandle = {
                handleError(it)
            }
        )
    }

    private fun loginVerify(sessionKey: KeyPair) = lifecycleScope.launch {
        showLoading()
        SignalProtocol.initSignal(requireContext().applicationContext)
        handleMixinResponse(
            invokeNetwork = { viewModel.loginVerifyEmergency(verificationId, buildEmergencyRequest(sessionKey)) },
            switchContext = Dispatchers.IO,
            successBlock = { response ->
                defaultSharedPreferences.putInt(PREF_LOGIN_FROM, FROM_EMERGENCY)
                handleAccount(response, sessionKey)
            },
            doAfterNetworkSuccess = { hideLoading() },
            defaultErrorHandle = {
                handleFailure(it)
            },
            defaultExceptionHandle = {
                handleError(it)
            }
        )
    }

    private fun buildEmergencyRequest(sessionKey: KeyPair): EmergencyRequest {
        val registrationId = CryptoPreference.getLocalRegistrationId(requireContext())
        val sessionSecret = Base64.encodeBytes(sessionKey.getPublicKey())
        val purpose = if (from == FROM_CONTACT) {
            EmergencyPurpose.CONTACT.name
        } else {
            EmergencyPurpose.SESSION.name
        }
        return EmergencyRequest(
            user?.phone,
            user?.identityNumber ?: userIdentityNumber,
            Session.getPinToken()?.let { encryptPin(it, pin)!! },
            pin_verification_view.code(),
            purpose,
            sessionSecret = sessionSecret,
            registrationId = registrationId
        )
    }
}