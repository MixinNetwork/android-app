package one.mixin.android.ui.common

import android.arch.lifecycle.Observer
import android.os.Bundle
import kotlinx.android.synthetic.main.view_link_state.*
import one.mixin.android.di.Injectable
import one.mixin.android.extension.animateHeight
import one.mixin.android.extension.dpToPx
import one.mixin.android.vo.LinkState
import javax.inject.Inject

open class LinkFragment : BaseFragment(), Injectable {
    @Inject
    lateinit var linkState: LinkState

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        linkState.observe(this, Observer { state ->
            check(state)
        })
    }

    private fun check(state: Int?) {
        if (LinkState.isOnline(state)) {
            state_layout.animateHeight(context!!.dpToPx(26f), 0)
        } else {
            state_layout.animateHeight(0, context!!.dpToPx(26f))
        }
    }
}