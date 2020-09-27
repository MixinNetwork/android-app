package one.mixin.android.ui.common

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import one.mixin.android.R
import one.mixin.android.session.Session
import one.mixin.android.ui.contacts.ContactsActivity
import one.mixin.android.ui.home.ConversationListFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.ui.setting.WalletPasswordFragment
import one.mixin.android.ui.wallet.WalletActivity

class NavigationController
constructor(mainActivity: MainActivity) {
    private val containerId: Int = R.id.container
    private val fragmentManager: FragmentManager = mainActivity.supportFragmentManager
    private val context = mainActivity

    fun pushContacts() {
        ContactsActivity.show(context)
    }

    fun pushWallet() {
        if (Session.getAccount()?.hasPin == true) {
            WalletActivity.show(context)
        } else {
            fragmentManager.beginTransaction()
                .replace(R.id.root_view, WalletPasswordFragment.newInstance(false))
                .addToBackStack(null)
                .commitAllowingStateLoss()
        }
    }

    fun navigateToMessage() {
        val conversationListFragment = ConversationListFragment.newInstance()
        fragmentManager.beginTransaction()
            .replace(containerId, conversationListFragment, ConversationListFragment.TAG)
            .commitAllowingStateLoss()
    }

    fun showSearch() {
        var searchFragment = fragmentManager.findFragmentByTag(SearchFragment.TAG)
        if (searchFragment == null) {
            searchFragment = SearchFragment()
            fragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                .add(containerId, searchFragment, SearchFragment.TAG)
                .commitAllowingStateLoss()
        } else {
            searchFragment.view?.isVisible = true
            searchFragment.view?.animate()?.alpha(1f)?.start()
        }
    }

    fun hideSearch() {
        val f = fragmentManager.findFragmentByTag(SearchFragment.TAG)
        f?.view?.animate()?.apply {
            setListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        setListener(null)
                        f.view?.isVisible = false
                    }
                }
            )
        }?.alpha(0f)?.start()
    }
}
