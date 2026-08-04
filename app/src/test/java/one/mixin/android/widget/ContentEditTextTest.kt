package one.mixin.android.widget

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.test.core.app.ApplicationProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContentEditTextTest {
    @Test
    fun pasteImageFromClipboardCommitsContent() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val editText = ContentEditText(context)
        val imageUri = Uri.parse("content://one.mixin.android.test/image.png")
        var committedUri: Uri? = null
        var committedOpts: Bundle? = null

        editText.setCommitContentListener(
            object : ContentEditText.OnCommitContentListener {
                override fun commitContentAsync(
                    inputContentInfo: InputContentInfoCompat?,
                    flags: Int,
                    opts: Bundle?,
                ) {
                    committedUri = inputContentInfo?.contentUri
                    committedOpts = opts
                }
            },
        )

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData(
                ClipDescription("image", arrayOf("image/png")),
                ClipData.Item(imageUri),
            ),
        )

        val handled = editText.onTextContextMenuItem(android.R.id.paste)

        assertTrue(handled)
        assertEquals(imageUri, committedUri)
        assertTrue(committedOpts?.getBoolean(ContentEditText.OPTION_FROM_CLIPBOARD) == true)
        assertEquals("image/png", committedOpts?.getString(ContentEditText.OPTION_CONTENT_MIME_TYPE))
    }
}
