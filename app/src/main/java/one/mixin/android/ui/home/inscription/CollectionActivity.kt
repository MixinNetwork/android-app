package one.mixin.android.ui.home.inscription

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.ui.common.BlazeBaseActivity

@AndroidEntryPoint
class CollectionActivity : BlazeBaseActivity() {
    companion object {
        const val ARGS_HASH = "args_hash"

        fun show(
            context: Context,
            collectionHash: String,
        ) {
            Intent(context, CollectionActivity::class.java).apply {
                putExtra(ARGS_HASH, collectionHash)
            }.run {
                context.startActivity(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        supportFragmentManager.beginTransaction().add(
            R.id.container,
            CollectionFragment.newInstance(requireNotNull(intent.getStringExtra(ARGS_HASH))),
            CollectionFragment.TAG,
        ).commit()
    }
}
