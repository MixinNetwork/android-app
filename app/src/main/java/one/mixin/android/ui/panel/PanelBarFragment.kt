package one.mixin.android.ui.panel

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import androidx.core.view.updateLayoutParams
import one.mixin.android.R
import one.mixin.android.extension.adjustAlpha
import one.mixin.android.extension.displaySize
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationFragment.Companion.COVER_MAX_ALPHA
import one.mixin.android.widget.PanelBarView
import one.mixin.android.widget.keyboard.InputAwareFrameLayoutLayout
import kotlin.math.max
import kotlin.math.min

abstract class PanelBarFragment : BaseFragment() {

    protected var maxHeight = 0
        set(value) {
            field = value
            middleHeight = (maxHeight + minHeight) / 2
        }
    var minHeight = 0
        set(value) {
            field = value
            middleHeight = (maxHeight + minHeight) / 2
        }
    protected var middleHeight = 0

    var expanded = false

    private lateinit var panelBar: PanelBarView
    private lateinit var panelContent: View
    private lateinit var cover: View

    private val panelBarCallback = object : PanelBarView.Callback {
        override fun onDrag(dis: Float) {
            panelContent.let {
                val h = it.height - dis.toInt()
                if (h in minHeight..maxHeight) {
                    it.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = h
                    }
                } else if (h < minHeight) {
                    collapse()
                }
            }
        }

        override fun onRelease() {
            panelContent.let {
                val h = it.height
                when {
                    h < middleHeight -> collapse()
                    else -> expand()
                }
            }
        }

        override fun onClick() {
            collapse()
        }

        override fun onTap() {
            onTapPanelBar()
        }
    }

    fun expand() {
        expanded = true
        panelBar.visibility = VISIBLE
        beforeExpand()
        animHeight(maxHeight)
    }

    fun collapse() {
        expanded = false
        beforeCollapse()
        animHeight(minHeight)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        maxHeight = (context.displaySize().y.toFloat() - context.statusBarHeight()).toInt()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = layoutInflater.inflate(getLayoutId(), container, false)
        panelBar = v.findViewById(R.id.panel_bar) ?: throw IllegalArgumentException("PanelBarFragment's subclass must have PanelBarView")
        panelContent = v.findViewById(R.id.panel_content) ?: throw IllegalArgumentException("PanelBarFragment's subclass must have panel content")
        cover = v.findViewById(R.id.panel_cover) ?: throw IllegalArgumentException("PanelBarFragment's subclass must have cover")

        panelBar.maxDragDistance = (maxHeight - minHeight).toFloat()
        panelContent.updateLayoutParams<ViewGroup.LayoutParams> {
            height = minHeight
        }
        panelBar.callback = panelBarCallback
        panelBar.visibility = GONE
        return v
    }

    abstract fun getLayoutId(): Int

    protected open fun beforeExpand() {
    }

    protected open fun beforeCollapse() {
    }

    protected open fun afterExpand() {
    }

    protected open fun afterCollapse() {
    }

    protected open fun onTapPanelBar() {
        // Left empty for override
    }

    private fun animHeight(targetH: Int) {
        panelContent.let { v ->
            val curH = v.height
            ValueAnimator.ofInt(curH, targetH).apply {
                interpolator = DecelerateInterpolator()
                addUpdateListener { va ->
                    v.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = (va.animatedValue as Int).apply {
                            cover.alpha = if (curH > targetH) {
                                max(0f, min(this.toFloat() / targetH - 1f, COVER_MAX_ALPHA))
                            } else {
                                var a = (this.toFloat() - curH) / (targetH - curH)
                                a = if (java.lang.Float.isNaN(a)) {
                                    0f
                                } else {
                                    min(COVER_MAX_ALPHA, (this.toFloat() - curH) / (targetH - curH))
                                }
                                a
                            }
                            val coverColor = (cover.background as ColorDrawable).color
                            activity?.window?.statusBarColor = coverColor.adjustAlpha(cover.alpha)
                        }
                    }
                }
                val expand = targetH != minHeight
                doOnEnd {
                    cover.visibility = if (!expand) GONE else VISIBLE
                    if (expand) {
                        afterExpand()
                    } else {
                        panelBar.visibility = GONE
                        afterCollapse()
                    }
                }
                doOnCancel {
                    cover.visibility = if (!expand) GONE else VISIBLE
                    if (expand) {
                        afterExpand()
                    } else {
                        panelBar.visibility = GONE
                        afterCollapse()
                    }
                }
                start()
            }
        }
    }
}