package one.mixin.android.web3.js

/**
 * Error codes returned to DApps by the injected wallet provider.
 *
 * Specifications and conventions:
 * - JSON-RPC 2.0 errors: https://www.jsonrpc.org/specification#error_object
 * - EIP-1193 provider errors: https://eips.ethereum.org/EIPS/eip-1193#provider-errors
 * - wallet_switchEthereumChain specification: https://eips.ethereum.org/EIPS/eip-3326#wallet_switchethereumchain
 * - MetaMask 4902 convention: https://metamask.github.io/mm-docs-v2/security-controls/wallet/reference/rpc-api/#wallet_switchethereumchain
 */
object WalletErrorCode {
    /** JSON-RPC 2.0: the request parameters are invalid. */
    const val INVALID_PARAMS = -32602

    /** JSON-RPC 2.0: an unexpected error occurred while processing the request. */
    const val INTERNAL_ERROR = -32603

    /** EIP-1193: the user rejected the request. */
    const val USER_REJECTED_REQUEST = 4001

    /** EIP-1193: the requested account or method has not been authorized. */
    const val UNAUTHORIZED = 4100

    /** EIP-1193: the provider does not support the requested method. */
    const val UNSUPPORTED_METHOD = 4200

    /**
     * MetaMask/ecosystem convention: wallet_switchEthereumChain does not recognize the requested
     * chain. EIP-3326 specifies the method but does not assign this error code.
     */
    const val UNRECOGNIZED_CHAIN = 4902
}
