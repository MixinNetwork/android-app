package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.crypto.PrivacyPreference.getIsLoaded
import one.mixin.android.crypto.PrivacyPreference.getIsSyncSession
import one.mixin.android.crypto.PrivacyPreference.putIsLoaded
import one.mixin.android.crypto.PrivacyPreference.putIsSyncSession
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session

@AndroidEntryPoint
class LoadingFragment : BaseFragment() {

    companion object {
        const val TAG: String = "LoadingFragment"

        fun newInstance() = LoadingFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_loading, container, false)

    private val loadingViewModel by viewModels<LoadingViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MixinApplication.get().onlining.set(true)
        lifecycleScope.launch {
            if (!getIsLoaded(requireContext(), false)) {
                load()
            }

            if (!getIsSyncSession(requireContext(), false)) {
                syncSession()
            }
            context?.let {
                MainActivity.show(it)
            }
            activity?.finish()
        }
    }

    private suspend fun syncSession() {
        try {
            Session.deleteExtensionSessionId()
            loadingViewModel.updateSignalSession()
            putIsSyncSession(requireContext(), true)
        } catch (e: Exception) {
            ErrorHandler.handleError(e)
        }
    }

    private suspend fun load() {
        if (count > 0) {
            count--
            try {
                val response = loadingViewModel.pushAsyncSignalKeys()
                when {
                    response.isSuccess -> {
                        putIsLoaded(requireContext(), true)
                    }
                    response.errorCode == ErrorHandler.AUTHENTICATION -> {
                        withContext(Dispatchers.IO) {
                            MixinApplication.get().closeAndClear()
                        }
                        activity?.finish()
                    }
                    else -> load()
                }
            } catch (e: Exception) {
                ErrorHandler.handleError(e)
                load()
            }
        }
    }

    private var count = 2
}
