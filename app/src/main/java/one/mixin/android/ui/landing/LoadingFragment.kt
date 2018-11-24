package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.uber.autodispose.kotlin.autoDisposable
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.ErrorHandler
import javax.inject.Inject

class LoadingFragment : BaseFragment() {

    companion object {
        const val TAG: String = "LoadingFragment"
        const val IS_LOADED = "is_loaded"
        fun newInstance() = LoadingFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_loading, container, false)

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val loadingViewModel: LoadingViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(LoadingViewModel::class.java)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireContext().defaultSharedPreferences.putBoolean(IS_LOADED, false)
        MixinApplication.get().onlining.set(true)
        load()
    }

    private fun load() {
        if (count > 0) {
            count--
            loadingViewModel.pushAsyncSignalKeys().autoDisposable(scopeProvider).subscribe({
                when {
                    it?.isSuccess == true -> {
                        context!!.defaultSharedPreferences.putBoolean(IS_LOADED, true)
                        MainActivity.show(context!!)
                        activity?.finish()
                    }
                    it?.errorCode == ErrorHandler.AUTHENTICATION -> {
                        MixinApplication.get().closeAndClear()
                        activity?.finish()
                    }
                    else -> load()
                }
            }, {
                load()
                ErrorHandler.handleError(it)
            })
        } else {
            activity?.finish()
        }
    }

    private var count = 2
}