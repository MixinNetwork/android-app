package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.core.widget.TextViewCompat
import load
import one.mixin.android.R
import one.mixin.android.databinding.ViewInscriptionBinding
import one.mixin.android.extension.clear
import one.mixin.android.extension.clearRound
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.roundTopOrBottom
import one.mixin.android.vo.safe.SafeCollectible
import one.mixin.android.vo.safe.SafeCollection
import timber.log.Timber

@SuppressLint("RestrictedApi")
class InscriptionView(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    private val binding: ViewInscriptionBinding = ViewInscriptionBinding.inflate(LayoutInflater.from(context), this)

    private var iconSize: Float = 0f
    private var textGranularity: Int = 0
    private var textMaxTextSize: Int = 0
    private var textMinTextSize: Int = 0
    private var textMarginHorizon: Int = 0
    private var textMarginVertical: Int = 0
    private var textMaxLines: Int = 0

    init {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        try {
            val attrArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.InscriptionView, 0, 0)
            iconSize = attrArray.getDimension(R.styleable.InscriptionView_iconSize, 0f)
            textGranularity = attrArray.getInt(R.styleable.InscriptionView_textGranularity, 0)
            textMaxTextSize = attrArray.getInt(R.styleable.InscriptionView_textMaxTextSize, 0)
            textMinTextSize = attrArray.getInt(R.styleable.InscriptionView_textMinTextSize, 0)
            textMaxLines = attrArray.getInt(R.styleable.InscriptionView_textMaxLines, 0)
            textMarginHorizon = attrArray.getDimension(R.styleable.InscriptionView_textMarginHorizon, 0f).toInt()
            textMarginVertical = attrArray.getDimension(R.styleable.InscriptionView_textMarginVertical, 0f).toInt()

            binding.textView.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setAutoSizeTextTypeWithDefaults(TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
                    setAutoSizeTextTypeUniformWithConfiguration(textMinTextSize, textMaxTextSize, 1, TypedValue.COMPLEX_UNIT_SP)
                }
            }
            binding.textView.layoutParams = (binding.textView.layoutParams as MarginLayoutParams).apply {
                marginStart = textMarginHorizon
                marginEnd = textMarginHorizon
                topMargin = textMarginVertical
            }
            binding.textView.maxLines = textMaxLines
            binding.icon.roundTopOrBottom(8.dp.toFloat(), top = true, bottom = false)
            attrArray.recycle()
        } catch (e: Exception) {
            Timber.e("InscriptionView", "Error initializing InscriptionView", e)
        }
    }

    fun render(inscriptionItem: SafeCollectible) {
        if (inscriptionItem.isText) {
            val lp = binding.icon.layoutParams
            lp.width = 50.dp
            lp.height = 50.dp
            binding.icon.layoutParams = lp
            binding.root.setBackgroundResource(R.drawable.bg_text_inscirption)
            binding.icon.setImageResource(R.drawable.ic_inscription_mao)
            binding.textView.isVisible = true
            binding.textView.load(inscriptionItem.contentURL)
        } else {
            val lp = binding.icon.layoutParams
            lp.width = LayoutParams.MATCH_PARENT
            lp.height = LayoutParams.MATCH_PARENT
            binding.icon.layoutParams = lp
            binding.root.setBackgroundColor(Color.TRANSPARENT)
            binding.icon.loadImage(data = inscriptionItem.contentURL, holder = R.drawable.ic_default_inscription)
            binding.textView.isVisible = false
        }
    }

    fun render(inscriptionCollection: SafeCollection) {
        val lp = binding.icon.layoutParams
        lp.width = LayoutParams.MATCH_PARENT
        lp.height = LayoutParams.MATCH_PARENT
        binding.icon.layoutParams = lp
        binding.root.setBackgroundColor(Color.TRANSPARENT)
        binding.icon.loadImage(data = inscriptionCollection.iconURL, holder = R.drawable.ic_default_inscription)
        binding.textView.isVisible = false
    }
}
