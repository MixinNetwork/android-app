package one.mixin.android.ui.address

import android.content.Context
import android.content.Intent
import android.os.Bundle
import one.mixin.android.R
import one.mixin.android.extension.replaceFragment
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.vo.AssetItem

class AddressActivity : BaseActivity() {

    companion object {
        const val TAG = "AddressActivity"

        const val ARGS_SHOW_ADD = "args_show_add"

        fun show(context: Context, showAdd: Boolean, asset: AssetItem) {
            Intent(context, AddressActivity::class.java).apply {
                putExtra(ARGS_SHOW_ADD, showAdd)
                putExtra(ARGS_ASSET, asset)
            }.run { context.startActivity(this) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        replaceFragment(AddressManagementFragment.newInstance(intent.extras!!.getParcelable(ARGS_ASSET)!!),
            R.id.container, AddressManagementFragment.TAG)
    }
}
