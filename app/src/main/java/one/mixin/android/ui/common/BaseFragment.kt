package one.mixin.android.ui.common

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.uber.autodispose.android.lifecycle.scope

open class BaseFragment : Fragment {
    constructor() : super()
    constructor(@LayoutRes contentLayoutId: Int) : super(contentLayoutId)

    protected val stopScope = scope(Lifecycle.Event.ON_STOP)
    protected val pauseScope = scope(Lifecycle.Event.ON_PAUSE)
    protected val destroyScope = scope(Lifecycle.Event.ON_DESTROY)

    open fun onBackPressed() = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        interceptClick(view)
    }

    private fun interceptClick(view: View) {
        view.setOnClickListener { }
    }
}
