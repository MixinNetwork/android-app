package one.mixin.android.web3.swap

import one.mixin.android.Constants
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.db.web3.vo.isWeb3TransferSupported

internal fun isSwapSearchChainAvailable(
    chainId: String,
    addressChainIds: Set<String>,
): Boolean =
    chainId in addressChainIds ||
        (chainId in Constants.Web3EvmChainIds &&
            Constants.ChainId.ETHEREUM_CHAIN_ID in addressChainIds)

internal fun filterSwapTokensByWalletChains(
    tokens: List<SwapToken>,
    addressChainIds: Set<String>?,
    inMixin: Boolean,
): List<SwapToken> =
    tokens.filter { token ->
        inMixin ||
            (isWeb3TransferSupported(token.chain.chainId) &&
                (addressChainIds == null || isSwapSearchChainAvailable(token.chain.chainId, addressChainIds)))
    }
