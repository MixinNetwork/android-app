package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_time.*
import kotlinx.coroutines.Job
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.shaking
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.ErrorHandler

@AndroidEntryPoint
class TimeFragment : BaseFragment() {

    companion object {
        const val TAG: String = "TimeFragment"
        fun newInstance() = TimeFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_time, container, false)

    private val loadingViewModel by viewModels<LoadingViewModel>()

    override fun onResume() {
        super.onResume()
        checkTime()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        continue_tv.setOnClickListener {
            checkTime()
        }
    }

    private var currentJob: Job? = null
    private fun checkTime() {
        if (currentJob == null || currentJob?.isActive == false) {
            everybody_pb.visibility = View.VISIBLE
            continue_tv.visibility = View.INVISIBLE
            currentJob = loadingViewModel.pingServer(
                {
                    if (isAdded) {
                        everybody_pb.visibility = View.INVISIBLE
                        continue_tv.visibility = View.VISIBLE
                        defaultSharedPreferences.putBoolean(Constants.Account.PREF_WRONG_TIME, false)
                        MainActivity.show(requireContext())
                        activity?.finish()
                    }
                },
                { exception ->
                    if (isAdded) {
                        everybody_pb.visibility = View.INVISIBLE
                        continue_tv.visibility = View.VISIBLE
                        if (exception == null) {
                            info.shaking()
                        } else {
                            ErrorHandler.handleError(exception)
                        }
                    }
                }
            )
        }
    }
}
