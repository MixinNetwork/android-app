package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import one.mixin.android.RxBus
import one.mixin.android.event.ProgressEvent
import one.mixin.android.extension.fileSize
import one.mixin.android.extension.fileUnit

class FileProgressTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    AppCompatTextView(context, attrs, defStyleAttr) {

    private var disposable: Disposable? = null
    private var mBindId: String? = null
    private var progress: Float = 0f
    private var mediaSize: Long = 0L

    @SuppressLint("SetTextI18n")
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (disposable == null) {
            disposable = RxBus.listen(ProgressEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event ->
                    if (event.id == mBindId && progress != event.progress) {
                        progress = event.progress
                        text = "${mediaSize.run { (this * progress).toLong() }.fileSize()} / ${mediaSize.fileSize()}"
                    }
                }
        }
    }

    @SuppressLint("SetTextI18n")
    fun setBindId(id: String, mediaSize: Long) {
        if (id != mBindId) {
            text = "0${mediaSize.fileUnit()} / ${mediaSize.fileSize()}"
            this.mediaSize = mediaSize
            mBindId = id
        }
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
}
