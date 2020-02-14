package one.mixin.android.ui.setting

import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_friends.*
import kotlinx.coroutines.Dispatchers
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
import one.mixin.android.ui.common.friends.BaseFriendsFragment
import one.mixin.android.ui.common.friends.FriendsListener
import one.mixin.android.ui.landing.LandingActivity
import one.mixin.android.ui.setting.VerificationEmergencyFragment.Companion.FROM_CONTACT
import one.mixin.android.util.Session
import one.mixin.android.util.encryptPin
import one.mixin.android.vo.User

class FriendsNoBotFragment : BaseFriendsFragment<FriendsNoBotViewHolder, EmergencyViewModel>(), FriendsListener {
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

    private val pin: String by lazy { arguments!!.getString(LandingActivity.ARGS_PIN)!! }

    override fun getModelClass() = EmergencyViewModel::class.java

    override fun getTitleResId() = R.string.setting_emergency_select_contact

    override suspend fun getFriends() = viewModel.getFriendsNotBot()

    override fun onItemClick(user: User) {
        alertDialogBuilder()
            .setTitle(getString(R.string.setting_emergency_set))
            .setMessage(getString(R.string.setting_emergency_set_message, user.identityNumber))
            .setNegativeButton(R.string.change) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                search_et.hideKeyboard()
                requestCreateEmergency(user)
                dialog.dismiss()
            }
            .show()
    }

    private fun requestCreateEmergency(user: User) = lifecycleScope.launch {
        val dialog = indeterminateProgressDialog(message = getString(R.string.pb_dialog_message),
            title = getString(if (Session.hasEmergencyContact()) R.string.changing else R.string.group_creating))
        dialog.setCancelable(false)
        dialog.show()
        handleMixinResponse(
            invokeNetwork = { viewModel.createEmergency(buildEmergencyRequest(user)) },
            switchContext = Dispatchers.IO,
            successBlock = { response ->
                navTo(VerificationEmergencyFragment.newInstance(user, pin,
                    (response.data as VerificationResponse).id, FROM_CONTACT),
                    VerificationEmergencyFragment.TAG)
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
        purpose = EmergencyPurpose.CONTACT.name)
}
