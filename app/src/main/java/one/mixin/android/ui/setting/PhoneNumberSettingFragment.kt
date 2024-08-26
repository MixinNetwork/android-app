package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.databinding.FragmentSettingPhoneNumberBinding
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.SearchSource

@AndroidEntryPoint
class PhoneNumberSettingFragment : BaseFragment(R.layout.fragment_setting_phone_number) {
    companion object {
        const val TAG = "PhoneNumberSettingFragment"
        const val ACCEPT_SEARCH_KEY = "accept_search_key"

        fun newInstance() = PhoneNumberSettingFragment()
    }

    private val viewModel by viewModels<SettingConversationViewModel>()
    private val binding by viewBinding(FragmentSettingPhoneNumberBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        viewModel.initSearchPreference(requireContext())
            .observe(viewLifecycleOwner) {
                it?.let {
                    render(it)
                }
            }
    }

    private fun render(pref: String) {
        binding.apply {
            everybodyIv.isVisible = pref == SearchSource.EVERYBODY.name
            myContactsIv.isVisible = pref == SearchSource.CONTACTS.name
            nobodyIv.isVisible = pref == SearchSource.NOBODY.name
            everybodyPb.isVisible = false
            myContactsPb.isVisible = false
            nobodyPb.isVisible = false
            everybodyRl.setOnClickListener {
                handleClick(it.id, SearchSource.EVERYBODY.name, pref)
            }
            myContactsRl.setOnClickListener {
                handleClick(it.id, SearchSource.CONTACTS.name, pref)
            }
            nobodyRl.setOnClickListener {
                handleClick(it.id, SearchSource.NOBODY.name, pref)
            }
        }
    }

    private fun handleClick(
        viewId: Int,
        targetPref: String,
        curPref: String,
    ) {
        binding.apply {
            if (R.id.everybody_rl == viewId && everybodyIv.isVisible) return
            if (R.id.contact_rl == viewId && myContactsIv.isVisible) return
            if (R.id.nobody_rl == viewId && nobodyIv.isVisible) return

            everybodyIv.isGone = true
            myContactsIv.isGone = true
            nobodyIv.isGone = true
            everybodyPb.isVisible = targetPref == SearchSource.EVERYBODY.name
            myContactsPb.isVisible = targetPref == SearchSource.CONTACTS.name
            nobodyPb.isVisible = targetPref == SearchSource.NOBODY.name
        }

        lifecycleScope.launch {
            handleMixinResponse(
                invokeNetwork = {
                    viewModel.savePreferences(AccountUpdateRequest(acceptSearchSource = targetPref))
                },
                successBlock = {
                    it.data?.let { account ->
                        Session.storeAccount(account, 13)
                        when (account.acceptSearchSource) {
                            SearchSource.EVERYBODY.name -> viewModel.searchPreference.setEveryBody()
                            SearchSource.CONTACTS.name -> viewModel.searchPreference.setContacts()
                            else -> viewModel.searchPreference.setNobody()
                        }
                    }
                },
                failureBlock = {
                    setPref(curPref)
                    resetPb(curPref)
                    return@handleMixinResponse false
                },
                exceptionBlock = {
                    setPref(curPref)
                    resetPb(curPref)
                    return@handleMixinResponse false
                },
                doAfterNetworkSuccess = {
                    resetPb(targetPref)
                },
            )
        }
    }

    private fun setPref(pref: String) {
        when (pref) {
            SearchSource.EVERYBODY.name -> {
                viewModel.searchPreference.setEveryBody()
            }
            SearchSource.CONTACTS.name -> {
                viewModel.searchPreference.setContacts()
            }
            else -> {
                viewModel.searchPreference.setNobody()
            }
        }
    }

    private fun resetPb(pref: String) {
        if (viewDestroyed()) return

        binding.apply {
            when (pref) {
                SearchSource.EVERYBODY.name -> {
                    everybodyPb.isVisible = false
                }
                SearchSource.CONTACTS.name -> {
                    myContactsPb.isVisible = false
                }
                else -> {
                    nobodyPb.isVisible = false
                }
            }
        }
    }
}
