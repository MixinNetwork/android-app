package one.mixin.android.ui.common

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.uber.autodispose.android.lifecycle.scope
import one.mixin.android.di.Injectable
import javax.inject.Inject

open class BaseFragment : Fragment(), Injectable {
    protected val stopScope = scope(Lifecycle.Event.ON_STOP)
    protected val destroyScope = scope(Lifecycle.Event.ON_DESTROY)

    open fun onBackPressed() = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        interceptClick(view)
    }

    private fun interceptClick(view: View) {
        view.setOnClickListener { }
    }
}
