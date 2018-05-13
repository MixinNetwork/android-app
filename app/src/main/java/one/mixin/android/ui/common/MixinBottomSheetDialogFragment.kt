package one.mixin.android.ui.common

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import one.mixin.android.R
import one.mixin.android.di.Injectable
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.widget.BottomSheet
import javax.inject.Inject

abstract class MixinBottomSheetDialogFragment : DialogFragment(), Injectable {

    protected lateinit var contentView: View
    protected val scopeProvider: AndroidLifecycleScopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    protected val bottomViewModel: BottomSheetViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(BottomSheetViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.AppTheme_Dialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheet {
        return BottomSheet.Builder(context!!, true).create()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activity is UrlInterpreterActivity) {
            (activity as UrlInterpreterActivity).finish()
        }
    }

    override fun dismiss() {
        if (isAdded) {
            super.dismiss()
        }
    }
}