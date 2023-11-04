package one.mixin.android.ui.landing

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.databinding.FragmentLandingBinding
import one.mixin.android.databinding.ViewLandingBinding
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.highlightStarTag
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.navTo
import one.mixin.android.extension.toast
import one.mixin.android.ui.setting.diagnosis.DiagnosisFragment
import one.mixin.android.util.debug.FileLogTree
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.BottomSheet

class LandingFragment : Fragment(R.layout.fragment_landing) {

    companion object {
        const val TAG: String = "LandingFragment"

        fun newInstance() = LandingFragment()
    }

    private val binding by viewBinding(FragmentLandingBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val policy: String = requireContext().getString(R.string.Privacy_Policy)
        val termsService: String = requireContext().getString(R.string.Terms_of_Service)
        val policyWrapper = requireContext().getString(R.string.landing_introduction, "**$policy**", "**$termsService**")
        val policyUrl = getString(R.string.landing_privacy_policy_url)
        val termsUrl = getString(R.string.landing_terms_url)
        binding.introductionTv.highlightStarTag(
            policyWrapper,
            arrayOf(policyUrl, termsUrl),
        )

        binding.agreeTv.setOnClickListener {
            activity?.addFragment(
                this@LandingFragment,
                MobileFragment.newInstance(),
                MobileFragment.TAG,
            )
        }
        binding.imageView.setOnLongClickListener {
            val builder = BottomSheet.Builder(requireActivity())
            val viewBinding = ViewLandingBinding.inflate(
                LayoutInflater.from(
                    ContextThemeWrapper(requireActivity(), R.style.Custom),
                ),
                null,
                false,
            )
            builder.setCustomView(viewBinding.root)
            val bottomSheet = builder.create()
            viewBinding.shareTv.setOnClickListener {
                shareLogsFile()
                bottomSheet.dismiss()
            }
            viewBinding.networkTv.setOnClickListener {
                navTo(DiagnosisFragment.newInstance(), DiagnosisFragment.TAG)
                bottomSheet.dismiss()
            }
            bottomSheet.show()
            true
        }
    }

    private fun shareLogsFile() {
        val dialog = indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
            setCancelable(false)
        }
        dialog.show()

        lifecycleScope.launch(Dispatchers.IO) {
            val logFile = FileLogTree.getLogFile()
            withContext(Dispatchers.Main) {
                dialog.dismiss()
                if (logFile.length() <= 0) {
                    toast(R.string.File_does_not_exist)
                }

                Intent().apply {
                    val uri = logFile.absolutePath.toUri()
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val extraMimeTypes = arrayOf("text/plain", "audio/*", "image/*", "video/*")
                    putExtra(Intent.EXTRA_MIME_TYPES, extraMimeTypes)
                    type = "application/*"

                    val resInfoList = requireContext().packageManager.queryIntentActivities(
                        this,
                        PackageManager.MATCH_DEFAULT_ONLY,
                    )
                    for (resolveInfo in resInfoList) {
                        val packageName = resolveInfo.activityInfo.packageName
                        requireContext().grantUriPermission(
                            packageName,
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    }
                    try {
                        startActivity(Intent.createChooser(this, logFile.name))
                    } catch (ignored: ActivityNotFoundException) {
                    }
                }
            }
        }
    }
}
