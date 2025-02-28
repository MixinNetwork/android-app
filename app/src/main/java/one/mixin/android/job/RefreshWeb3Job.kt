package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.Constants.Account.ChainAddress.SOLANA_ADDRESS
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.db.property.PropertyHelper

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
            var response = web3Service.web3Account(erc20Address)
            response.data?.tokens?.let {
                list.addAll(it)
            }
            response = web3Service.web3Account(solAddress)
            response.data?.tokens?.let {
                list.addAll(it)
            }
            web3TokenDao.insertListSuspend(list)
        }
}
