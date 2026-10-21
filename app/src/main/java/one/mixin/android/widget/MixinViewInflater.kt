package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.theme.MaterialComponentsViewInflater

class MixinViewInflater : MaterialComponentsViewInflater() {
    override fun createTextView(context: Context, attrs: AttributeSet): AppCompatTextView {
        return WideGamutTextView(context, attrs)
    }
}
