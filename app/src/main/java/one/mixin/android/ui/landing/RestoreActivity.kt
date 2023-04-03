package one.mixin.android.ui.landing

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.ActivityContactBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.replaceFragment
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class RestoreActivity : BaseActivity() {

    private val binding by viewBinding(ActivityContactBinding::inflate)

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        defaultSharedPreferences.putBoolean(Constants.Account.PREF_RESTORE, true)
        replaceFragment(RestoreFragment.newInstance(), R.id.container, RestoreFragment.TAG)
    }

    companion object {
        fun show(context: Context) {
            context.startActivity(Intent(context, RestoreActivity::class.java))
        }
    }
}
