package one.mixin.android.ui.common

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.uber.autodispose.android.lifecycle.scope
import one.mixin.android.R
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.widget.BottomSheet
import timber.log.Timber

abstract class MixinBottomSheetDialogFragment : DialogFragment() {
    protected lateinit var contentView: View
    protected val stopScope = scope(Lifecycle.Event.ON_STOP)
    protected val destroyScope = scope(Lifecycle.Event.ON_DESTROY)

    protected val bottomViewModel by viewModels<BottomSheetViewModel>()

    override fun getTheme() = R.style.AppTheme_Dialog

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheet {
        return BottomSheet.Builder(requireActivity(), needFocus = true, softInputResize = true)
            .create()
    }

    override fun onDetach() {
        super.onDetach()
        // UrlInterpreterActivity doesn't have a UI and needs it's son fragment to handle it's finish.
        if (activity is UrlInterpreterActivity) {
            var realFragmentCount = 0
            parentFragmentManager.fragments.forEach { f ->
                realFragmentCount++
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
                    Timber.w(e)
                }
            }
        } else {
            try {
                super.dismissAllowingStateLoss()
            } catch (e: IllegalStateException) {
                Timber.w(e)
            }
        }
    }

    override fun show(
        manager: FragmentManager,
        tag: String?,
    ) {
        try {
            super.show(manager, tag)
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    override fun showNow(
        manager: FragmentManager,
        tag: String?,
    ) {
        try {
            super.showNow(manager, tag)
        } catch (e: Exception) {
            Timber.w(e)
        }
    }
}
