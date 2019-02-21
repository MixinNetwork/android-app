package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.getSystemService
import kotlinx.android.synthetic.main.layout_toast.view.*
import one.mixin.android.R

class MixinToast {
    companion object {
        private const val TYPE_SUCCESS = 0
        private const val TYPE_FAILURE = 1
        private const val TYPE_WARNING = 2
        private const val TYPE_LOADING = 3

        fun showSuccess(context: Context, duration: Int = Toast.LENGTH_SHORT) {
            show(TYPE_SUCCESS, context, duration)
        }

        fun showFailure(context: Context, duration: Int = Toast.LENGTH_SHORT) {
            show(TYPE_FAILURE, context, duration)
        }

        fun showWarning(context: Context, duration: Int = Toast.LENGTH_SHORT, text: String? = null) {
            show(TYPE_WARNING, context, duration, text)
        }

        fun showLoading(context: Context, duration: Int = Toast.LENGTH_LONG) {
            show(TYPE_LOADING, context, duration)
        }

        @SuppressLint("InflateParams")
        fun show(type: Int, context: Context, duration: Int, text: String? = null) {
            val toast = Toast.makeText(context, "", duration)
            val toastLayout = context.getSystemService<LayoutInflater>()!!.inflate(R.layout.layout_toast, null)
            var toastText: String? = null
            when (type) {
                TYPE_SUCCESS -> {
                    toastLayout.iv.setImageResource(R.drawable.ic_toast_success)
                    toastLayout.pb.visibility = GONE
                    toastLayout.iv.visibility = VISIBLE
                    toastText = text ?: context.getString(R.string.successful)
                }
                TYPE_FAILURE -> {
                    toastLayout.iv.setImageResource(R.drawable.ic_toast_failure)
                    toastLayout.pb.visibility = GONE
                    toastLayout.iv.visibility = VISIBLE
                    toastText = text ?: context.getString(R.string.failed)
                }
                TYPE_WARNING -> {
                    toastLayout.iv.setImageResource(R.drawable.ic_toast_warning)
                    toastLayout.pb.visibility = GONE
                    toastLayout.iv.visibility = VISIBLE
                    toastText = text  // no default text
                }
                TYPE_LOADING -> {
                    toastLayout.pb.visibility = VISIBLE
                    toastLayout.iv.visibility = GONE
                    toastText = text ?: context.getString(R.string.loading)
                }
            }
            toastLayout.tv.text = toastText
            toast.view = toastLayout
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }
    }
}