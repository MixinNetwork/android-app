package one.mixin.android.ui.landing

import android.Manifest
import android.os.Bundle
import android.view.View
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentRestoreBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putBoolean
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.transfer.TransferActivity
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class RestoreFragment : BaseFragment(R.layout.fragment_restore) {

    private val binding by viewBinding(FragmentRestoreBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            fromAnotherCl.setOnClickListener {
                RxPermissions(requireActivity())
                    .request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .autoDispose(stopScope)
                    .subscribe { granted ->
                        if (granted) {
                            TransferActivity.showRestoreFromPhone(requireContext())
                        } else {
                            requireActivity().openPermissionSetting()
                        }
                    }
            }
            fromLocalCl.setOnClickListener {
                RxPermissions(requireActivity())
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .autoDispose(stopScope)
                    .subscribe { granted ->
                        if (granted) {
                            navTo(LocalRestoreFragment.newInstance(), LocalRestoreFragment.TAG)
                        } else {
                            requireActivity().openPermissionSetting()
                        }
                    }
            }
            skipTv.setOnClickListener {
                InitializeActivity.showLoading(requireContext())
                defaultSharedPreferences.putBoolean(Constants.Account.PREF_RESTORE, false)
                requireActivity().finish()
            }
        }
    }

    override fun onBackPressed(): Boolean {
        return true
    }
    companion object {
        const val TAG = "RestoreFragment"

        fun newInstance() = RestoreFragment()
    }
}
