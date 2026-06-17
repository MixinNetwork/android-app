package one.mixin.android.ui.wallet.transfer.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ViewAnimator
import androidx.annotation.StringRes
import androidx.core.view.isInvisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import one.mixin.android.R
import one.mixin.android.databinding.ViewTransferBottomBinding
import one.mixin.android.extension.dp
import one.mixin.android.ui.wallet.transfer.data.TransferStatus

class TransferBottom : ViewAnimator {
    private val _binding: ViewTransferBottomBinding
    private val dp8 = 8.dp
    private val dp24 = 24.dp

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        _binding = ViewTransferBottomBinding.inflate(LayoutInflater.from(context), this)
        updatePaddingRelative(dp8, dp8, dp8, dp24)
    }

    fun setOnClickListener(
        cancelClickListener: OnClickListener,
        confirmClickListener: OnClickListener,
        doneClickListener: OnClickListener,
    ) {
        _binding.cancelButton.setOnClickListener(cancelClickListener)
        _binding.confirmButton.setOnClickListener(confirmClickListener)
        _binding.doneBtn.setOnClickListener(doneClickListener)
        _binding.retryCancel.setOnClickListener(cancelClickListener)
        _binding.retry.setOnClickListener(confirmClickListener)
    }

    fun setText(@StringRes res: Int) {
        _binding.confirmButton.setText(res)
    }

    fun setText(text: String) {
        _binding.confirmButton.text = text
    }

    fun updateStatus(
        status: TransferStatus,
        canretry: Boolean = false,
    ) {
        when (status) {
            TransferStatus.AWAITING_CONFIRMATION -> {
                isInvisible = false
                displayedChild = 0
                updateLayoutParams {
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                requestLayout()
            }

            TransferStatus.IN_PROGRESS -> {
                isInvisible = true
                displayedChild = 0
                updateLayoutParams {
                    height = dp24
                }
                requestLayout()
            }

            TransferStatus.SUCCESSFUL, TransferStatus.SIGNED -> {
                isInvisible = false
                displayedChild = 1
                _binding.doneBtn.setText(R.string.Done)
                updateLayoutParams {
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                requestLayout()
            }

            TransferStatus.FAILED -> {
                isInvisible = false
                if (canretry) {
                    displayedChild = 2
                } else {
                    displayedChild = 1
                    _binding.doneBtn.setText(R.string.Got_it)
                }
                updateLayoutParams {
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                requestLayout()
            }
        }
    }
}
