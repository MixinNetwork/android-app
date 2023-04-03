package one.mixin.android.ui.landing

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentRestoreBinding
import one.mixin.android.extension.base64RawURLDecode
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.toUri
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.qr.CaptureActivity
import one.mixin.android.ui.transfer.TransferActivity
import one.mixin.android.ui.transfer.vo.TransferCommandData
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.viewBinding
import timber.log.Timber

@AndroidEntryPoint
class RestoreFragment : BaseFragment(R.layout.fragment_restore) {

    private val binding by viewBinding(FragmentRestoreBinding::bind)

    lateinit var getScanResult: ActivityResultLauncher<Pair<String, Boolean>>

    private val gson by lazy {
        GsonHelper.customGson
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        getScanResult = registerForActivityResult(
            CaptureActivity.CaptureContract(),
            requireActivity().activityResultRegistry,
            ::callbackScan,
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            fromAnotherCl.setOnClickListener {
                RxPermissions(requireActivity())
                    .request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .autoDispose(stopScope)
                    .subscribe { granted ->
                        if (granted) {
                            getScanResult.launch(Pair(CaptureActivity.ARGS_FOR_SCAN_RESULT, true))
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

    private fun callbackScan(intent: Intent?) {
        val qrContent = intent?.getStringExtra(CaptureActivity.ARGS_FOR_SCAN_RESULT) ?: return
        val transferCommandData = try {
            val data = qrContent.toUri().getQueryParameter("data")?.base64RawURLDecode() ?: return
            gson.fromJson(String(data), TransferCommandData::class.java)
        } catch (e: Exception) {
            Timber.e("Invalid TransferCommandData")
            return
        }
        TransferActivity.show(requireContext(), transferCommandData, false)
        requireActivity().finish()
    }

    companion object {
        const val TAG = "RestoreFragment"

        fun newInstance() = RestoreFragment()
    }
}
