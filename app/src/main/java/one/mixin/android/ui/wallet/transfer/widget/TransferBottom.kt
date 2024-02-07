package one.mixin.android.ui.wallet.transfer.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ViewAnimator
import androidx.core.view.isInvisible
import androidx.core.view.setPadding
import one.mixin.android.R
import one.mixin.android.databinding.ViewTransferBottomBinding
import one.mixin.android.extension.dp
import one.mixin.android.ui.wallet.transfer.data.TransferStatus

class TransferBottom : ViewAnimator {

    private val _binding: ViewTransferBottomBinding
    private val dp16 = 16.dp

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        _binding = ViewTransferBottomBinding.inflate(LayoutInflater.from(context), this)
        setPadding(dp16)
    }

    fun setOnClickListener(cancelClickListener: OnClickListener, confirmClickListener: OnClickListener, doneClickListener: OnClickListener) {
        _binding.cancelButton.setOnClickListener(cancelClickListener)
        _binding.confirmButton.setOnClickListener(confirmClickListener)
        _binding.doneBtn.setOnClickListener(doneClickListener)
    }

    fun updateStatus(status: TransferStatus) {
        when (status) {
            TransferStatus.AWAITING_CONFIRMATION -> {
                isInvisible = false
                displayedChild = 0
            }

            TransferStatus.IN_PROGRESS -> {
                isInvisible = true
                displayedChild = 0
            }

            TransferStatus.SUCCESSFUL -> {
                isInvisible = false
                displayedChild = 1
                _binding.doneBtn.setText(R.string.Done)
            }

            TransferStatus.FAILED -> {
                isInvisible = false
                displayedChild = 1
                _binding.doneBtn.setText(R.string.Got_it)
            }
        }
    }
}
