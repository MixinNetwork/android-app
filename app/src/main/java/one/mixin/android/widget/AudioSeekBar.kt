package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSeekBar
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import one.mixin.android.RxBus
import one.mixin.android.event.ProgressEvent

class AudioSeekBar constructor(context: Context, attrs: AttributeSet) : AppCompatSeekBar(context, attrs) {

    private var disposable: Disposable? = null
    var bindId: String? = null

    override fun onAttachedToWindow() {
        if (disposable == null) {
            disposable = RxBus.listen(ProgressEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it.id == bindId && it.progress in 0f..max.toFloat()) {
                        progress = (it.progress * max).toInt()
                    } else if (it.status == CircleProgress.STATUS_PLAY || it.status == CircleProgress.STATUS_PAUSE) {
                        progress = 0
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
}