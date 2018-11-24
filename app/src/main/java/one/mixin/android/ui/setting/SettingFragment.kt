package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.fragment_setting.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.isGooglePlayServicesAvailable
import one.mixin.android.ui.common.BaseFragment
import javax.inject.Inject

class SettingFragment : BaseFragment() {
    companion object {
        const val TAG = "SettingFragment"

        fun newInstance() = SettingFragment()
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val settingViewModel: SettingViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(SettingViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_setting, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.left_ib.setOnClickListener {
            activity?.onBackPressed()
        }
        about_rl.setOnClickListener {
            activity?.addFragment(this@SettingFragment,
                AboutFragment.newInstance(), AboutFragment.TAG)
        }
        storage_rl.setOnClickListener {
            requireActivity().addFragment(this@SettingFragment,
                SettingStorageFragment.newInstance(), SettingStorageFragment.TAG)
        }

        settingViewModel.countBlockingUsers().observe(this, Observer {
            it?.let {
                blocking_tv.text = "${it.size}"
            }
        })
        blocked_rl.setOnClickListener {
            activity?.addFragment(this@SettingFragment,
                SettingBlockedFragment.newInstance(), SettingBlockedFragment.TAG)
        }
        conversation_rl.setOnClickListener {
            activity?.addFragment(this@SettingFragment,
                SettingConversationFragment.newInstance(), SettingConversationFragment.TAG)
        }
        auth_rl.setOnClickListener {
            activity?.addFragment(this@SettingFragment,
                AuthenticationsFragment.newInstance(), AuthenticationsFragment.TAG)
        }

        if (requireContext().isGooglePlayServicesAvailable()) {
            backup_layout.visibility = View.VISIBLE
            backup_rl.setOnClickListener {
                activity?.addFragment(this@SettingFragment,
                    BackUpFragment.newInstance(), BackUpFragment.TAG)
            }
        } else {
            backup_layout.visibility = View.GONE
        }
    }
}
