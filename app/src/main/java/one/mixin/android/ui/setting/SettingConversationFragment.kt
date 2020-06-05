package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_setting_conversation.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.ui.common.BaseViewModelFragment
import one.mixin.android.util.Session
import one.mixin.android.vo.MessageSource

class SettingConversationFragment : BaseViewModelFragment<SettingConversationViewModel>() {
    companion object {
        const val TAG = "SettingConversationFragment"
        const val CONVERSATION_KEY = "conversation_key"
        const val CONVERSATION_GROUP_KEY = "conversation_group_key"
        fun newInstance() = SettingConversationFragment()
    }

    override fun getModelClass() = SettingConversationViewModel::class.java

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        layoutInflater.inflate(R.layout.fragment_setting_conversation, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        viewModel.initPreferences(requireContext())
            .observe(
                viewLifecycleOwner,
                Observer {
                    it?.let {
                        render(it)
                    }
                }
            )
        viewModel.initGroupPreferences(requireContext())
            .observe(
                viewLifecycleOwner,
                Observer {
                    it?.let {
                        renderGroup(it)
                    }
                }
            )
    }

    private fun render(prefer: Int) {
        if (prefer == MessageSource.EVERYBODY.ordinal) {
            everybody_iv.visibility = VISIBLE
            my_contacts_iv.visibility = View.GONE
            everybody_pb.visibility = View.GONE
            my_contacts_pb.visibility = View.GONE
            my_contacts_rl.setOnClickListener {
                if (my_contacts_iv.visibility == VISIBLE) return@setOnClickListener

                everybody_iv.visibility = View.GONE
                my_contacts_pb.visibility = VISIBLE
                lifecycleScope.launch {
                    handleMixinResponse(
                        invokeNetwork = {
                            viewModel.savePreferences(AccountUpdateRequest(receiveMessageSource = MessageSource.CONTACTS.name))
                        },
                        switchContext = Dispatchers.IO,
                        successBlock = {
                            it.data?.let { account ->
                                Session.storeAccount(account)
                            }
                            viewModel.preferences.setContacts()
                        },
                        failureBlock = {
                            viewModel.preferences.setEveryBody()
                            return@handleMixinResponse false
                        },
                        exceptionBlock = {
                            my_contacts_pb?.visibility = View.GONE
                            viewModel.preferences.setEveryBody()
                            return@handleMixinResponse false
                        },
                        doAfterNetworkSuccess = {
                            my_contacts_pb?.visibility = View.GONE
                        }
                    )
                }
            }
        } else {
            everybody_iv.visibility = View.GONE
            my_contacts_iv.visibility = VISIBLE
            everybody_pb.visibility = View.GONE
            my_contacts_pb.visibility = View.GONE
            everybody_rl.setOnClickListener {
                if (everybody_iv.visibility == VISIBLE) return@setOnClickListener

                everybody_pb.visibility = VISIBLE
                my_contacts_iv.visibility = View.GONE
                lifecycleScope.launch {
                    handleMixinResponse(
                        invokeNetwork = {
                            viewModel.savePreferences(AccountUpdateRequest(receiveMessageSource = MessageSource.EVERYBODY.name))
                        },
                        switchContext = Dispatchers.IO,
                        successBlock = {
                            it.data?.let { account ->
                                Session.storeAccount(account)
                            }
                            viewModel.preferences.setEveryBody()
                        },
                        failureBlock = {
                            viewModel.preferences.setContacts()
                            return@handleMixinResponse false
                        },
                        exceptionBlock = {
                            everybody_pb?.visibility = View.GONE
                            viewModel.preferences.setContacts()
                            return@handleMixinResponse false
                        },
                        doAfterNetworkSuccess = {
                            everybody_pb?.visibility = View.GONE
                        }
                    )
                }
            }
        }
    }

    private fun renderGroup(prefer: Int) {
        if (prefer == MessageSource.EVERYBODY.ordinal) {
            everybody_group_iv.visibility = VISIBLE
            my_contacts_group_iv.visibility = View.GONE
            everybody_group_pb.visibility = View.GONE
            my_contacts_group_pb.visibility = View.GONE
            my_contacts_group_rl.setOnClickListener {
                if (my_contacts_group_iv.visibility == VISIBLE) return@setOnClickListener

                everybody_group_iv.visibility = View.GONE
                my_contacts_group_pb.visibility = VISIBLE
                lifecycleScope.launch {
                    handleMixinResponse(
                        invokeNetwork = {
                            viewModel.savePreferences(AccountUpdateRequest(acceptConversationSource = MessageSource.CONTACTS.name))
                        },
                        switchContext = Dispatchers.IO,
                        successBlock = {
                            it.data?.let { account ->
                                Session.storeAccount(account)
                            }
                            viewModel.groupPreferences.setContacts()
                        },
                        failureBlock = {
                            viewModel.groupPreferences.setEveryBody()
                            return@handleMixinResponse false
                        },
                        exceptionBlock = {
                            my_contacts_group_pb?.visibility = View.GONE
                            viewModel.groupPreferences.setEveryBody()
                            return@handleMixinResponse false
                        },
                        doAfterNetworkSuccess = {
                            my_contacts_group_pb?.visibility = View.GONE
                        }
                    )
                }
            }
        } else {
            everybody_group_iv.visibility = View.GONE
            my_contacts_group_iv.visibility = VISIBLE
            everybody_group_pb.visibility = View.GONE
            my_contacts_group_pb.visibility = View.GONE
            everybody_group_rl.setOnClickListener {
                if (everybody_group_iv.visibility == VISIBLE) return@setOnClickListener

                everybody_group_pb.visibility = VISIBLE
                my_contacts_group_iv.visibility = View.GONE
                lifecycleScope.launch {
                    handleMixinResponse(
                        invokeNetwork = {
                            viewModel.savePreferences(AccountUpdateRequest(acceptConversationSource = MessageSource.EVERYBODY.name))
                        },
                        switchContext = Dispatchers.IO,
                        successBlock = {
                            it.data?.let { account ->
                                Session.storeAccount(account)
                            }
                            viewModel.groupPreferences.setEveryBody()
                        },
                        failureBlock = {
                            viewModel.groupPreferences.setContacts()
                            return@handleMixinResponse false
                        },
                        exceptionBlock = {
                            everybody_group_pb?.visibility = View.GONE
                            viewModel.groupPreferences.setContacts()
                            return@handleMixinResponse false
                        },
                        doAfterNetworkSuccess = {
                            everybody_group_pb?.visibility = View.GONE
                        }
                    )
                }
            }
        }
    }
}
