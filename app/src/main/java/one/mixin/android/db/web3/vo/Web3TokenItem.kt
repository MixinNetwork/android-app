package one.mixin.android.db.web3.vo

import android.os.Parcelable
import androidx.room.ColumnInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import one.mixin.android.Constants
import one.mixin.android.api.response.web3.SwapChain
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.api.response.web3.Swappable
import one.mixin.android.api.response.web3.WalletOutput
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.toHex
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.web3.Rpc
import one.mixin.android.web3.Web3Exception
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.SolanaTxSource
import one.mixin.android.web3.js.Web3Signer
import one.mixin.android.ui.common.biometric.EmptyUtxoException
import org.bitcoinj.base.AddressParser
import org.bitcoinj.base.Coin
import org.bitcoinj.base.LegacyAddress
import org.bitcoinj.base.Sha256Hash
import org.bitcoinj.core.TransactionInput
import org.bitcoinj.core.TransactionOutPoint
import org.bitcoinj.crypto.ECKey
import org.bitcoinj.base.Address as BtcAddress
import org.bitcoinj.core.Transaction as BtcTransaction
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.sol4k.Constants.TOKEN_2022_PROGRAM_ID
import org.sol4k.Constants.TOKEN_PROGRAM_ID
import org.sol4k.Convert.solToLamport
import org.sol4k.PublicKey
import org.sol4k.Transaction
import org.sol4k.instruction.Instruction
import org.sol4k.instruction.TransferInstruction
import org.sol4kt.addPlaceholderSignature
import org.sol4kt.instruction.CreateAssociatedTokenAccountInstructionCompat
import org.sol4kt.instruction.SplTransferInstructionCompat
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Uint
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.RoundingMode

@Parcelize
data class Web3TokenItem(
    @ColumnInfo(name = "wallet_id")
    val walletId: String,
    @ColumnInfo(name = "asset_id")
    val assetId: String,
    @ColumnInfo(name = "chain_id")
    val chainId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "asset_key")
    val assetKey: String,
    @ColumnInfo(name = "symbol")
    val symbol: String,
    @ColumnInfo(name = "icon_url")
    val iconUrl: String,
    @ColumnInfo(name = "precision")
    val precision: Int,
    @ColumnInfo(name = "kernel_asset_id")
    val kernelAssetId: String = "",
    @ColumnInfo(name = "amount")
    val balance: String,
    @ColumnInfo(name = "price_usd")
    val priceUsd: String,
    @ColumnInfo(name = "change_usd")
    val changeUsd: String,
    @ColumnInfo(name = "chain_icon_url")
    val chainIcon: String?,
    @ColumnInfo(name = "chain_name")
    val chainName: String?,
    @ColumnInfo(name = "chain_symbol")
    val chainSymbol: String?,
    @ColumnInfo(name = "hidden")
    val hidden: Boolean?,
    @ColumnInfo(name = "level")
    val level: Int,
) : Parcelable, Swappable {
    
    fun getChainDisplayName(): String {
        return chainName ?: when {
            chainId == Constants.ChainId.ETHEREUM_CHAIN_ID -> "Ethereum"
            chainId == Constants.ChainId.Base -> "ETH"
            chainId == Constants.ChainId.Arbitrum -> "Arbitrum One"
            chainId == Constants.ChainId.Optimism -> "Optimism"
            chainId == Constants.ChainId.Polygon -> "Polygon"
            chainId == Constants.ChainId.BinanceSmartChain -> "BNB Chain"
            chainId == Constants.ChainId.BITCOIN_CHAIN_ID -> "Bitcoin"
            chainId == Constants.ChainId.SOLANA_CHAIN_ID -> "Solana"
            else -> chainId
        }
    }
    
    fun isSolanaChain(): Boolean {
        return chainId.equals(Constants.ChainId.SOLANA_CHAIN_ID, true)
    }
    
    override fun toSwapToken(): SwapToken {
        return SwapToken(
            walletId = walletId,
            address = assetKey,
            assetId = assetId,
            decimals = precision,
            name = name,
            symbol = symbol,
            icon = iconUrl,
            chain = SwapChain(
                chainId = chainId,
                name = getChainDisplayName(),
                symbol = chainSymbol ?: "",
                icon = chainIcon ?: "",
                price = null,
            ),
            balance = balance,
            price = priceUsd,
            isWeb3 =  true,
            level = level
        )
    }
    
    override fun getUnique(): String {
       return assetId
    }

    fun toStringAmount(amount: Long): String {
        return realAmount(amount).stripTrailingZeros().toPlainString()
    }

    fun realAmount(amount: Long): BigDecimal {
        return BigDecimal(amount).divide(BigDecimal.TEN.pow(precision)).setScale(9, RoundingMode.CEILING)
    }

    fun btcValue(btcPrice: BigDecimal): BigDecimal = try {
        BigDecimal(balance).multiply(btcPrice)
    } catch (e: NumberFormatException) {
        BigDecimal.ZERO
    }
    
    fun fiat(): BigDecimal = try {
        if (balance.isBlank()) BigDecimal.ZERO
        else BigDecimal(balance).multiply(priceFiat())
    } catch (e: NumberFormatException) {
        BigDecimal.ZERO
    }

    fun priceFiat(): BigDecimal = when (priceUsd) {
        "0" -> BigDecimal.ZERO
        "" -> BigDecimal.ZERO
        else -> BigDecimal(priceUsd).multiply(Fiats.getRate().toBigDecimal())
    }

    fun Long.solLamportToAmount(scale: Int = 9): BigDecimal {
        return BigDecimal(this).divide(BigDecimal.TEN.pow(9)).setScale(scale, RoundingMode.CEILING)
    }

    fun isSpam() = level <= Constants.AssetLevel.SPAM

    fun toTokenItem(): TokenItem {
        return TokenItem(
            assetId = assetId,
            symbol = symbol,
            name = name,
            iconUrl = iconUrl,
            balance = balance,
            priceBtc = "0",
            priceUsd = priceUsd,
            chainId = chainId,
            changeUsd = changeUsd.toBigDecimalOrNull()?.divide(BigDecimal.TEN.pow(2), 16, RoundingMode.HALF_UP)?.toPlainString() ?: "0",
            changeBtc = "0",
            hidden = hidden,
            confirmations = 0,
            chainIconUrl = chainIcon,
            chainSymbol = chainSymbol,
            chainName = chainName,
            assetKey = assetKey,
            dust = null,
            withdrawalMemoPossibility = null,
            collectionHash = null,
            level = level,
            precision = precision
        )
    }
}

fun Web3TokenItem.getChainFromName(): Chain {
    return when {
        chainId == Constants.ChainId.ETHEREUM_CHAIN_ID -> Chain.Ethereum
        chainId == Constants.ChainId.Base -> Chain.Base
        chainId == Constants.ChainId.Optimism -> Chain.Optimism
        chainId == Constants.ChainId.Arbitrum -> Chain.Arbitrum
        chainId == Constants.ChainId.Polygon-> Chain.Polygon
        chainId == Constants.ChainId.BinanceSmartChain-> Chain.BinanceSmartChain
        chainId == Constants.ChainId.Avalanche -> Chain.Avalanche
        chainId == Constants.ChainId.SOLANA_CHAIN_ID -> Chain.Solana
        chainId == Constants.ChainId.BITCOIN_CHAIN_ID -> Chain.Bitcoin
        else -> throw IllegalArgumentException("Not support: $chainId")
    }
}

fun Web3TokenItem.getChainSymbolFromName(): String {
    return when {
        chainId == Constants.ChainId.ETHEREUM_CHAIN_ID -> "ETH"
        chainId == Constants.ChainId.Base -> "ETH"
        chainId == Constants.ChainId.Optimism -> "ETH"
        chainId == Constants.ChainId.Arbitrum -> "ETH"
        chainId == Constants.ChainId.Avalanche -> "AVAX"
        chainId == Constants.ChainId.BinanceSmartChain -> "BNB"
        chainId == Constants.ChainId.Polygon -> "POL"
        chainId == Constants.ChainId.BITCOIN_CHAIN_ID -> "BTC"
        chainId == Constants.ChainId.SOLANA_CHAIN_ID -> "SOL"
        else -> throw IllegalArgumentException("Not support: $chainId")
    }
}


suspend fun Web3TokenItem.buildTransaction(
    rpc: Rpc,
    fromAddress: String,
    toAddress: String,
    v: String,
    localUtxos: List<WalletOutput>? = null,
    gas: BigDecimal? = BigDecimal.ZERO,
): JsSignMessage {
    if (chainId == Constants.ChainId.SOLANA_CHAIN_ID) {
        Web3Signer.useSolana()
        val sender = PublicKey(fromAddress)
        val receiver = PublicKey(toAddress)
        val instructions = mutableListOf<Instruction>()
        if (isNativeSolToken()) {
            val amount = solToLamport(v).toLong()
            instructions.add(TransferInstruction(sender, receiver, amount))
        } else {
            val tokenMintAddress = PublicKey(assetKey)
            val tokenMintAccount = withContext(Dispatchers.IO) {
                rpc.getAccountInfo(tokenMintAddress)
            }
            if (tokenMintAccount == null) {
                throw Web3Exception(Web3Exception.ErrorCode.InvalidWeb3Token, "rpc getAccountInfo $assetKey is null")
            }
            val tokenProgramId = if (tokenMintAccount.owner == TOKEN_PROGRAM_ID) {
                TOKEN_PROGRAM_ID
            } else if (tokenMintAccount.owner == TOKEN_2022_PROGRAM_ID) {
                TOKEN_2022_PROGRAM_ID
            } else {
                throw Web3Exception(Web3Exception.ErrorCode.InvalidWeb3Token, "invalid account owner ${tokenMintAccount.owner}")
            }

            val (receiveAssociatedAccount) = PublicKey.findProgramDerivedAddress(receiver, tokenMintAddress, tokenProgramId)
            val receiveAssociatedAccountInfo =
                withContext(Dispatchers.IO) {
                    rpc.getAccountInfo(receiveAssociatedAccount)
                }
            if (receiveAssociatedAccountInfo == null) {
                instructions.add(
                    CreateAssociatedTokenAccountInstructionCompat(
                        payer = sender,
                        associatedToken = receiveAssociatedAccount,
                        owner = receiver,
                        mint = tokenMintAddress,
                        tokenProgramId,
                    ),
                )
            }
            val tokenAmount =
                withContext(Dispatchers.IO) {
                    rpc.getTokenSupply(assetKey)
                }
            if (tokenAmount == null) {
                throw Web3Exception(Web3Exception.ErrorCode.InvalidWeb3Token, "rpc getTokenSupply Web3Token $assetKey is null")
            }
            if (tokenAmount.decimals != precision) {
                throw Web3Exception(Web3Exception.ErrorCode.InvalidWeb3Token, "Web3Token decimals $precision not equal rpc decimals ${tokenAmount.decimals}")
            }
            val (sendAssociatedAccount) = PublicKey.findProgramDerivedAddress(sender, tokenMintAddress, tokenProgramId)
            instructions.add(
                SplTransferInstructionCompat(
                    from = sendAssociatedAccount,
                    to = receiveAssociatedAccount,
                    mint = tokenMintAddress,
                    owner = sender,
                    signers = emptyList(),
                    amount = BigDecimal(v).multiply(BigDecimal.TEN.pow(precision)).toLong(),
                    decimals = tokenAmount.decimals,
                    tokenProgramId = tokenProgramId,
                ),
            )
        }
        val transaction =
            Transaction(
                fromAddress, // use address as temp placeholder, will replace when signing
                instructions,
                sender,
            )
        transaction.addPlaceholderSignature()
        val tx = transaction.serialize().base64Encode()
        return JsSignMessage(0, JsSignMessage.TYPE_RAW_TRANSACTION, data = tx, solanaTxSource = SolanaTxSource.InnerTransfer)
    } else if (chainId in Constants.Web3EvmChainIds) {
        Web3Signer.useEvm()
        // (chainId.equals("blast", true) && assetKey == "0x0000000000000000000000000000000000000000") ||
        val transaction =
            if ((chainId == Constants.ChainId.Base && assetKey == "0x0000000000000000000000000000000000000000") ||
                (chainId == Constants.ChainId.ETHEREUM_CHAIN_ID && assetKey == "0x0000000000000000000000000000000000000000") ||
                (chainId == Constants.ChainId.Polygon && (assetKey == "0x0000000000000000000000000000000000000000" || assetKey == "0x0000000000000000000000000000000000001010")) ||
                (chainId == Constants.ChainId.BinanceSmartChain && assetKey == "0x0000000000000000000000000000000000000000") ||
                (chainId == Constants.ChainId.Optimism && assetKey == "0x0000000000000000000000000000000000000000") ||
                (chainId == Constants.ChainId.Arbitrum && assetKey == "0x0000000000000000000000000000000000000000") ||
                (chainId == Constants.ChainId.Avalanche && assetKey == "0x0000000000000000000000000000000000000000")
            ) {
                val value = Numeric.toHexStringWithPrefix(Convert.toWei(v, Convert.Unit.ETHER).toBigInteger())
                WCEthereumTransaction(fromAddress, toAddress, null, null, null, null, null, null, value, null)
            } else {
                val function =
                    Function(
                        "transfer",
                        listOf(
                            Address(toAddress),
                            Uint(BigDecimal(v).multiply(BigDecimal.TEN.pow(precision)).toBigInteger(),),
                        ),
                        emptyList(),
                    )
                val data = FunctionEncoder.encode(function)
                WCEthereumTransaction(fromAddress, assetKey, null, null, null, null, null, null, "0x0", data)
            }
        return JsSignMessage(0, JsSignMessage.TYPE_TRANSACTION, transaction)
    } else if (chainId == Constants.ChainId.BITCOIN_CHAIN_ID) {
        val addressParser = AddressParser.getDefault()
        if (!localUtxos.isNullOrEmpty()) {
            val changeAddress = addressParser.parseAddress( fromAddress)
            val recipientAddress = addressParser.parseAddress( toAddress)
            val sendAmount = Coin.parseCoin(v)
            val fee = Coin.parseCoin((gas ?: BigDecimal.ZERO).toString())
            val targetAmount = sendAmount.add(fee)
            var selectedAmount = Coin.ZERO
            val selectedUtxos: MutableList<WalletOutput> = mutableListOf()
            for (localUtxo: WalletOutput in localUtxos) {
                if (selectedAmount.isGreaterThan(targetAmount) || selectedAmount == targetAmount) {
                    break
                }
                selectedUtxos.add(localUtxo)
                selectedAmount = selectedAmount.add(Coin.parseCoin(localUtxo.amount))
            }
            val changeAmount = selectedAmount.subtract(targetAmount)
            if (changeAmount.isNegative) {
                throw IllegalArgumentException("insufficient balance")
            }
            val tx = BtcTransaction()
            tx.addOutput(sendAmount, recipientAddress)
            if (!changeAmount.isZero) {
                tx.addOutput(changeAmount, changeAddress)
            }
            for (selectedUtxo: WalletOutput in selectedUtxos) {
                val prevTxHash = Sha256Hash.wrap(selectedUtxo.transactionHash)
                val outPoint = TransactionOutPoint(selectedUtxo.outputIndex, prevTxHash)
                val input = TransactionInput(tx, byteArrayOf(), outPoint)
                tx.addInput(input)
            }
            val rawTxHex: String = tx.serialize().toHex()
            return JsSignMessage(0, JsSignMessage.TYPE_BTC_TRANSACTION, data = rawTxHex, fee = gas)

        } else {
            throw EmptyUtxoException
        }
    } else {
        throw IllegalStateException("Not support: $chainId")
    }
}
