package one.mixin.android.ui.home.inscription

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.extension.getParcelableExtraCompat
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAssetsJob
import one.mixin.android.job.SyncInscriptionCollectionJob
import one.mixin.android.ui.common.BlazeBaseActivity
import one.mixin.android.vo.safe.SafeCollection
import javax.inject.Inject

@AndroidEntryPoint
class CollectionActivity : BlazeBaseActivity() {
    companion object {
        const val ARGS_COLLECTION = "args_collection"

        fun show(
            context: Context,
            collection: SafeCollection,
        ) {
            Intent(context, CollectionActivity::class.java).apply {
                putExtra(ARGS_COLLECTION, collection)
            }.run {
                context.startActivity(this)
            }
        }
    }

    @Inject
    lateinit var jobManager: MixinJobManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        val collection = intent.getParcelableExtraCompat(ARGS_COLLECTION, SafeCollection::class.java)!!
        jobManager.addJobInBackground(SyncInscriptionCollectionJob(collection.collectionHash))
        supportFragmentManager.beginTransaction().add(
            R.id.container,
            CollectionFragment.newInstance(collection),
            CollectionFragment.TAG,
        ).commit()
    }
}
