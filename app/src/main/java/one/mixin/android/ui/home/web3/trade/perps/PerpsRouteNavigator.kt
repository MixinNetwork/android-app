package one.mixin.android.ui.home.web3.trade.perps

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsOrderItem
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.util.analytics.AnalyticsTracker

object PerpsRouteNavigator {
    private val ROUTE_TAGS = setOf(
        PerpsMarketDetailFragment.TAG,
        AllPositionsFragment.TAG,
        PositionDetailFragment.TAG,
        PerpsOpenPositionFragment.TAG,
    )

    fun showTradeRoot(fragmentManager: FragmentManager) {
        ROUTE_TAGS.forEach { tag ->
            popRoute(fragmentManager, tag)
        }
        removeRoutes(fragmentManager) { it.isPerpsRoute() }
    }

    fun showMarketDetail(
        fragmentManager: FragmentManager,
        marketId: String,
        marketSymbol: String,
        displaySymbol: String,
        tokenSymbol: String,
        source: String = AnalyticsTracker.PerpsSource.PERPS_ACTIVITY_DETAIL,
    ) {
        popRoute(fragmentManager, PerpsMarketDetailFragment.TAG)
        removeRoutes(fragmentManager) { it is PerpsMarketDetailFragment }
        addRoute(
            fragmentManager = fragmentManager,
            fragment = PerpsMarketDetailFragment.newInstance(
                marketId = marketId,
                marketSymbol = marketSymbol,
                displaySymbol = displaySymbol,
                tokenSymbol = tokenSymbol,
                source = source,
            ),
            tag = PerpsMarketDetailFragment.TAG,
        )
    }

    fun showPositionList(
        fragmentManager: FragmentManager,
        showOpenPositions: Boolean,
        source: String = AnalyticsTracker.PerpsSource.PERPS_ALL_POSITIONS,
    ) {
        popRoute(fragmentManager, AllPositionsFragment.TAG)
        removeRoutes(fragmentManager) { it is AllPositionsFragment }
        addRoute(
            fragmentManager = fragmentManager,
            fragment = AllPositionsFragment.newInstance(
                showOpenPositions = showOpenPositions,
                source = source,
                useTradeFlowNavigator = true,
            ),
            tag = AllPositionsFragment.TAG,
        )
    }

    fun showPositionDetail(
        fragmentManager: FragmentManager,
        position: PerpsPositionItem,
        source: String = AnalyticsTracker.PerpsSource.PERPS_ACTIVITY_DETAIL,
    ) {
        popRoute(fragmentManager, PositionDetailFragment.TAG)
        removeRoutes(fragmentManager) { it is PositionDetailFragment }
        addRoute(
            fragmentManager = fragmentManager,
            fragment = PositionDetailFragment.newInstance(
                position = position,
                source = source,
                useTradeFlowNavigator = true,
            ),
            tag = PositionDetailFragment.TAG,
        )
    }

    fun showPositionDetail(
        fragmentManager: FragmentManager,
        order: PerpsOrderItem,
        source: String = AnalyticsTracker.PerpsSource.PERPS_ACTIVITY_DETAIL,
    ) {
        popRoute(fragmentManager, PositionDetailFragment.TAG)
        removeRoutes(fragmentManager) { it is PositionDetailFragment }
        addRoute(
            fragmentManager = fragmentManager,
            fragment = PositionDetailFragment.newInstance(
                order = order,
                source = source,
                useTradeFlowNavigator = true,
            ),
            tag = PositionDetailFragment.TAG,
        )
    }

    fun showOpenPosition(
        fragmentManager: FragmentManager,
        marketId: String,
        marketSymbol: String,
        displaySymbol: String,
        tokenSymbol: String,
        isLong: Boolean,
        source: String,
    ) {
        popRoute(fragmentManager, PerpsOpenPositionFragment.TAG)
        removeRoutes(fragmentManager) { it is PerpsOpenPositionFragment }
        addRoute(
            fragmentManager = fragmentManager,
            fragment = PerpsOpenPositionFragment.newInstance(
                marketId = marketId,
                marketSymbol = marketSymbol,
                displaySymbol = displaySymbol,
                tokenSymbol = tokenSymbol,
                isLong = isLong,
                source = source,
            ),
            tag = PerpsOpenPositionFragment.TAG,
        )
    }

    fun closeTopRoute(fragmentManager: FragmentManager, fragment: Fragment) {
        val topEntryName = fragmentManager.takeIf { it.backStackEntryCount > 0 }
            ?.getBackStackEntryAt(fragmentManager.backStackEntryCount - 1)
            ?.name
        if (topEntryName in ROUTE_TAGS) {
            fragmentManager.popBackStack()
            return
        }

        val topFragment = fragmentManager.fragments.lastOrNull { it.isVisible }
        val target = if (topFragment?.isPerpsRoute() == true) topFragment else fragment
        removeRoute(fragmentManager, target)
    }

    private fun addRoute(
        fragmentManager: FragmentManager,
        fragment: Fragment,
        tag: String,
    ) {
        fragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, 0, 0, R.anim.slide_out_right)
            .add(R.id.container, fragment, tag)
            .addToBackStack(tag)
            .commitAllowingStateLoss()
    }

    private fun popRoute(fragmentManager: FragmentManager, tag: String) {
        if (fragmentManager.isStateSaved) return
        fragmentManager.popBackStackImmediate(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    private fun removeRoutes(
        fragmentManager: FragmentManager,
        predicate: (Fragment) -> Boolean,
    ) {
        val routeFragments = fragmentManager.fragments.filter(predicate)
        if (routeFragments.isEmpty()) return

        val transaction = fragmentManager.beginTransaction()
        routeFragments.forEach(transaction::remove)
        if (fragmentManager.isStateSaved) {
            transaction.commitAllowingStateLoss()
        } else {
            transaction.commitNowAllowingStateLoss()
        }
    }

    private fun removeRoute(fragmentManager: FragmentManager, fragment: Fragment) {
        fragmentManager.beginTransaction()
            .setCustomAnimations(0, R.anim.slide_out_right, R.anim.stay, 0)
            .remove(fragment)
            .commitAllowingStateLoss()
    }

    private fun Fragment.isPerpsRoute(): Boolean {
        return this is PerpsMarketDetailFragment ||
            this is AllPositionsFragment ||
            this is PositionDetailFragment ||
            this is PerpsOpenPositionFragment
    }
}
