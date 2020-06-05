package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ViewAnimator
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.layout_file_holder_bottom.view.*
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.event.ProgressEvent

class FileHolderBottomLayout constructor(context: Context, attrs: AttributeSet) : ViewAnimator(context, attrs) {

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_file_holder_bottom, this, true)
    }

    private var disposable: Disposable? = null
    var bindId: String? = null
        set(value) {
            if (field != value) {
                field = value
                seek_bar.progress = 0
            }
        }

    override fun onAttachedToWindow() {
        if (disposable == null) {
            disposable = RxBus.listen(ProgressEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it.id == bindId) {
                        if (it.status == CircleProgress.STATUS_PLAY &&
                            it.progress in 0f..seek_bar.max.toFloat()
                        ) {
                            seek_bar.progress = (it.progress * seek_bar.max).toInt()
                            if (displayedChild != POS_SEEK_BAR) {
                                showSeekBar()
                            }
                        } else if (it.status == CircleProgress.STATUS_PAUSE) {
                            if (displayedChild != POS_TEXT) {
                                showText()
                            }
                        }
                    } else {
                        if (it.status == CircleProgress.STATUS_PAUSE ||
                            it.status == CircleProgress.STATUS_PLAY ||
                            it.status == CircleProgress.STATUS_ERROR
                        ) {
                            seek_bar.progress = 0
                            if (displayedChild != POS_TEXT) {
                                showText()
                            }
                        }
                    }
                }
        }
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        disposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
            disposable = null
        }
        super.onDetachedFromWindow()
    }

    fun showSeekBar() {
        displayedChild = POS_SEEK_BAR
    }

    fun showText() {
        displayedChild = POS_TEXT
    }

    companion object {
        const val POS_TEXT = 0
        const val POS_SEEK_BAR = 1
    }
}
