package one.mixin.android.ui.sticker

import android.app.Activity
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.extension.replaceFragment
import one.mixin.android.ui.common.BaseActivity

@AndroidEntryPoint
class StickerActivity : BaseActivity() {

    companion object {
        const val TAG = "StickerActivity"
        const val ARGS_SHOW_ADD = "args_show_add"
        const val ARGS_PERSONAL_ALBUM_ID = "args_personal_album_id"
        const val ARGS_URL = "args_url"

        fun show(
            context: Activity,
            personalAlbumId: String? = null,
            url: String? = null,
            showAdd: Boolean = false
        ) {
//            Intent(context, StickerActivity::class.java).apply {
//                putExtra(ARGS_PERSONAL_ALBUM_ID, personalAlbumId)
//                putExtra(ARGS_URL, url)
//                putExtra(ARGS_SHOW_ADD, showAdd)
//            }.run {
//                context.startActivity(this)
//            }
            StickerShopActivity.show(context)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        if (intent.hasExtra(ARGS_SHOW_ADD) && intent.getBooleanExtra(ARGS_SHOW_ADD, false)) {
            val url = intent.getStringExtra(ARGS_URL)
            require(url != null)
            replaceFragment(
                StickerAddFragment.newInstance(url),
                R.id.container,
                StickerAddFragment.TAG
            )
        } else {
            replaceFragment(
                StickerManagementFragment.newInstance(intent.getStringExtra(ARGS_PERSONAL_ALBUM_ID)),
                R.id.container,
                StickerManagementFragment.TAG
            )
        }
    }
}
