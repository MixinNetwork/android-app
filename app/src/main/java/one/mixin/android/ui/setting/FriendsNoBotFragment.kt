package one.mixin.android.ui.setting

import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.EmergencyPurpose
import one.mixin.android.api.request.EmergencyRequest
import one.mixin.android.api.response.VerificationResponse
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.navTo
import one.mixin.android.extension.withArgs
import one.mixin.android.session.Session
import one.mixin.android.session.encryptPin
import one.mixin.android.ui.common.friends.BaseFriendsFragment
import one.mixin.android.ui.common.friends.FriendsListener
import one.mixin.android.ui.landing.LandingActivity
import one.mixin.android.ui.setting.VerificationEmergencyFragment.Companion.FROM_CONTACT
import one.mixin.android.vo.User

@AndroidEntryPoint
class FriendsNoBotFragment : BaseFriendsFragment<FriendsNoBotViewHolder>(), FriendsListener {
    init {
        adapter = FriendsNoBotAdapter(userCallback).apply {
            listener = this@FriendsNoBotFragment
        }
    }

    companion object {
        const val TAG = "FriendsNoBotFragment"

        fun newInstance(pin: String) = FriendsNoBotFragment().withArgs {
            putString(LandingActivity.ARGS_PIN, pin)
        }
    }

    private val pin: String by lazy { requireArguments().getString(LandingActivity.ARGS_PIN)!! }

    private val viewModel by viewModels<EmergencyViewModel>()

    override fun getTitleResId() = R.string.setting_emergency_select_contact

    override suspend fun getFriends() = viewModel.findFriendsNotBot()

    override fun onItemClick(user: User) {
        alertDialogBuilder()
            .setTitle(getString(R.string.setting_emergency_set))
            .setMessage(getString(R.string.setting_emergency_set_message, user.identityNumber))
            .setNegativeButton(R.string.Change) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.Confirm) { dialog, _ ->
                binding.searchEt.hideKeyboard()
                requestCreateEmergency(user)
                dialog.dismiss()
            }
            .show()
    }

    private fun requestCreateEmergency(user: User) = lifecycleScope.launch {
        val dialog = indeterminateProgressDialog(
            message = getString(R.string.pb_dialog_message),
            title = getString(if (Session.hasEmergencyContact()) R.string.changing else R.string.Creating)
        )
        dialog.setCancelable(false)
        dialog.show()
        handleMixinResponse(
            invokeNetwork = { viewModel.createEmergency(buildEmergencyRequest(user)) },
            successBlock = { response ->
                navTo(
                    VerificationEmergencyFragment.newInstance(
                        user,
                        pin,
                        (response.data as VerificationResponse).id,
                        FROM_CONTACT
                    ),
                    VerificationEmergencyFragment.TAG
                )
            },
            exceptionBlock = {
                dialog.dismiss()
                return@handleMixinResponse false
            },
            doAfterNetworkSuccess = { dialog.dismiss() }
        )
    }

    private fun buildEmergencyRequest(user: User) = EmergencyRequest(
        identityNumber = user.identityNumber,
        pin = Session.getPinToken()?.let { encryptPin(it, pin)!! },
        purpose = EmergencyPurpose.CONTACT.name
    )
}
