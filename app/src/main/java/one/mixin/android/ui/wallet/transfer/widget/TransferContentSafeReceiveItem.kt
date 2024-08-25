package one.mixin.android.ui.wallet.transfer.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.text.SpannableString
import android.text.Spanned
import android.text.style.LineHeightSpan
import android.text.style.ReplacementSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.StringRes
import one.mixin.android.R
import one.mixin.android.api.response.SafeTransactionRecipient
import one.mixin.android.databinding.ItemTransferRecipientBinding
import one.mixin.android.databinding.ItemTransferSafeReceiveContentBinding
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.dp
import one.mixin.android.extension.layoutInflater

class TransferContentSafeReceiveItem : LinearLayout {
    private val _binding: ItemTransferSafeReceiveContentBinding
    private val dp28 = 28.dp
    private val dp8 = 8.dp

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        orientation = VERTICAL
        _binding = ItemTransferSafeReceiveContentBinding.inflate(LayoutInflater.from(context), this)
        setPadding(dp28, dp8, dp28, dp8)
    }

    @SuppressLint("SetTextI18n")
    fun setContent(
        @StringRes titleRes: Int,
        list: List<SafeTransactionRecipient>,
        symbol: String,
    ) {
        _binding.apply {
            title.text = context.getString(titleRes).uppercase()
            list.forEach { recipient ->

                val itemView = ItemTransferRecipientBinding.inflate(context.layoutInflater, this@TransferContentSafeReceiveItem, false)

                itemView.addressTextView.text = createRecipientSpannable(recipient.label, recipient.address)
                itemView.amountTextView.text = "${recipient.amount} $symbol"

                container.addView(itemView.root, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    topMargin = 16.dp
                })
            }
        }
    }

    private fun createRecipientSpannable(label: String?, address: String): SpannableString {
        val topBottomPadding = 2.5f.dp
        val borderWidth = 1.dp
        val leftRightPadding = 10.dp
        val radius = 16.dp.toFloat()
        val borderColor = context.colorAttr(R.attr.bg_window)
        val textAssist = context.colorAttr(R.attr.text_assist)
        val textMinor = context.colorAttr(R.attr.text_minor)

        return if (label != null) {
            val labelText = "$label"
            val spannableString = SpannableString("$labelText  $address")

            val backgroundSpan = object : ReplacementSpan(), LineHeightSpan {
                override fun getSize(
                    paint: Paint,
                    text: CharSequence,
                    start: Int,
                    end: Int,
                    fm: Paint.FontMetricsInt?
                ): Int {
                    paint.getTextBounds(text.toString(), start, end, Rect())
                    return paint.measureText(text, start, end).toInt() + leftRightPadding * 2
                }

                override fun draw(
                    canvas: Canvas,
                    text: CharSequence,
                    start: Int,
                    end: Int,
                    x: Float,
                    top: Int,
                    y: Int,
                    bottom: Int,
                    paint: Paint
                ) {
                    val textWidth = paint.measureText(text, start, end)

                    val rect = RectF(
                        x + borderWidth / 2,
                        y + paint.ascent() - topBottomPadding - borderWidth,
                        x + textWidth + leftRightPadding * 2,
                        y + paint.descent() + topBottomPadding + borderWidth
                    )

                    paint.color = borderColor
                    canvas.drawRoundRect(rect, radius, radius, paint)

                    paint.style = Paint.Style.STROKE
                    paint.color = textAssist
                    paint.strokeWidth = borderWidth.toFloat()
                    canvas.drawRoundRect(rect, radius, radius, paint)

                    paint.style = Paint.Style.FILL
                    paint.color = textAssist
                    canvas.drawText(text, start, end, x + leftRightPadding, y.toFloat(), paint)
                }

                override fun chooseHeight(
                    text: CharSequence,
                    start: Int,
                    end: Int,
                    spanstartv: Int,
                    v: Int,
                    fm: Paint.FontMetricsInt
                ) {
                    val originalHeight = fm.descent - fm.ascent
                    val totalPadding = (topBottomPadding + borderWidth) * 2

                    if (originalHeight < totalPadding) {
                        val extraPadding = totalPadding - originalHeight

                        fm.ascent -= extraPadding / 2
                        fm.descent += extraPadding / 2

                        fm.top = fm.ascent
                        fm.bottom = fm.descent
                    }
                }
            }

            spannableString.setSpan(backgroundSpan, 0, labelText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannableString
        } else {
            SpannableString(address)
        }
    }
}
