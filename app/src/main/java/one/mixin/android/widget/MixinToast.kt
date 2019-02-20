package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.core.content.getSystemService
import kotlinx.android.synthetic.main.layout_toast.view.*
import one.mixin.android.R

class MixinToast {
    companion object {
        const val TYPE_SUCCESS = 0
        const val TYPE_FAILURE = 1
        const val TYPE_LOADING = 2

        fun showSuccess(context: Context, duration: Int = Toast.LENGTH_SHORT) {
            show(TYPE_SUCCESS, context, duration)
        }

        fun showFailure(context: Context, duration: Int = Toast.LENGTH_SHORT) {
            show(TYPE_FAILURE, context, duration)
        }

        fun showLoading(context: Context, duration: Int = Toast.LENGTH_LONG) {
            show(TYPE_LOADING, context, duration)
        }

        @SuppressLint("InflateParams")
        fun show(type: Int, context: Context, duration: Int) {
            val toast = Toast.makeText(context, "", duration)
            val toastLayout = context.getSystemService<LayoutInflater>()!!.inflate(R.layout.layout_toast, null)
            when (type) {
                TYPE_SUCCESS -> {
                    toastLayout.iv.setImageResource(R.drawable.ic_toast_success)
                    toastLayout.pb.visibility = GONE
                    toastLayout.iv.visibility = VISIBLE
                    toastLayout.tv.setText(R.string.successful)
                }
                TYPE_FAILURE -> {
                    toastLayout.iv.setImageResource(R.drawable.ic_toast_failure)
                    toastLayout.pb.visibility = GONE
                    toastLayout.iv.visibility = VISIBLE
                    toastLayout.tv.setText(R.string.failed)
                }
                TYPE_LOADING -> {
                    toastLayout.pb.visibility = VISIBLE
                    toastLayout.iv.visibility = GONE
                    toastLayout.tv.setText(R.string.loading)
                }
            }
            toast.view = toastLayout
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }
    }
}