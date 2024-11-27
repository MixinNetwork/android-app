package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentTimeBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.shaking
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.landing.viewmodel.LandingViewModel

import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.reportException
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class TimeFragment : BaseFragment(R.layout.fragment_time) {
    companion object {
        const val TAG: String = "TimeFragment"

        fun newInstance() = TimeFragment()
    }

    private val landingViewModel by viewModels<LandingViewModel>()
    private val binding by viewBinding(FragmentTimeBinding::bind)

    override fun onResume() {
        super.onResume()
        checkTime()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.continueTv.setOnClickListener {
            checkTime()
        }
    }

    private var currentJob: Job? = null

    private fun checkTime() {
        if (currentJob == null || currentJob?.isActive == false) {
            binding.apply {
                everybodyPb.visibility = View.VISIBLE
                continueTv.visibility = View.INVISIBLE
                currentJob =
                    landingViewModel.pingServer(
                        {
                            if (isAdded) {
                                everybodyPb.visibility = View.INVISIBLE
                                continueTv.visibility = View.VISIBLE
                                defaultSharedPreferences.putBoolean(Constants.Account.PREF_WRONG_TIME, false)
                                MainActivity.show(requireContext())
                                activity?.finish()
                            }
                        },
                        { exception ->
                            if (isAdded) {
                                everybodyPb.visibility = View.INVISIBLE
                                continueTv.visibility = View.VISIBLE
                                if (exception == null) {
                                    info.shaking()
                                } else {
                                    ErrorHandler.handleError(exception)
                                    reportException("$TAG pingServer", exception)
                                }
                            }
                        },
                    )
            }
        }
    }
}
