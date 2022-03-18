package one.mixin.android.widget

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Spanned
import android.text.method.KeyListener
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
import android.view.inputmethod.InputConnection
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.emoji2.viewsintegration.EmojiEditTextHelper
import one.mixin.android.Constants
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.supportsOreo
import one.mixin.android.widget.gallery.MimeType

open class ContentEditText : AppCompatEditText {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private val emojiEditTextHelper = EmojiEditTextHelper(this)

    init {
        supportsOreo {
            val incognitoKeyboardEnabled = context.defaultSharedPreferences.getBoolean(
                Constants.Account.PREF_INCOGNITO_KEYBOARD,
                false
            )
            imeOptions = if (incognitoKeyboardEnabled) {
                imeOptions or IME_FLAG_NO_PERSONALIZED_LEARNING
            } else {
                imeOptions and IME_FLAG_NO_PERSONALIZED_LEARNING.inv()
            }
        }
        super.setKeyListener(emojiEditTextHelper.getKeyListener(keyListener))
    }

    override fun setKeyListener(keyListener: KeyListener?) {
        super.setKeyListener(emojiEditTextHelper.getKeyListener(keyListener))
    }

    var listener: OnCommitContentListener? = null

    private val mimeTypes = arrayOf(
        MimeType.PNG.toString(),
        MimeType.GIF.toString(),
        MimeType.JPEG.toString(),
        MimeType.JPG.toString(),
        MimeType.WEBP.toString(),
        MimeType.HEIC.toString()
    )

    override fun onTextContextMenuItem(id: Int): Boolean {
        if (id == android.R.id.paste) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return super.onTextContextMenuItem(android.R.id.pasteAsPlainText)
            } else {
                onInterceptClipDataToPlainText()
            }
        }
        return super.onTextContextMenuItem(id)
    }

    private fun onInterceptClipDataToPlainText() {
        val clipboard: ClipboardManager = context?.getClipboardManager() ?: return
        val clip: ClipData? = clipboard.primaryClip
        if (clip != null) {
            for (i in 0 until clip.itemCount) {
                val paste: CharSequence
                val text = clip.getItemAt(i).coerceToText(context)
                paste = (text as? Spanned)?.toString() ?: text
                if (paste != null) {
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText(null, paste)
                    )
                }
            }
        }
    }

    override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection? {
        val ic = emojiEditTextHelper.onCreateInputConnection(
            super.onCreateInputConnection(editorInfo),
            editorInfo
        )
        if (listener == null || ic == null) {
            return ic
        }

        EditorInfoCompat.setContentMimeTypes(editorInfo, mimeTypes)
        val callback =
            InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, opts ->
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
                    this.listener!!.commitContentAsync(inputContentInfo, flags, opts)
                    return@OnCommitContentListener true
                }
                return@OnCommitContentListener false
            }

        return InputConnectionCompat.createWrapper(ic, editorInfo, callback)
    }

    fun setCommitContentListener(listener: OnCommitContentListener) {
        this.listener = listener
    }

    interface OnCommitContentListener {
        fun commitContentAsync(
            inputContentInfo: InputContentInfoCompat?,
            flags: Int,
            opts: Bundle?
        )
    }
}
