package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.databinding.ItemGridKeyboardBinding
import one.mixin.android.databinding.ViewKeyboardBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dpToPx
import one.mixin.android.session.Session

class Keyboard
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : RelativeLayout(context, attrs, defStyleAttr) {
        private var onClickKeyboardListener: OnClickKeyboardListener? = null
        private var key: List<String>? = null

        var tipTitleEnabled = true

        private val keyboardAdapter =
            object : RecyclerView.Adapter<KeyboardHolder>() {
                override fun onCreateViewHolder(
                    parent: ViewGroup,
                    viewType: Int,
                ): KeyboardHolder {
                    return if (viewType == 1) {
                        NormalKeyboardHolder(ItemGridKeyboardBinding.inflate(LayoutInflater.from(context), parent, false))
                    } else {
                        if (this@Keyboard.isWhite) {
                            KeyboardHolder(
                                LayoutInflater.from(context)
                                    .inflate(R.layout.item_grid_keyboard_delete_white, parent, false),
                            )
                        } else {
                            KeyboardHolder(
                                LayoutInflater.from(context)
                                    .inflate(R.layout.item_grid_keyboard_delete, parent, false),
                            )
                        }
                    }
                }

                override fun getItemCount(): Int = key!!.size

                override fun onBindViewHolder(
                    holder: KeyboardHolder,
                    position: Int,
                ) {
                    if (getItemViewType(position) == 1) {
                        (holder as NormalKeyboardHolder).bind(key!![position])
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

        open class KeyboardHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        class NormalKeyboardHolder(val binding: ItemGridKeyboardBinding) : KeyboardHolder(binding.root) {
            fun bind(text: String?) {
                binding.root.isVisible = text != ""
                binding.tvKeyboardKeys.text = text
                itemView.isEnabled = !text.isNullOrEmpty()
            }
        }

        private val binding = ViewKeyboardBinding.inflate(LayoutInflater.from(context), this, true)

        private fun initKeyboardView() {
            binding.gvKeyboard.adapter = keyboardAdapter
            binding.gvKeyboard.layoutManager = GridLayoutManager(context, 3)
            binding.gvKeyboard.addItemDecoration(SpacesItemDecoration(context.dpToPx(8f)))
            binding.tipFl.isVisible = tipTitleEnabled && Session.getTipPub().isNullOrBlank().not()
        }

        interface OnClickKeyboardListener {
            fun onKeyClick(
                position: Int,
                value: String,
            )

            fun onLongClick(
                position: Int,
                value: String,
            )
        }

        fun setOnClickKeyboardListener(onClickKeyboardListener: OnClickKeyboardListener) {
            this.onClickKeyboardListener = onClickKeyboardListener
        }

        fun initPinKeys(
            context: Context? = null,
            key: List<String>? = null,
            force: Boolean = false,
            white: Boolean = false,
        ) {
            if (white) {
                isWhite = white
                white(context ?: MixinApplication.appContext)
            }
            if (!force && context?.defaultSharedPreferences?.getBoolean(Constants.Account.PREF_RANDOM, false) == true) {
                val list = mutableListOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
                list.shuffle()
                list.add(9, "")
                list.add("<<")
                this.key = list
                initKeyboardView()
            } else {
                this.key = key ?: listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "<<")
                initKeyboardView()
            }
        }

        var isWhite = false
            private set

        fun white(context: Context) {
            binding.gvKeyboard.setBackgroundColor(context.colorFromAttribute(R.attr.bg_white))
            binding.diver.isVisible = false
        }

        fun disableNestedScrolling() {
            binding.gvKeyboard.isNestedScrollingEnabled = false
        }
    }
