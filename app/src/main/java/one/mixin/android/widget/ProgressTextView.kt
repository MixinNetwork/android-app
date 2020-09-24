package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import one.mixin.android.RxBus
import kotlin.jvm.JvmOverloads

class ProgressTextView @JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var disposable: Disposable? = null
    private var mId: String? = null
    private var progress: Float = 0f
        set(value) {
            field = value
            text = String.format("%.1f%%", value)
        }

    override fun onAttachedToWindow() {
        if (disposable == null) {
            disposable = RxBus.listen(ConvertEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event ->
                    if (mId == event.id) {
                        progress = event.progress
                    }
                }
        }

        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        disposable?.let {
            if (!it.isDisposed) {
                it.dispose()
                disposable = null
            }
        }
        disposable = null
        super.onDetachedFromWindow()
    }

    fun bindId(id: String?) {
        if (mId != id) {
            mId = id
            if (id != null) {
                text = "0.0%"
            }
        }
    }
}
