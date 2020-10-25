package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.event.BotEvent

class ShadowCircleView : RelativeLayout {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        LayoutInflater.from(context).inflate(R.layout.view_shadow_circle, this, true)
    }

    private var disposable: Disposable? = null
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (disposable == null) {
            disposable = RxBus.listen(BotEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        disposable?.let {
            if (!it.isDisposed) {
                it.dispose()
                disposable = null
            }
        }
        disposable = null
    }
}
