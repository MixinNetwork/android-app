package one.mixin.android.ui.home.web3.swap

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.ActivityContactBinding
import one.mixin.android.extension.replaceFragment
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.home.web3.swap.SwapFragment.Companion.ARGS_AMOUNT
import one.mixin.android.ui.home.web3.swap.SwapFragment.Companion.ARGS_INPUT
import one.mixin.android.ui.home.web3.swap.SwapFragment.Companion.ARGS_OUTPUT
import one.mixin.android.ui.home.web3.swap.SwapFragment.Companion.ARGS_REFERRAL
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.safe.TokenItem

@AndroidEntryPoint
class SwapActivity : BaseActivity(){
    companion object {
        fun show(
            context: Context,
            input: String?,
            output: String?,
            amount: String?,
            referral: String?,
        ) {
            context.startActivity(
                Intent(context, SwapActivity::class.java).apply {
                    input?.let { putExtra(ARGS_INPUT, it) }
                    output?.let { putExtra(ARGS_OUTPUT, it) }
                    amount?.let { putExtra(ARGS_AMOUNT, it) }
                    referral?.let { putExtra(ARGS_REFERRAL, it) }
                    setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                },
            )
        }
    }

    private val binding by viewBinding(ActivityContactBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val swapFragment = SwapFragment.newInstance<TokenItem>(
            null,
            intent.getStringExtra(ARGS_INPUT),
            intent.getStringExtra(ARGS_OUTPUT),
            intent.getStringExtra(ARGS_AMOUNT),
            referral = intent.getStringExtra(ARGS_REFERRAL),
        )
        replaceFragment(swapFragment, R.id.container, SwapFragment.TAG)}
}