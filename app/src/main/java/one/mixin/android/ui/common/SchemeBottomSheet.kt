package one.mixin.android.ui.common

import androidx.annotation.StringRes
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import one.mixin.android.R
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SyncOutputJob

abstract class SchemeBottomSheet : BottomSheetDialogFragment() {
    open fun showError(@StringRes errorRes: Int = R.string.Invalid_Link) {
        showError(getString(errorRes))
    }

    abstract fun showError(error: String)

    abstract fun syncUtxo()
}
