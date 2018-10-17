package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.IdRes

class RadioGroup(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    private val passThroughListener = PassThroughHierarchyChangeListener()

    init {
        super.setOnHierarchyChangeListener(passThroughListener)
    }

    inner class PassThroughHierarchyChangeListener : ViewGroup.OnHierarchyChangeListener {
        private val mOnHierarchyChangeListener: ViewGroup.OnHierarchyChangeListener? = null

        override fun onChildViewAdded(parent: View, child: View) {
            if (parent === this@RadioGroup && child is RadioButton) {
                var id = child.getId()
                // generates an id if it's missing
                if (id == View.NO_ID) {
                    id = View.generateViewId()
                    child.setId(id)
                } else {
                    child.setOnCheckedChangeListener(object : RadioButton.OnCheckedChangeListener {
                        override fun onCheckedChanged(id: Int, checked: Boolean) {
                            if (checked) {
                                update(id)
                                onCheckedListener?.onCheced(id)
                            }
                        }
                    })
                }
            }

            mOnHierarchyChangeListener?.onChildViewAdded(parent, child)
        }

        override fun onChildViewRemoved(parent: View, child: View) {
            if (parent === this@RadioGroup && child is RadioButton) {
                child.setOnCheckedChangeListener(null)
            }

            mOnHierarchyChangeListener?.onChildViewRemoved(parent, child)
        }
    }

    fun getCheckedId(): Int {
        return this.currentId
    }

    private var currentId = View.NO_ID

    fun update(id: Int) {
        this.currentId = id
        for (i in 0 until childCount) {
            getChildAt(i).let {
                if (it.id != id && it is RadioButton) {
                    it.isChecked = false
                }
            }
        }
    }

    fun setCheckedById(@IdRes id: Int) {
        this.currentId = id
        for (i in 0 until childCount) {
            getChildAt(i).let {
                if (it.id == id && it is RadioButton) {
                    it.isChecked = true
                }
            }
        }
    }

    private var onCheckedListener: OnCheckedListener? = null

    fun setOnCheckedListener(onCheckedListener: OnCheckedListener) {
        this.onCheckedListener = onCheckedListener
    }

    interface OnCheckedListener {
        fun onCheced(id: Int)
    }
}