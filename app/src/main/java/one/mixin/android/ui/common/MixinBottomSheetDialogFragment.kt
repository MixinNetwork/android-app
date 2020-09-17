package one.mixin.android.ui.common

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.manager.SupportRequestManagerFragment
import com.uber.autodispose.android.lifecycle.scope
import one.mixin.android.R
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.widget.BottomSheet
import timber.log.Timber
import java.lang.Exception
import javax.inject.Inject

abstract class MixinBottomSheetDialogFragment : DialogFragment() {

    protected lateinit var contentView: View
    protected val stopScope = scope(Lifecycle.Event.ON_STOP)

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    protected val bottomViewModel: BottomSheetViewModel by viewModels { viewModelFactory }

    override fun getTheme() = R.style.AppTheme_Dialog

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheet {
        return BottomSheet.Builder(requireActivity(), needFocus = true, softInputResize = true)
            .create()
    }

    override fun onDetach() {
        super.onDetach()
        if (activity is UrlInterpreterActivity) {
            var realFragmentCount = 0
            parentFragmentManager.fragments.forEach { f ->
                if (f !is SupportRequestManagerFragment) {
                    realFragmentCount++
                }
            }
            if (realFragmentCount <= 0) {
                activity?.finish()
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        safeDismiss()
    }

    override fun dismiss() {
        safeDismiss()
    }

    private fun safeDismiss() {
        if (isAdded) {
            dialog?.dismiss()
            // Prevent dialog slide animation end
            dialog?.setOnDismissListener {
                try {
                    super.dismissAllowingStateLoss()
                } catch (e: IllegalStateException) {
                }
            }
        } else {
            try {
                super.dismissAllowingStateLoss()
            } catch (e: IllegalStateException) {
            }
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        try {
            super.show(manager, tag)
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    override fun showNow(manager: FragmentManager, tag: String?) {
        try {
            super.showNow(manager, tag)
        } catch (e: Exception) {
            Timber.w(e)
        }
    }
}
