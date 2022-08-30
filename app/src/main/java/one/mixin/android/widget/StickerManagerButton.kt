package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import one.mixin.android.R

class StickerManagerButton : AppCompatTextView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var deleteCallback: OnClickListener? = null
    private var addCallback: OnClickListener? = null
    private var status = false // false when added and true when deleted
    fun init(deleteCallback: OnClickListener?, addCallback: OnClickListener) {
        this.deleteCallback = deleteCallback
        this.addCallback = addCallback
    }

     fun setStatus(status: Boolean) {
        this.status = status
        if (status){
            setText(R.string.Delete)
            setBackgroundResource(R.drawable.bg_round_red_btn_solid)
        }else{
            setText(R.string.Add)
            setBackgroundResource(R.drawable.bg_round_blue_btn_solid)
        }
    }

    init {
        setTextColor(ContextCompat.getColor(context,R.color.white))
        setOnClickListener {
            if (status) {
                deleteCallback?.onClick(this)
            } else {
                addCallback?.onClick(this)
            }
        }
    }
}