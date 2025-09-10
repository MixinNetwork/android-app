package one.mixin.android.ui.common

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import one.mixin.android.R
import one.mixin.android.ui.contacts.ContactsActivity
import one.mixin.android.ui.home.ConversationListFragment
import one.mixin.android.ui.home.ExploreFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.home.inscription.CollectiblesFragment
import one.mixin.android.ui.home.web3.MarketFragment
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.ui.wallet.WalletFragment
import timber.log.Timber

class NavigationController(mainActivity: MainActivity) {
    private val fragmentManager: FragmentManager = mainActivity.supportFragmentManager
    private val context = mainActivity

    sealed class Destination(val tag: String)

    data object ConversationList : Destination(ConversationListFragment.TAG)

    data object Wallet : Destination(WalletFragment.TAG)

    data object Explore : Destination(ExploreFragment.TAG)

    data object Market : Destination(MarketFragment.TAG)

    private val destinations = listOf(ConversationList, Wallet, Market, Explore)

    fun navigate(
        destination: Destination,
        destinationFragment: Fragment,
    ) {
        try {
            // Ensure any previous transactions are completed
            fragmentManager.executePendingTransactions()
            val tx = fragmentManager.beginTransaction()
            val tag = destination.tag
            val f = fragmentManager.findFragmentByTag(tag)
            if (f == null || !f.isAdded) {
                tx.add(R.id.root_view, destinationFragment, tag)
            } else {
                tx.show(f)
            }
            destinations.forEach { d ->
                if (d != destination) {
                    fragmentManager.findFragmentByTag(d.tag)?.let { tx.hide(it) }
                }
            }
            tx.commitNowAllowingStateLoss()
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    fun pushContacts() {
        ContactsActivity.show(context)
    }

    fun showSearch(fm: FragmentManager) {
        var searchFragment = fm.findFragmentByTag(SearchFragment.TAG)
        if (searchFragment == null) {
            searchFragment = SearchFragment()
            fm.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                .add(R.id.container_search, searchFragment, SearchFragment.TAG)
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
                    override fun onAnimationEnd(animation: Animator) {
                        setListener(null)
                        f.view?.isVisible = false
                    }
                },
            )
        }?.alpha(0f)?.start()
    }

    fun removeSearch() {
        val f = fragmentManager.findFragmentByTag(SearchFragment.TAG) ?: return
        fragmentManager.beginTransaction().remove(f).commit()
    }
}
