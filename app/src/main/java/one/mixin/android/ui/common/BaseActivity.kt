package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import one.mixin.android.R
import javax.inject.Inject

@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity(), HasSupportFragmentInjector {

    protected val scopeProvider: AndroidLifecycleScopeProvider by lazy { AndroidLifecycleScopeProvider.from(this) }

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    override fun supportFragmentInjector(): DispatchingAndroidInjector<Fragment>? =
        dispatchingAndroidInjector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 26) {
            window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR or
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    override fun onBackPressed() {
        val fragments = supportFragmentManager.fragments
        if (fragments != null && fragments.size > 0) {
            // Make sure there is a BaseFragment handle this event.
            fragments.indices.reversed()
                .map { fragments[it] }
                .filter { it != null && it is BaseFragment && it.onBackPressed() }
                .forEach { return }
        }
        super.onBackPressed()
    }
}