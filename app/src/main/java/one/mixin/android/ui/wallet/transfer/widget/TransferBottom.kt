package one.mixin.android.ui.wallet.transfer.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ViewAnimator
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
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
    private var inProgressInPlace = false

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

    fun setCancelBackgroundResource(
        @DrawableRes resId: Int,
    ) {
        _binding.cancelButton.setBackgroundResource(resId)
        _binding.retryCancel.setBackgroundResource(resId)
    }

    fun setInProgressInPlace(enabled: Boolean) {
        inProgressInPlace = enabled
    }

    fun updateStatus(
        status: TransferStatus,
        canretry: Boolean = false,
    ) {
        when (status) {
            TransferStatus.AWAITING_CONFIRMATION -> {
                setConfirmButtonsVisible(true)
                isInvisible = false
                displayedChild = 0
                updateLayoutParams {
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                requestLayout()
            }

            TransferStatus.IN_PROGRESS -> {
                if (inProgressInPlace) {
                    setConfirmButtonsVisible(false)
                    isInvisible = false
                    displayedChild = 0
                    updateLayoutParams {
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                } else {
                    isInvisible = true
                    displayedChild = 0
                    updateLayoutParams {
                        height = dp24
                    }
                }
                requestLayout()
            }

            TransferStatus.SUCCESSFUL, TransferStatus.SIGNED -> {
                setConfirmButtonsVisible(true)
                isInvisible = false
                displayedChild = 1
                _binding.doneBtn.setText(R.string.Done)
                updateLayoutParams {
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                requestLayout()
            }

            TransferStatus.FAILED -> {
                setConfirmButtonsVisible(true)
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

    private fun setConfirmButtonsVisible(visible: Boolean) {
        _binding.cancelButton.isInvisible = !visible
        _binding.cancelButton.isEnabled = visible
        _binding.confirmButton.isInvisible = !visible
        _binding.confirmButton.isEnabled = visible
        _binding.progress.isVisible = !visible
    }
}
