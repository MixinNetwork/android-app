package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import one.mixin.android.BuildConfig
import one.mixin.android.R
import one.mixin.android.databinding.FragmentLandingBinding
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.navTo
import one.mixin.android.ui.setting.diagnosis.DiagnosisFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.DebugClickListener

class LandingFragment : Fragment(R.layout.fragment_landing) {
    companion object {
        const val TAG: String = "LandingFragment"

        fun newInstance() = LandingFragment()
    }

    private val binding by viewBinding(FragmentLandingBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.imageView.setOnClickListener(
            object : DebugClickListener() {
                override fun onDebugClick() {
                    navTo(DiagnosisFragment.newInstance(), DiagnosisFragment.TAG)
                }

                override fun onSingleClick() {
                }
            },
        )

        binding.version.text = getString(R.string.current_version, BuildConfig.VERSION_NAME)
        binding.createTv.setOnClickListener {
            activity?.addFragment(
                this@LandingFragment,
                CreateAccountFragment.newInstance(),
                CreateAccountFragment.TAG,
            )
        }
        binding.continueTv.setOnClickListener {
            activity?.addFragment(
                this@LandingFragment,
                MobileFragment.newInstance(),
                MobileFragment.TAG,
            )
        }
    }
}
