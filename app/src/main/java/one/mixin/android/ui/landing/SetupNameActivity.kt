package one.mixin.android.ui.landing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import one.mixin.android.R
import one.mixin.android.extension.replaceFragment
import one.mixin.android.ui.common.BaseActivity

class SetupNameActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)
        val setName = intent.getBooleanExtra(SET_NAME, false)
        if (setName) {
            replaceFragment(SetupNameFragment.newInstance(), R.id.container)
        } else {
            replaceFragment(LoadingFragment.newInstance(), R.id.container, LoadingFragment.TAG)
        }
    }

    override fun onBackPressed() {
    }

    companion object {
        const val SET_NAME = "set_name"
        fun getIntent(context: Context, setName: Boolean): Intent {
            return Intent(context, SetupNameActivity::class.java).apply {
                this.putExtra(SET_NAME, setName)
            }
        }
    }
}