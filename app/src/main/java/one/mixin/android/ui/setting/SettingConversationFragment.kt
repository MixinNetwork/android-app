package one.mixin.android.ui.setting

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_setting_conversation.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.MessageSource
import javax.inject.Inject

class SettingConversationFragment : BaseFragment() {
    companion object {
        val TAG = "SettingConversationFragment"
        val CONVERSATION_KEY = "conversation_key"
        fun newInstance() = SettingConversationFragment()
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val settingConversationViewModel: SettingConversationViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(SettingConversationViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_setting_conversation, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        settingConversationViewModel.initPreferences(context!!)
            .observe(this@SettingConversationFragment, Observer {
                it?.let {
                    render(it)
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposable?.dispose()
    }

    private var disposable: Disposable? = null
    private fun render(prefer: Int) {
        if (prefer == MessageSource.EVERYBODY.ordinal) {
            everybody_iv.visibility = View.VISIBLE
            my_contacts_iv.visibility = View.GONE
            everybody_pb.visibility = View.GONE
            my_contacts_pb.visibility = View.GONE
            everybody_iv.setOnClickListener { }
            my_contacts_rl.setOnClickListener {
                everybody_iv.visibility = View.GONE
                my_contacts_pb.visibility = View.VISIBLE
                disposable?.let {
                    if (!it.isDisposed) {
                        it.dispose()
                    }
                }
                disposable = settingConversationViewModel
                    .savePreferences(AccountUpdateRequest(receiveMessageSource = MessageSource.CONTACTS.name))
                    .autoDisposable(scopeProvider)
                    .subscribe({
                        if (it.isSuccess) {
                            settingConversationViewModel.preferences.setContacts()
                        } else {
                            settingConversationViewModel.preferences.setEveryBody()
                            ErrorHandler.handleMixinError(it.errorCode)
                        }
                        my_contacts_pb?.visibility = View.GONE
                    }, {
                        my_contacts_pb?.visibility = View.GONE
                        settingConversationViewModel.preferences.setEveryBody()
                        ErrorHandler.handleError(it)
                    })
            }
        } else {
            everybody_iv.visibility = View.GONE
            my_contacts_iv.visibility = View.VISIBLE
            everybody_pb.visibility = View.GONE
            my_contacts_pb.visibility = View.GONE
            everybody_rl.setOnClickListener {
                everybody_pb.visibility = View.VISIBLE
                my_contacts_iv.visibility = View.GONE
                disposable?.let {
                    if (!it.isDisposed) {
                        it.dispose()
                    }
                }
                disposable = settingConversationViewModel
                    .savePreferences(AccountUpdateRequest(receiveMessageSource = MessageSource.EVERYBODY.name))
                    .autoDisposable(scopeProvider)
                    .subscribe({
                        if (it.isSuccess) {
                            settingConversationViewModel.preferences.setEveryBody()
                        } else {
                            settingConversationViewModel.preferences.setContacts()
                            ErrorHandler.handleMixinError(it.errorCode)
                        }
                        everybody_pb?.visibility = View.GONE
                    }, {
                        everybody_pb?.visibility = View.GONE
                        settingConversationViewModel.preferences.setContacts()
                        ErrorHandler.handleError(it)
                    })
            }
            my_contacts_rl.setOnClickListener { }
        }
    }
}
