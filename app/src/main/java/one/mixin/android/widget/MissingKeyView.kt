package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import one.mixin.android.R
import one.mixin.android.databinding.ViewMissingKeyBinding
import one.mixin.android.extension.highlightStarTag

class MissingKeyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewMissingKeyBinding

    init {
        binding = ViewMissingKeyBinding.inflate(LayoutInflater.from(context), this)
        orientation = VERTICAL
    }

    fun setMissingKey(isMnemonic: Boolean, onImportClick: () -> Unit) {
        binding.importKeyBtn.text = context.getString(
            if (isMnemonic) R.string.Import_Mnemonic_Phrase else R.string.import_private_key
        )
        
        val learn = context.getString(R.string.Learn_More)
        val info = context.getString(
            if (isMnemonic) R.string.Import_Mnemonic_Phrase_Desc else R.string.Import_Private_Key_Desc,
            "**$learn**"
        )
        val learnUrl = context.getString(
            if (isMnemonic) R.string.import_mnemonic_phrase_url else R.string.import_private_key_url
        )
        
        binding.missingKeyTv.highlightStarTag(info, arrayOf(learnUrl))
        binding.importKeyBtn.setOnClickListener { onImportClick() }
    }

}
