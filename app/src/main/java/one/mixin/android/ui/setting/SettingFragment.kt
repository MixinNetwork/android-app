package one.mixin.android.ui.setting

import android.app.Dialog
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_setting.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.MixinResponse
import one.mixin.android.extension.addFragment
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.landing.LandingActivity
import one.mixin.android.util.ErrorHandler
import org.jetbrains.anko.cancelButton
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.indeterminateProgressDialog
import org.jetbrains.anko.support.v4.onUiThread
import org.jetbrains.anko.yesButton
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        logout_rl.setOnClickListener {
            alert(
                getString(R.string.setting_logout_message),
                getString(R.string.setting_logout_confirm)
            ) {
                yesButton { alert ->
                    alert.dismiss()

                    val dialog = indeterminateProgressDialog(message = getString(R.string.pb_dialog_message),
                        title = getString(R.string.logout))
                    dialog.setCancelable(false)
                    dialog.show()

                    settingViewModel.logout().subscribeOn(Schedulers.io()).subscribe({ r: MixinResponse<Unit> ->
                        if (r.isSuccess) {
                            MixinApplication.get().closeAndClear(false)
                            dismissDialog(dialog)
                            LandingActivity.show(ctx)
                        } else {
                            dismissDialog(dialog)
                            ErrorHandler.handleMixinError(r.errorCode)
                        }
                    }, { t: Throwable ->
                        dismissDialog(dialog)
                        ErrorHandler.handleError(t)
                    })
                }
                cancelButton { alert -> alert.dismiss() }
            }.show()
        }
    }

    private fun dismissDialog(dialog: Dialog?) {
        if (dialog != null && dialog.isShowing) {
            onUiThread {
                dialog.dismiss()
            }
        }
    }
}
