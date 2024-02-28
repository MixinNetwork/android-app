package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.ViewBlurOverlayBinding
import one.mixin.android.extension.blurBitmap
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.textColor
import one.mixin.android.extension.dp
import one.mixin.android.extension.round
import one.mixin.android.extension.supportsS

class BlurOverlayView : ConstraintLayout {

    private val _binding: ViewBlurOverlayBinding

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        _binding = ViewBlurOverlayBinding.inflate(LayoutInflater.from(context), this)
        _binding.content.layoutManager = GridLayoutManager(context, 4)
        _binding.content.adapter = contentAdapter
        _binding.content.addItemDecoration(SpacesItemDecoration(10.dp))
        _binding.content.round(8.dp)
        _binding.overlay.round(8.dp)
    }

    private val contentAdapter by lazy {
        ContentAdapter()
    }

    @SuppressLint("SetTextI18n")
    fun setKey(key: String) {
        _binding.apply {
            contentAdapter.content = key.chunked(6).toMutableList()

            _binding.content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    _binding.content.viewTreeObserver.removeOnPreDrawListener(this)
                    val screenBitmap =  _binding.content.drawToBitmap()
                    supportsS({
                        _binding.content.background = BitmapDrawable(resources, screenBitmap)
                        _binding.content.setRenderEffect(RenderEffect.createBlurEffect(25f, 25f, Shader.TileMode.MIRROR))
                    }, {
                        _binding.content.background = BitmapDrawable(resources, screenBitmap.blurBitmap(25))
                    })
                    return true
                }
            })
            _binding.overlay.isVisible = true
            _binding.overlay.setOnClickListener {
                _binding.overlay.isVisible = false
            }
        }
    }

    class ContentAdapter : RecyclerView.Adapter<TextHolder>() {
        var content = mutableListOf<String>()
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                if (field != value) {
                    field = value
                    notifyDataSetChanged()
                }
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextHolder {
            val textView = TextView(parent.context)
            textView.textSize = 18f
            textView.textColor = parent.context.colorAttr(R.attr.text_assist)
            textView.gravity = Gravity.CENTER_VERTICAL
            textView.typeface = ResourcesCompat.getFont(parent.context, R.font.roboto_regular)
            return TextHolder(textView)
        }

        override fun getItemCount(): Int {
            return content.size
        }

        override fun onBindViewHolder(holder: TextHolder, position: Int) {
            holder.bind(content[position])
        }
    }

    class TextHolder(textView: TextView) : RecyclerView.ViewHolder(textView) {
        fun bind(str: String) {
            (itemView as TextView).text = str
        }
    }
}