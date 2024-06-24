package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import one.mixin.android.R
import one.mixin.android.databinding.ViewInscriptionBinding
import one.mixin.android.extension.clearRound
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.round
import one.mixin.android.extension.roundLeftOrRight
import one.mixin.android.extension.roundTopOrBottom
import one.mixin.android.util.load
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.safe.SafeCollectible
import one.mixin.android.vo.safe.SafeCollection
import timber.log.Timber

@SuppressLint("RestrictedApi")
class InscriptionView(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    private val binding: ViewInscriptionBinding = ViewInscriptionBinding.inflate(LayoutInflater.from(context), this)

    private var iconSize: Int = 0
    private var textGranularity: Int = 0
    private var textMaxTextSize: Int = 0
    private var textMinTextSize: Int = 0
    private var textMarginHorizon: Int = 0
    private var textMarginVertical: Int = 0
    private var textMaxLines: Int = 0
    private var roundSize: Float = 0f
    private var roundMode: Int = 0

    init {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        try {
            val attrArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.InscriptionView, 0, 0)
            roundMode = attrArray.getInt(R.styleable.InscriptionView_iconRound, 0)
            iconSize = attrArray.getDimension(R.styleable.InscriptionView_iconSize, 0f).toInt()
            roundSize = attrArray.getDimension(R.styleable.InscriptionView_iconRoundSize, 0f)
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
            attrArray.recycle()
        } catch (e: Exception) {
            Timber.e("InscriptionView", "Error initializing InscriptionView", e)
        }
    }

    fun render(inscriptionItem: SafeCollectible?) {
        binding.apply {
            round(root)
            if (inscriptionItem == null) {
                val lp = icon.layoutParams
                lp.width = LayoutParams.MATCH_PARENT
                lp.height = LayoutParams.MATCH_PARENT
                icon.layoutParams = lp
                background.setBackgroundColor(Color.TRANSPARENT)
                icon.setImageResource(R.drawable.ic_default_inscription)
                textView.isVisible = false
            } else if (inscriptionItem.isText) {
                val lp = icon.layoutParams
                lp.width = iconSize
                lp.height = iconSize
                icon.layoutParams = lp
                background.setBackgroundResource(R.drawable.bg_text_inscirption)
                icon.setImageResource(R.drawable.ic_text_inscription)
                textView.isVisible = true
                textView.load(inscriptionItem.contentURL)
            } else {
                val lp = icon.layoutParams
                lp.width = LayoutParams.MATCH_PARENT
                lp.height = LayoutParams.MATCH_PARENT
                icon.layoutParams = lp
                background.setBackgroundColor(Color.TRANSPARENT)
                icon.loadImage(data = inscriptionItem.contentURL, holder = R.drawable.ic_default_inscription)
                textView.isVisible = false
            }
        }
    }


    fun render(inscriptionCollection: SafeCollection) {
        binding.apply {
            round(root)
            val lp = icon.layoutParams
            lp.width = LayoutParams.MATCH_PARENT
            lp.height = LayoutParams.MATCH_PARENT
            icon.layoutParams = lp
            background.setBackgroundColor(Color.TRANSPARENT)
            icon.loadImage(data = inscriptionCollection.iconURL, holder = R.drawable.ic_default_inscription)
            textView.isVisible = false
        }
    }

    fun render(snapshot: SnapshotItem) {
        binding.apply {
            round(root)
            if (snapshot.contentType?.startsWith("text", true) == true) {
                val lp = icon.layoutParams
                lp.width = iconSize
                lp.height = iconSize
                icon.layoutParams = lp
                background.setBackgroundResource(R.drawable.bg_text_inscirption)
                icon.setImageResource(R.drawable.ic_text_inscription)
                textView.isVisible = true
                textView.load(snapshot.contentUrl)
            } else {
                val lp = icon.layoutParams
                lp.width = LayoutParams.MATCH_PARENT
                lp.height = LayoutParams.MATCH_PARENT
                icon.layoutParams = lp
                background.setBackgroundColor(Color.TRANSPARENT)
                icon.loadImage(data = snapshot.contentUrl, holder = R.drawable.ic_default_inscription)
                textView.isVisible = false
            }
        }
    }

    private fun round(root: View) {
        when (roundMode) {
            1 -> { // all
                root.round(roundSize)
            }

            2 -> { // top
                root.roundTopOrBottom(roundSize, top = true, bottom = false)
            }

            3 -> { // start
                root.roundLeftOrRight(roundSize, left = true, right = false)
            }

            else -> { // none
                root.clearRound()
            }
        }
    }
}
