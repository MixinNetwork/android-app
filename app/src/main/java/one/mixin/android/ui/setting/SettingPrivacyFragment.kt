package one.mixin.android.ui.setting

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_privacy.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.vo.MessageSource
import javax.inject.Inject

class SettingPrivacyFragment : BaseFragment() {
    companion object {
        val TAG = "SettingPrivacyFragment"

        fun newInstance(): SettingPrivacyFragment = SettingPrivacyFragment()
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val settingPrivacyViewModel: SettingPrivacyViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(SettingPrivacyViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_privacy, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        settingPrivacyViewModel.countBlockingUsers().observe(this, Observer {
            it?.let {
                blocking_tv.text = "${it.size}"
            }
        })
        blocked_rl.setOnClickListener { activity?.addFragment(this@SettingPrivacyFragment,
            SettingBlockedFragment.newInstance(), SettingBlockedFragment.TAG) }
        conversation_rl.setOnClickListener { activity?.addFragment(this@SettingPrivacyFragment,
            SettingConversationFragment.newInstance(), SettingConversationFragment.TAG) }
        loadConversationSource()
    }

    private fun loadConversationSource() {
        context!!.defaultSharedPreferences.getInt(SettingConversationFragment.CONVERSATION_KEY,
            MessageSource.EVERYBODY.ordinal).apply {
            if (this == MessageSource.EVERYBODY.ordinal) {
                conversation_source_tv.setText(R.string.setting_everybody)
            } else {
                conversation_source_tv.setText(R.string.setting_my_contacts)
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            loadConversationSource()
        }
    }
}
