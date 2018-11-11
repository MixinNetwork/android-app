package one.mixin.android.ui.common

import android.os.Bundle
import android.view.View
import androidx.fragment.app.MixinDialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import one.mixin.android.R
import one.mixin.android.di.Injectable
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.widget.BottomSheet
import javax.inject.Inject

abstract class MixinBottomSheetDialogFragment : MixinDialogFragment(), Injectable {

    protected lateinit var contentView: View
    protected val scopeProvider: AndroidLifecycleScopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    protected val bottomViewModel: BottomSheetViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(BottomSheetViewModel::class.java)
    }

    override fun getTheme() = R.style.AppTheme_Dialog

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheet {
        return BottomSheet.Builder(requireActivity(), true).create()
    }

    override fun onDetach() {
        super.onDetach()
        if (activity is UrlInterpreterActivity) {
            fragmentManager?.fragments?.let {
                if (it.size <= 0) {
                    activity?.finish()
                }
            }
        }
    }

    override fun dismiss() {
        if (isAdded) {
            try {
                dialog.dismiss()
            } catch (e: IllegalStateException) {
                super.dismissAllowingStateLoss()
            }
        }
    }
}