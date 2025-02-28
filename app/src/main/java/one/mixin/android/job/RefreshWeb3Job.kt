package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.Constants.Account.ChainAddress.SOLANA_ADDRESS
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI

class RefreshWeb3Job : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(GROUP).persist().requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshWeb3Job"
    }

    override fun onRun(): Unit =
        runBlocking {
            val erc20Address = PropertyHelper.findValueByKey(EVM_ADDRESS, "")
            val solAddress = PropertyHelper.findValueByKey(SOLANA_ADDRESS, "")
            val list = mutableListOf<Web3Token>()
            if (erc20Address.isNotBlank()) {
                requestRouteAPI(
                    invokeNetwork = {
                        web3Service.web3Account(erc20Address)
                    },
                    successBlock = { response ->
                        response.data?.tokens?.let {
                            list.addAll(it)
                        }
                    },
                    requestSession = {
                        userService.fetchSessionsSuspend(listOf(Constants.RouteConfig.ROUTE_BOT_USER_ID))
                    }
                )
            }
            if (solAddress.isNotBlank()) {
                requestRouteAPI(
                    invokeNetwork = {
                        web3Service.web3Account(solAddress)
                    },
                    successBlock = { response ->
                        response.data?.tokens?.let {
                            list.addAll(it)
                        }
                        web3TokenDao.insertListSuspend(list)
                    },
                    requestSession = {
                        userService.fetchSessionsSuspend(listOf(Constants.RouteConfig.ROUTE_BOT_USER_ID))
                    }
                )
            }
        }
}
