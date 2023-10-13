package one.mixin.android.ui.setting.diagnosis

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.ActivityContactBinding
import one.mixin.android.extension.replaceFragment
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class DiagnosisActivity : BaseActivity() {

    private val binding by viewBinding(ActivityContactBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        replaceFragment(DiagnosisFragment.newInstance(), R.id.container, DiagnosisFragment.TAG)
    }

    companion object {
        fun show(context: Context) {
            context.startActivity(Intent(context, DiagnosisActivity::class.java))
        }
    }
}
