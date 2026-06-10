package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ViewAnimator
import com.uber.autodispose.android.autoDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import one.mixin.android.RxBus
import one.mixin.android.databinding.LayoutFileHolderBottomBinding
import one.mixin.android.event.ProgressEvent

class FileHolderBottomLayout constructor(context: Context, attrs: AttributeSet) : ViewAnimator(context, attrs) {
    private val binding = LayoutFileHolderBottomBinding.inflate(LayoutInflater.from(context), this)
    val fileSizeTv = binding.fileSizeTv
    val seekBar = binding.noSkipSeekBar

    private var disposable: Disposable? = null
    var bindId: String? = null
        set(value) {
            if (field != value) {
                field = value
                seekBar.progress = 0
            }
        }

    override fun onAttachedToWindow() {
        if (disposable == null) {
            disposable =
                RxBus.listen(ProgressEvent::class.java)
                    .observeOn(AndroidSchedulers.mainThread())
                    .autoDispose(this)
                    .subscribe {
                        if (it.id == bindId) {
                            if (it.status == CircleProgress.STATUS_PLAY &&
                                it.progress in 0f..seekBar.max.toFloat()
                            ) {
                                if (!seekBar.isDragging) {
                                    val eventProgress = (it.progress * seekBar.max).toInt()
                                    if (eventProgress > 0) {
                                        seekBar.progress = eventProgress
                                    }
                                }
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
                                (it.status == CircleProgress.STATUS_PLAY && it.progress == 0f) ||
                                it.status == CircleProgress.STATUS_ERROR
                            ) {
                                seekBar.progress = 0
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
