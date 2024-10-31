package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants.DEVICE_ID
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountRequest
import one.mixin.android.api.request.VerificationPurpose
import one.mixin.android.crypto.CryptoPreference
import one.mixin.android.crypto.EdKeyPair
import one.mixin.android.crypto.SignalProtocol
import one.mixin.android.crypto.initFromSeedAndSign
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.clear
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getStringDeviceId
import one.mixin.android.extension.navTo
import one.mixin.android.extension.putString
import one.mixin.android.extension.toHex
import one.mixin.android.session.Session
import one.mixin.android.session.decryptPinToken
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.landing.components.MnemonicPhraseInput
import one.mixin.android.ui.landing.components.MnemonicState
import one.mixin.android.ui.landing.vo.MnemonicPhraseState
import one.mixin.android.util.database.clearDatabase
import one.mixin.android.util.database.clearJobsAndRawTransaction
import one.mixin.android.util.database.getLastUserId
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.toUser
import timber.log.Timber

@AndroidEntryPoint
class LandingMnemonicPhraseFragment : BaseFragment(R.layout.fragment_landing_mnemonic_phrase) {
    companion object {
        const val TAG: String = "MnemonicPhraseFragment"

        fun newInstance(
        ): LandingMnemonicPhraseFragment =
            LandingMnemonicPhraseFragment().apply {

            }
    }

    private val binding by viewBinding(FragmentComposeBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.compose.setContent {
            MnemonicPhraseInput(MnemonicState.Input, onComplete = {
                val list = ArrayList<String>()
                list.addAll(it)
                navTo(MnemonicPhraseFragment.newInstance(list), MnemonicPhraseFragment.TAG)
            }
            )
        }
    }
}