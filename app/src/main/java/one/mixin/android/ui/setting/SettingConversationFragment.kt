package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.View
import android.view.View.VISIBLE
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.databinding.FragmentSettingConversationBinding
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.MessageSource

@AndroidEntryPoint
class SettingConversationFragment : BaseFragment(R.layout.fragment_setting_conversation) {
    companion object {
        const val TAG = "SettingConversationFragment"
        const val CONVERSATION_KEY = "conversation_key"
        const val CONVERSATION_GROUP_KEY = "conversation_group_key"

        fun newInstance() = SettingConversationFragment()
    }

    private val viewModel by viewModels<SettingConversationViewModel>()
    private val binding by viewBinding(FragmentSettingConversationBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
        viewModel.initPreferences(requireContext())
            .observe(
                viewLifecycleOwner,
                {
                    it?.let {
                        render(it)
                    }
                },
            )
        viewModel.initGroupPreferences(requireContext())
            .observe(
                viewLifecycleOwner,
                {
                    it?.let {
                        renderGroup(it)
                    }
                },
            )
    }

    private fun render(prefer: Int) {
        binding.apply {
            if (prefer == MessageSource.EVERYBODY.ordinal) {
                everybodyIv.visibility = VISIBLE
                myContactsIv.visibility = View.GONE
                everybodyPb.visibility = View.GONE
                myContactsPb.visibility = View.GONE
                myContactsRl.setOnClickListener {
                    if (myContactsIv.visibility == VISIBLE) return@setOnClickListener

                    everybodyIv.visibility = View.GONE
                    myContactsPb.visibility = VISIBLE
                    lifecycleScope.launch {
                        handleMixinResponse(
                            invokeNetwork = {
                                viewModel.savePreferences(AccountUpdateRequest(receiveMessageSource = MessageSource.CONTACTS.name))
                            },
                            successBlock = {
                                it.data?.let { account ->
                                    Session.storeAccount(account, 14)
                                }
                                viewModel.preferences.setContacts()
                            },
                            failureBlock = {
                                viewModel.preferences.setEveryBody()
                                return@handleMixinResponse false
                            },
                            exceptionBlock = {
                                myContactsPb.visibility = View.GONE
                                viewModel.preferences.setEveryBody()
                                return@handleMixinResponse false
                            },
                            doAfterNetworkSuccess = {
                                myContactsPb.visibility = View.GONE
                            },
                        )
                    }
                }
            } else {
                everybodyIv.visibility = View.GONE
                myContactsIv.visibility = VISIBLE
                everybodyPb.visibility = View.GONE
                myContactsPb.visibility = View.GONE
                everybodyRl.setOnClickListener {
                    if (everybodyIv.visibility == VISIBLE) return@setOnClickListener

                    everybodyPb.visibility = VISIBLE
                    myContactsIv.visibility = View.GONE
                    lifecycleScope.launch {
                        handleMixinResponse(
                            invokeNetwork = {
                                viewModel.savePreferences(AccountUpdateRequest(receiveMessageSource = MessageSource.EVERYBODY.name))
                            },
                            successBlock = {
                                it.data?.let { account ->
                                    Session.storeAccount(account, 16)
                                }
                                viewModel.preferences.setEveryBody()
                            },
                            failureBlock = {
                                viewModel.preferences.setContacts()
                                return@handleMixinResponse false
                            },
                            exceptionBlock = {
                                everybodyPb.visibility = View.GONE
                                viewModel.preferences.setContacts()
                                return@handleMixinResponse false
                            },
                            doAfterNetworkSuccess = {
                                everybodyPb.visibility = View.GONE
                            },
                        )
                    }
                }
            }
        }
    }

    private fun renderGroup(prefer: Int) {
        binding.apply {
            if (prefer == MessageSource.EVERYBODY.ordinal) {
                everybodyGroupIv.visibility = VISIBLE
                myContactsGroupIv.visibility = View.GONE
                everybodyGroupPb.visibility = View.GONE
                myContactsGroupPb.visibility = View.GONE
                myContactsGroupRl.setOnClickListener {
                    if (myContactsGroupIv.visibility == VISIBLE) return@setOnClickListener

                    everybodyGroupIv.visibility = View.GONE
                    myContactsGroupPb.visibility = VISIBLE
                    lifecycleScope.launch {
                        handleMixinResponse(
                            invokeNetwork = {
                                viewModel.savePreferences(AccountUpdateRequest(acceptConversationSource = MessageSource.CONTACTS.name))
                            },
                            successBlock = {
                                it.data?.let { account ->
                                    Session.storeAccount(account, 15)
                                }
                                viewModel.groupPreferences.setContacts()
                            },
                            failureBlock = {
                                viewModel.groupPreferences.setEveryBody()
                                return@handleMixinResponse false
                            },
                            exceptionBlock = {
                                myContactsGroupPb.visibility = View.GONE
                                viewModel.groupPreferences.setEveryBody()
                                return@handleMixinResponse false
                            },
                            doAfterNetworkSuccess = {
                                myContactsGroupPb.visibility = View.GONE
                            },
                        )
                    }
                }
            } else {
                everybodyGroupIv.visibility = View.GONE
                myContactsGroupIv.visibility = VISIBLE
                everybodyGroupPb.visibility = View.GONE
                myContactsGroupPb.visibility = View.GONE
                everybodyGroupRl.setOnClickListener {
                    if (everybodyGroupIv.visibility == VISIBLE) return@setOnClickListener

                    everybodyGroupPb.visibility = VISIBLE
                    myContactsGroupIv.visibility = View.GONE
                    lifecycleScope.launch {
                        handleMixinResponse(
                            invokeNetwork = {
                                viewModel.savePreferences(AccountUpdateRequest(acceptConversationSource = MessageSource.EVERYBODY.name))
                            },
                            successBlock = {
                                it.data?.let { account ->
                                    Session.storeAccount(account, 17)
                                }
                                viewModel.groupPreferences.setEveryBody()
                            },
                            failureBlock = {
                                viewModel.groupPreferences.setContacts()
                                return@handleMixinResponse false
                            },
                            exceptionBlock = {
                                everybodyGroupPb.visibility = View.GONE
                                viewModel.groupPreferences.setContacts()
                                return@handleMixinResponse false
                            },
                            doAfterNetworkSuccess = {
                                everybodyGroupPb.visibility = View.GONE
                            },
                        )
                    }
                }
            }
        }
    }
}
