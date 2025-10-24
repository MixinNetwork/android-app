package one.mixin.android.ui.common

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import one.mixin.android.R
import one.mixin.android.ui.contacts.ContactsActivity
import one.mixin.android.ui.home.ConversationListFragment
import one.mixin.android.ui.home.ExploreFragment
import one.mixin.android.ui.home.web3.MarketFragment
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.ui.wallet.WalletFragment
import timber.log.Timber

class NavigationController() {

    sealed class Destination(val tag: String)

    data object ConversationList : Destination(ConversationListFragment.TAG)

    data object Wallet : Destination(WalletFragment.TAG)

    data object Explore : Destination(ExploreFragment.TAG)

    data object Market : Destination(MarketFragment.TAG)

    private val destinations = listOf(ConversationList.tag, Wallet.tag, Market.tag, Explore.tag)

    fun navigate(
        fragmentManager: FragmentManager,
        destination: Destination,
        destinationFragment: Fragment,
    ) {
        try {
            // Ensure any previous transactions are completed
            fragmentManager.executePendingTransactions()
            val tx = fragmentManager.beginTransaction()
            val tag = destination.tag
            val f = fragmentManager.findFragmentByTag(tag)
            if (destinationFragment.isAdded) {
                if (fragmentManager != destinationFragment.parentFragmentManager) {
                    destinationFragment.parentFragmentManager.beginTransaction().remove(destinationFragment).commitNowAllowingStateLoss()
                    tx.add(R.id.root_view, destinationFragment, tag)
                } else {
                    tx.show(destinationFragment)
                }
            } else if (f == null || !f.isAdded) {
                tx.add(R.id.root_view, destinationFragment, tag)
            } else {
                tx.show(f)
            }
            fragmentManager.fragments.filter { it.tag in destinations && it.tag != destination.tag }.forEach { fragment ->
                tx.hide(fragment)
            }
            tx.commitNowAllowingStateLoss()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun pushContacts(activity: Activity) {
        ContactsActivity.show(activity)
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

    fun hideSearch(fragmentManager: FragmentManager) {
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

    fun removeSearch(fragmentManager: FragmentManager) {
        val f = fragmentManager.findFragmentByTag(SearchFragment.TAG) ?: return
        fragmentManager.beginTransaction().remove(f).commit()
    }
}
