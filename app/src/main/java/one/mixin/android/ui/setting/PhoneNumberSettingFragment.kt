package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_appearance.title_view
import kotlinx.android.synthetic.main.fragment_setting_phone_number.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.session.Session
import one.mixin.android.vo.SearchSource

@AndroidEntryPoint
class PhoneNumberSettingFragment : BaseFragment() {
    companion object {
        const val TAG = "PhoneNumberSettingFragment"
        const val ACCEPT_SEARCH_KEY = "accept_search_key"

        fun newInstance() = PhoneNumberSettingFragment()
    }

    private val viewModel by viewModels<SettingConversationViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        layoutInflater.inflate(R.layout.fragment_setting_phone_number, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.left_ib.setOnClickListener {
            activity?.onBackPressed()
        }
        viewModel.initSearchPreference(requireContext())
            .observe(
                viewLifecycleOwner,
                {
                    it?.let {
                        render(it)
                    }
                }
            )
    }

    private fun render(pref: String) {
        everybody_iv.isVisible = pref == SearchSource.EVERYBODY.name
        my_contacts_iv.isVisible = pref == SearchSource.CONTACTS.name
        nobody_iv.isVisible = pref == SearchSource.NOBODY.name
        everybody_pb.isVisible = false
        my_contacts_pb.isVisible = false
        nobody_pb.isVisible = false
        everybody_rl.setOnClickListener {
            handleClick(it.id, SearchSource.EVERYBODY.name, pref)
        }
        my_contacts_rl.setOnClickListener {
            handleClick(it.id, SearchSource.CONTACTS.name, pref)
        }
        nobody_rl.setOnClickListener {
            handleClick(it.id, SearchSource.NOBODY.name, pref)
        }
    }

    private fun handleClick(viewId: Int, targetPref: String, curPref: String) {
        if (R.id.everybody_rl == viewId && everybody_iv.isVisible) return
        if (R.id.contact_rl == viewId && my_contacts_iv.isVisible) return
        if (R.id.nobody_rl == viewId && nobody_iv.isVisible) return

        everybody_iv.isGone = true
        my_contacts_iv.isGone = true
        nobody_iv.isGone = true
        everybody_pb.isVisible = targetPref == SearchSource.EVERYBODY.name
        my_contacts_pb.isVisible = targetPref == SearchSource.CONTACTS.name
        nobody_pb.isVisible = targetPref == SearchSource.NOBODY.name

        lifecycleScope.launch {
            handleMixinResponse(
                invokeNetwork = {
                    viewModel.savePreferences(AccountUpdateRequest(acceptSearchSource = targetPref))
                },
                successBlock = {
                    it.data?.let { account ->
                        Session.storeAccount(account)
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
                }
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
        when (pref) {
            SearchSource.EVERYBODY.name -> {
                everybody_pb?.isVisible = false
            }
            SearchSource.CONTACTS.name -> {
                my_contacts_pb?.isVisible = false
            }
            else -> {
                nobody_pb?.isVisible = false
            }
        }
    }
}
