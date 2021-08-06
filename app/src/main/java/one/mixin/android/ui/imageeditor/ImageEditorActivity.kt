package one.mixin.android.ui.imageeditor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import one.mixin.android.R
import one.mixin.android.databinding.ActivityContactBinding
import one.mixin.android.extension.replaceFragment
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.imageeditor.ImageEditorFragment.Companion.ARGS_IMAGE_URI
import one.mixin.android.util.viewBinding

class ImageEditorActivity : BaseActivity() {

    private val binding by viewBinding(ActivityContactBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val imageUri = intent.getParcelableExtra<Uri>(ARGS_IMAGE_URI)
        replaceFragment(ImageEditorFragment.newInstance(requireNotNull(imageUri)), R.id.container, ImageEditorFragment.TAG)
    }

    class ImageEditorContract : ActivityResultContract<Pair<String, Boolean>, Intent?>() {
        override fun createIntent(context: Context, input: Pair<String, Boolean>): Intent {
            return Intent(context, ImageEditorActivity::class.java).apply {
                putExtra(input.first, input.second)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Intent? {
            if (intent == null || resultCode != Activity.RESULT_OK) return null
            return intent
        }
    }

    companion object {
        fun show(context: Context, imageUri: Uri) {
            context.startActivity(
                Intent(context, ImageEditorActivity::class.java).apply {
                    putExtra(ARGS_IMAGE_URI, imageUri)
                }
            )
        }
    }
}
