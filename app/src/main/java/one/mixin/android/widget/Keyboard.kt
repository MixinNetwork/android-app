package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_grid_keyboard.view.*
import kotlinx.android.synthetic.main.view_keyboard.view.*
import one.mixin.android.R

class Keyboard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private var onClickKeyboardListener: OnClickKeyboardListener? = null
    private var key: Array<String>? = null

    private val keyboardAdapter = object : RecyclerView.Adapter<KeyboardHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeyboardHolder {
            return KeyboardHolder(
                if (viewType == 1) {
                    LayoutInflater.from(context)
                        .inflate(R.layout.item_grid_keyboard, parent, false)
                } else {
                    LayoutInflater.from(context)
                        .inflate(R.layout.item_grid_keyboard_delete, parent, false)
                }
            )
        }

        override fun getItemCount(): Int = key!!.size

        override fun onBindViewHolder(holder: KeyboardHolder, position: Int) {
            if (getItemViewType(position) == 1) {
                holder.bind(key!![position])
            }

            holder.itemView.setOnClickListener { _ ->
                if (onClickKeyboardListener != null) {
                    onClickKeyboardListener!!.onKeyClick(position, key!![position])
                }
            }
            holder.itemView.setOnLongClickListener { _ ->
                if (onClickKeyboardListener != null) {
                    onClickKeyboardListener!!.onLongClick(position, key!![position])
                }
                true
            }
        }

        private val KEY_NINE = 11

        override fun getItemViewType(position: Int): Int {
            return if (position == KEY_NINE) 0 else 1
        }
    }

    class KeyboardHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(text: String?) {
            itemView.tv_keyboard_keys.text = text
            itemView.isEnabled = !text.isNullOrEmpty()
        }
    }

    /**
     * 初始化KeyboardView
     */
    private fun initKeyboardView() {
        View.inflate(context, R.layout.view_keyboard, this)
        gv_keyboard.adapter = keyboardAdapter
        gv_keyboard.layoutManager = GridLayoutManager(context, 3)
        gv_keyboard.addItemDecoration(SpacesItemDecoration(1))
    }

    interface OnClickKeyboardListener {
        fun onKeyClick(position: Int, value: String)

        fun onLongClick(position: Int, value: String)
    }

    fun setOnClickKeyboardListener(onClickKeyboardListener: OnClickKeyboardListener) {
        this.onClickKeyboardListener = onClickKeyboardListener
    }

    /**
     * 设置键盘所显示的内容
     *
     * @param key
     */
    fun setKeyboardKeys(key: Array<String>) {
        this.key = key
        initKeyboardView()
    }
}
