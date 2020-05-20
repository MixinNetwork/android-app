package one.mixin.android.widget

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import one.mixin.android.widget.gallery.MimeType

class ContentEditText : AppCompatEditText {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var listener: OnCommitContentListener? = null

    private val mimeTypes = arrayOf(MimeType.PNG.toString(), MimeType.GIF.toString(), MimeType.JPEG.toString(), MimeType.JPG.toString(), MimeType.WEBP.toString(), MimeType.HEIC.toString())
    override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection {
        val ic = super.onCreateInputConnection(editorInfo)
        if (listener == null) {
            return ic
        }

        EditorInfoCompat.setContentMimeTypes(editorInfo, mimeTypes)
        val callback = InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, opts ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && (flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION != 0)) {
                try {
                    inputContentInfo.requestPermission()
                } catch (e: Exception) {
                    return@OnCommitContentListener false
                }
            }

            var supported = false
            for (mimeType in mimeTypes) {
                if (inputContentInfo.description.hasMimeType(mimeType)) {
                    supported = true
                    break
                }
            }
            if (!supported) {
                return@OnCommitContentListener false
            }
            if (this.listener != null) {
                return@OnCommitContentListener this.listener!!.onCommitContent(inputContentInfo, flags, opts)
            }
            return@OnCommitContentListener false
        }
        return InputConnectionCompat.createWrapper(ic, editorInfo, callback)
    }

    fun setCommitContentListener(listener: OnCommitContentListener) {
        this.listener = listener
    }

    interface OnCommitContentListener {
        fun onCommitContent(inputContentInfo: InputContentInfoCompat?, flags: Int, opts: Bundle?): Boolean
    }
}
