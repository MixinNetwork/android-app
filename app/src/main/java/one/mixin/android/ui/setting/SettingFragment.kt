package one.mixin.android.ui.setting

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_setting.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.addFragment
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
        notification_rl.setOnClickListener {
            activity?.addFragment(this@SettingFragment,
                NotificationsFragment.newInstance(), NotificationsFragment.TAG)
        }
        privacy_rl.setOnClickListener {
            activity?.addFragment(this@SettingFragment,
                SettingPrivacyFragment.newInstance(), SettingPrivacyFragment.TAG)
        }
        about_rl.setOnClickListener {
            activity?.addFragment(this@SettingFragment,
                AboutFragment.newInstance(), AboutFragment.TAG)
        }
    }
}
