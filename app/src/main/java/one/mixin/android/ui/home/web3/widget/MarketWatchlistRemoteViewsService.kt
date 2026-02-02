package one.mixin.android.ui.home.web3.widget

import android.content.Intent
import android.widget.RemoteViewsService

class MarketWatchlistRemoteViewsService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appContext = applicationContext
        return MarketWatchlistRemoteViewsFactory(appContext, intent)
    }
}
