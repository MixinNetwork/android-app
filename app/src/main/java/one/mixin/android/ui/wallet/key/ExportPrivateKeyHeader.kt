package one.mixin.android.ui.wallet.key

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import one.mixin.android.R
import one.mixin.android.databinding.ViewExportPrivateKeyHeaderBinding
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.textColorResource

class ExportPrivateKeyHeader : LinearLayout {
    private val _binding: ViewExportPrivateKeyHeaderBinding

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        orientation = VERTICAL
        _binding = ViewExportPrivateKeyHeaderBinding.inflate(LayoutInflater.from(context), this)
        gravity = Gravity.CENTER_HORIZONTAL
    }

    fun progress() {
        _binding.apply {
            iconLayout.displayedChild = 2
            subTitle.setTextColor(context.colorAttr(R.attr.text_assist))
            title.setText(R.string.Exporting_Private_Key)
            subTitle.setText(R.string.exporting_private_key_description)
        }
    }

    fun filed(
        errorMessage: String?,
    ) {
        _binding.apply {
            iconLayout.displayedChild = 1
            statusIcon.setImageResource(R.drawable.ic_transfer_status_failed)
            subTitle.text = errorMessage
            subTitle.textColorResource = R.color.text_color_error_tip
            title.setText(R.string.Private_Key_Export_Failed)
        }
    }

    fun success() {
        _binding.apply {
            iconLayout.displayedChild = 1
            subTitle.setTextColor(context.colorAttr(R.attr.text_assist))
            statusIcon.setImageResource(R.drawable.ic_transfer_status_success)

            title.setText(R.string.Private_Key_Export_Successful)
            subTitle.setText(R.string.private_key_export_success_description)
        }
    }

    fun awaiting() {
        _binding.apply {
            iconLayout.displayedChild = 0

            title.setText(R.string.Export_Private_Key_Confirmation)
            subTitle.setText(R.string.export_private_key_description)
            subTitle.textColorResource = R.color.text_color_error_tip
        }
    }
}
