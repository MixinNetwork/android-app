package one.mixin.android.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import javax.inject.Inject

abstract class BaseViewModelFragment<VM : ViewModel> : BaseFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    protected val viewModel: VM by lazy {
        ViewModelProvider(this, viewModelFactory).get(getModelClass())
    }

    abstract fun getModelClass(): Class<VM>
}
