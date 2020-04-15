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
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.ui.home.bot.TOP_BOT
import one.mixin.android.util.GsonHelper

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
                .subscribe { event ->
                }
        }
    }

    private fun loadData() {
        context.defaultSharedPreferences.getString(TOP_BOT, null)?.let {
            val ids = GsonHelper.customGson.fromJson(it, Array<String>::class.java)
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
