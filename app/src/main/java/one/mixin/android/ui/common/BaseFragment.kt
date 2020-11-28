package one.mixin.android.ui.common

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.viewbinding.ViewBinding
import com.uber.autodispose.android.lifecycle.scope

open class BaseFragment : Fragment() {
    protected val stopScope = scope(Lifecycle.Event.ON_STOP)
    protected val destroyScope = scope(Lifecycle.Event.ON_DESTROY)

    protected var baseBinding: ViewBinding? = null

    open fun onBackPressed() = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        interceptClick(view)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        baseBinding = null
    }

    private fun interceptClick(view: View) {
        view.setOnClickListener { }
    }
}
