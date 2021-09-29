package one.mixin.android.widget.bot

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.arraySetOf
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorCompat
import androidx.core.view.ViewPropertyAnimatorListener
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ItemDockBinding
import one.mixin.android.extension.clickVibrate
import one.mixin.android.vo.BotInterface
import java.lang.Exception
import kotlin.math.max

class BotDock : ViewGroup, View.OnLongClickListener {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    @SuppressLint("InflateParams")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        val layoutInflater = LayoutInflater.from(context)
        addView(layoutInflater.inflate(R.layout.item_dock_empty, null))
        repeat(4) { index ->
            addView(
                layoutInflater.inflate(R.layout.item_dock, null).apply {
                    id = when (index) {
                        0 -> R.id.dock_1
                        1 -> R.id.dock_2
                        2 -> R.id.dock_3
                        else -> R.id.dock_4
                    }
                }
            )
        }
        clipChildren = false
        clipToPadding = false
        render()
    }

    private fun getItemDock(view: View) = ItemDockBinding.bind(view)

    private var itemWidth = 0
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildren(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.AT_MOST), heightMeasureSpec)
        var height = 0
        var width = 0
        for (i in 1 until childCount) {
            val child = getChildAt(i)
            width += child.measuredWidth

            height = max(child.measuredHeight, height)
        }
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height + paddingBottom + paddingTop)
        itemWidth = (measuredWidth - paddingStart - paddingEnd) / 4
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var itemOffset = paddingStart
        val firstView = getChildAt(0)
        if (firstView.isVisible) {
            val left = (measuredWidth - firstView.measuredWidth) / 2
            val top = (measuredHeight - firstView.measuredHeight) / 2
            firstView.layout(left, top, left + firstView.measuredWidth, top + firstView.measuredHeight)
        }
        for (i in 1 until childCount) {
            val child = getChildAt(i)
            child.layout(itemOffset, paddingTop, itemOffset + itemWidth, paddingTop + child.measuredHeight)
            itemOffset += itemWidth
        }
    }

    override fun setOnDragListener(l: OnDragListener?) {
        super.setOnDragListener(l)
        for (i in 1 until childCount) {
            val child = getChildAt(i)
            child.setOnDragListener(l)
        }
    }

    override fun onLongClick(v: View): Boolean {
        val data = ClipData.newPlainText("", "")
        val shadowBuilder = DragShadowBuilder(v)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            v.startDragAndDrop(data, shadowBuilder, v, View.DRAG_FLAG_OPAQUE)
        } else {
            @Suppress("DEPRECATION")
            v.startDrag(data, shadowBuilder, v, 0)
        }
        v.context.clickVibrate()
        v.alpha = 0f
        return false
    }

    override fun generateDefaultLayoutParams(): MarginLayoutParams {
        return MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    override fun generateLayoutParams(p: LayoutParams): MarginLayoutParams {
        return MarginLayoutParams(p)
    }

    override fun generateLayoutParams(attrs: AttributeSet): MarginLayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    var apps: MutableList<BotInterface> = mutableListOf()
        set(value) {
            field = value
            render()
        }

    fun render() {
        val isEmpty = apps.isEmpty()
        currentShoveIndex = -1
        getChildAt(0).isVisible = isEmpty
        for (animator in animatorSet) {
            try {
                animator.cancel()
            } catch (e: Exception) {
            }
        }
        for (i in 1..4) {
            val avatar = getItemDock(getChildAt(i)).apply {
                avatar.alpha = 1f
                avatar.translationX = 0f
            }.avatar
            if (i - 1 < apps.size) {
                avatar.isVisible = true
                val app = apps[i - 1]
                avatar.renderApp(app)
                avatar.tag = app
                avatar.setOnLongClickListener(this@BotDock)
                avatar.setOnClickListener {
                    onDockListener?.onDockClick(app)
                }
            } else {
                avatar.isInvisible = true
                avatar.tag = null
                avatar.setOnLongClickListener(null)
                avatar.setOnClickListener(null)
            }
        }
        requestLayout()
    }

    fun addApp(position: Int, app: BotInterface) {
        if (!apps.contains(app) && apps.size < 4) {
            if (position < apps.size) {
                apps.add(position, app)
            } else {
                apps.add(app)
            }
            render()
            onDockListener?.onDockChange(apps)
        }
    }

    fun remove(app: BotInterface) {
        if (apps.contains(app)) {
            apps.remove(app)
            render()
            onDockListener?.onDockChange(apps)
        }
    }

    fun switch(index: Int, app: BotInterface) {
        if (apps.contains(app)) {
            apps.remove(app)
            if (index < apps.size) {
                apps.add(index, app)
            } else {
                apps.add(app)
            }
            render()
            onDockListener?.onDockChange(apps)
        }
    }

    private var currentShoveIndex = -1
    fun shove(index: Int) {
        if (apps.size in 1 until 4 && currentShoveIndex != index) {
            currentShoveIndex = index
            for (i in 1..4) {
                if (i <= index) {
                    viewPropertyAnimator(getItemDock(getChildAt(i)).avatar).translationX(0f).start()
                } else {
                    viewPropertyAnimator(getItemDock(getChildAt(i)).avatar).translationX(0f).translationX(itemWidth.toFloat()).start()
                }
            }
        }
    }

    fun shove(index: Int, bot: BotInterface) {
        val appIndex = apps.indexOf(bot)
        if (appIndex != -1 && currentShoveIndex != index) {
            currentShoveIndex = index
            when {
                appIndex + 1 > index -> {
                    for (i in 1..4) { // forward
                        if (i >= appIndex + 1 || i < index) {
                            viewPropertyAnimator(getItemDock(getChildAt(i)).avatar).translationX(0f).start()
                        } else {
                            viewPropertyAnimator(getItemDock(getChildAt(i)).avatar).translationX(0f).translationX(itemWidth.toFloat()).start()
                        }
                    }
                }
                appIndex + 1 < index -> { // backward
                    for (i in 1..4) {
                        if (i <= appIndex + 1 || i > index) {
                            viewPropertyAnimator(getItemDock(getChildAt(i)).avatar).translationX(0f).start()
                        } else {
                            viewPropertyAnimator(getItemDock(getChildAt(i)).avatar).translationX(0f).translationX(-itemWidth.toFloat()).start()
                        }
                    }
                }
                else -> { // hold
                    for (i in 1..4) {
                        viewPropertyAnimator(getItemDock(getChildAt(i)).avatar).translationX(0f).start()
                    }
                }
            }
        }
    }

    private val animatorSet = arraySetOf<ViewPropertyAnimatorCompat>()
    private fun viewPropertyAnimator(view: View) = ViewCompat.animate(view).setDuration(120).apply {
        val animator = this
        setListener(
            object : ViewPropertyAnimatorListener {
                override fun onAnimationStart(view: View) {
                    animatorSet.add(animator)
                }

                override fun onAnimationEnd(view: View) {
                    animatorSet.remove(animator)
                }

                override fun onAnimationCancel(view: View) {
                    view.translationX = 0f
                    animatorSet.remove(animator)
                }
            }
        )
    }

    private var onDockListener: OnDockListener? = null

    fun setOnDockListener(listener: OnDockListener) {
        this.onDockListener = listener
    }

    interface OnDockListener {
        fun onDockChange(apps: List<BotInterface>)

        fun onDockClick(app: BotInterface)
    }
}
