package one.mixin.android.web3.send

import one.mixin.android.Constants
import one.mixin.android.api.response.web3.WalletOutput
import one.mixin.android.crypto.PearlKeyGenerator
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.toHex
import org.bitcoinj.base.Address
import org.bitcoinj.base.AddressParser
import org.bitcoinj.base.BitcoinNetwork
import org.bitcoinj.base.Coin
import org.bitcoinj.base.Sha256Hash
import org.bitcoinj.core.TransactionInput
import org.bitcoinj.core.TransactionOutPoint
import org.bitcoinj.core.TransactionOutput
import org.bitcoinj.core.Transaction as BtcTransaction
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.ByteBuffer

object BtcTransactionBuilder {

    private const val RBF_SEQUENCE: Long = 0xfffffffdL
    private const val BTC_MINIMUM_OUTPUT_SATOSHIS: Long = 1_000L
    private const val PEARL_MINIMUM_OUTPUT_SATOSHIS: Long = 100_000L
    private const val INCREMENTAL_RELAY_FEE_SAT_PER_VB: Long = 1L
    private const val RBF_SAFETY_EXTRA_SATOSHIS: Long = 300L

    private val satoshisPerBtc: BigDecimal = BigDecimal.valueOf(Coin.COIN.value)

    private fun calculateRbfRequiredTotalFeeSatoshis(
        oldTotalFeeSatoshis: Long,
        replacementVirtualSize: Int,
        targetFeeRateSatPerVb: Long,
    ): Long {
        if (replacementVirtualSize <= 0) {
            throw IllegalArgumentException("invalid transaction")
        }
        val feeByTargetRate: Long = replacementVirtualSize.toLong() * targetFeeRateSatPerVb
        val feeByRbfIncrement: Long = oldTotalFeeSatoshis + (replacementVirtualSize.toLong() * INCREMENTAL_RELAY_FEE_SAT_PER_VB)
        val feeBySafetyExtra: Long = oldTotalFeeSatoshis + RBF_SAFETY_EXTRA_SATOSHIS
        return maxOf(feeByTargetRate, feeByRbfIncrement, feeBySafetyExtra)
    }

    data class BuiltBtcTransaction(
        val rawHex: String,
        val feeBtc: BigDecimal,
        val virtualSize: Int,
        val changeAmount: Coin,
    )

    fun buildSendTransaction(
        chainId: String = Constants.ChainId.BITCOIN_CHAIN_ID,
        fromAddress: String,
        toAddress: String,
        amountBtc: String,
        localUtxos: List<WalletOutput>,
        feeRate: BigDecimal,
        minFeeBtc: BigDecimal? = null,
        minimumChangeSatoshis: Long = minimumChangeSatoshis(chainId),
    ): BuiltBtcTransaction {
        val built: BuiltBtcTransaction = buildSendTransactionInternal(
            chainId = chainId,
            fromAddress = fromAddress,
            toAddress = toAddress,
            amountBtc = amountBtc,
            localUtxos = localUtxos,
            feeRate = feeRate,
            minimumChangeSatoshis = minimumChangeSatoshis,
        )
        val resolvedMinFeeBtc: BigDecimal = minFeeBtc ?: return built
        if (built.virtualSize <= 0) return built
        if (built.feeBtc >= resolvedMinFeeBtc) return built
        val minFeeSatoshis: BigDecimal = resolvedMinFeeBtc.multiply(satoshisPerBtc).setScale(0, RoundingMode.UP)
        val requiredFeeRate: BigDecimal = minFeeSatoshis
            .divide(BigDecimal(built.virtualSize), 8, RoundingMode.UP)
            .max(feeRate)
        return buildSendTransactionInternal(
            chainId = chainId,
            fromAddress = fromAddress,
            toAddress = toAddress,
            amountBtc = amountBtc,
            localUtxos = localUtxos,
            feeRate = requiredFeeRate,
            minimumChangeSatoshis = minimumChangeSatoshis,
        )
    }

    private fun buildSendTransactionInternal(
        chainId: String,
        fromAddress: String,
        toAddress: String,
        amountBtc: String,
        localUtxos: List<WalletOutput>,
        feeRate: BigDecimal,
        minimumChangeSatoshis: Long,
    ): BuiltBtcTransaction {
        require(chainId in Constants.Web3UtxoChainIds) { "Unsupported UTXO chain: $chainId" }
        require(localUtxos.all { it.assetId == chainId && it.address == fromAddress }) {
            "UTXOs do not belong to the selected chain and address"
        }
        val changeAddress = parseAddress(chainId, fromAddress)
        val recipientAddress = parseAddress(chainId, toAddress)
        val sendAmount = Coin.parseCoin(amountBtc)
        require(sendAmount.value >= minimumOutputSatoshis(chainId)) {
            "Amount must be at least ${minimumTransferAmount(chainId).toPlainString()}"
        }
        val minimumChangeAmount: Coin = Coin.valueOf(minimumChangeSatoshis)
        var selectedAmount: Coin = Coin.ZERO
        val selectedUtxos: MutableList<WalletOutput> = mutableListOf()
        var feeBtc: BigDecimal = BigDecimal.ZERO
        var virtualSize: Int = 0
        var changeAmount: Coin = Coin.ZERO
        for (localUtxo: WalletOutput in localUtxos) {
            selectedUtxos.add(localUtxo)
            selectedAmount = selectedAmount.add(Coin.parseCoin(localUtxo.amount))
            val candidateTx = BtcTransaction()
            candidateTx.addOutput(sendAmount, recipientAddress)
            for (utxo: WalletOutput in selectedUtxos) {
                val prevTxHash = Sha256Hash.wrap(utxo.transactionHash)
                val outPoint = TransactionOutPoint(utxo.outputIndex, prevTxHash)
                val input = TransactionInput(candidateTx, byteArrayOf(), outPoint)
                candidateTx.addInput(input.withSequence(RBF_SEQUENCE))
            }
            virtualSize = estimateVirtualSize(candidateTx, chainId)
            val feeSatoshis: BigDecimal = feeRate.multiply(BigDecimal(virtualSize)).setScale(0, RoundingMode.UP)
            feeBtc = feeSatoshis.divide(satoshisPerBtc, 8, RoundingMode.HALF_UP)
            val targetAmount: Coin = sendAmount.add(Coin.parseCoin(feeBtc.toPlainString()))
            changeAmount = selectedAmount.subtract(targetAmount)
            if (changeAmount.isNegative) {
                continue
            }
            if (changeAmount.isGreaterThan(minimumChangeAmount) || changeAmount == minimumChangeAmount) {
                candidateTx.addOutput(changeAmount, changeAddress)
                virtualSize = estimateVirtualSize(candidateTx, chainId)
                val feeSatoshisWithChange: BigDecimal = feeRate.multiply(BigDecimal(virtualSize)).setScale(0, RoundingMode.UP)
                feeBtc = feeSatoshisWithChange.divide(satoshisPerBtc, 8, RoundingMode.HALF_UP)
                val targetAmountWithChange: Coin = sendAmount.add(Coin.parseCoin(feeBtc.toPlainString()))
                changeAmount = selectedAmount.subtract(targetAmountWithChange)
            }
            val finalTargetAmount: Coin = sendAmount.add(Coin.parseCoin(feeBtc.toPlainString()))
            if (selectedAmount.isLessThan(finalTargetAmount)) {
                continue
            }
            if (changeAmount.isZero || changeAmount.isGreaterThan(minimumChangeAmount) || changeAmount == minimumChangeAmount) {
                break
            }
            if (!changeAmount.isNegative && changeAmount.isLessThan(minimumChangeAmount)) {
                continue
            }
        }
        val targetAmount: Coin = sendAmount.add(Coin.parseCoin(feeBtc.toPlainString()))
        if (selectedAmount.isLessThan(targetAmount)) {
            val totalUtxoBtc: BigDecimal = BigDecimal.valueOf(selectedAmount.value)
                .divide(satoshisPerBtc, 8, RoundingMode.HALF_UP)
            throw InsufficientBtcBalanceException(feeBtc = feeBtc, utxoTotalBtc = totalUtxoBtc)
        }
        val tx = BtcTransaction()
        tx.addOutput(sendAmount, recipientAddress)
        if (changeAmount.isGreaterThan(minimumChangeAmount) || changeAmount == minimumChangeAmount) {
            tx.addOutput(changeAmount, changeAddress)
        }
        for (selectedUtxo: WalletOutput in selectedUtxos) {
            val prevTxHash = Sha256Hash.wrap(selectedUtxo.transactionHash)
            val outPoint = TransactionOutPoint(selectedUtxo.outputIndex, prevTxHash)
            val input = TransactionInput(tx, byteArrayOf(), outPoint)
            tx.addInput(input.withSequence(RBF_SEQUENCE))
        }
        virtualSize = estimateVirtualSize(tx, chainId)
        val feeSatoshi: BigDecimal = BigDecimal.valueOf(calculateFeeSatoshi(tx, localUtxos))
        val finalFeeBtc: BigDecimal = feeSatoshi.divide(satoshisPerBtc)
        return BuiltBtcTransaction(rawHex = tx.serialize().toHex(), feeBtc = finalFeeBtc, virtualSize = virtualSize, changeAmount = changeAmount)
    }

    fun buildSpeedUpReplacement(
        chainId: String = Constants.ChainId.BITCOIN_CHAIN_ID,
        rawTransactionHex: String,
        fromAddress: String,
        localUtxos: List<WalletOutput>,
        feeRate: BigDecimal,
        minimumChangeSatoshis: Long = minimumChangeSatoshis(chainId),
        maxExtraInputs: Int = 2,
    ): String {
        validateUtxos(chainId, fromAddress, localUtxos)
        val cleanedRawHex: String = rawTransactionHex.removePrefix("0x").trim()
        require(chainId != Constants.ChainId.PEARL_CHAIN_ID || isReplaceable(cleanedRawHex)) {
            "Pearl transaction does not signal RBF"
        }
        val originalTx: BtcTransaction = BtcTransaction.read(java.nio.ByteBuffer.wrap(cleanedRawHex.hexStringToByteArray()))
        val fromScriptBytes: ByteArray = buildOutputScript(chainId, fromAddress).program()
        val originalInputs: List<TransactionInput> = originalTx.inputs
        val originalOutputs: List<TransactionOutput> = originalTx.outputs
        val originalInputAmount: Coin = calculateInputAmount(originalInputs, localUtxos)
        val originalOutputAmount: Coin = originalOutputs.fold(Coin.ZERO) { acc, output -> acc.add(output.value) }
        val oldTotalFeeSatoshis: Long = originalInputAmount.subtract(originalOutputAmount).value
        val targetFeeRateSatPerVb: Long = feeRate.setScale(0, RoundingMode.UP).longValueExact()
        val extraUtxos: List<WalletOutput> = findAdditionalUtxos(originalInputs, localUtxos, maxExtraInputs)
        val minimumChangeAmount: Coin = Coin.valueOf(minimumChangeSatoshis)
        val changeIndex: Int = originalOutputs.indexOfFirst { output -> output.scriptBytes.contentEquals(fromScriptBytes) }
        for (extraCount: Int in 0..extraUtxos.size) {
            val usedExtraUtxos: List<WalletOutput> = extraUtxos.take(extraCount)
            val extraInputAmount: Coin = usedExtraUtxos.fold(Coin.ZERO) { acc, utxo -> acc.add(Coin.parseCoin(utxo.amount)) }
            if (changeIndex < 0 && extraInputAmount.isZero) continue
            val candidateTx = BtcTransaction()
            addInputs(candidateTx, originalInputs, usedExtraUtxos)
            for (output: TransactionOutput in originalOutputs) {
                candidateTx.addOutput(output.value, Script.parse(output.scriptBytes))
            }
            if (changeIndex < 0) {
                candidateTx.addOutput(minimumChangeAmount, Script.parse(fromScriptBytes))
            }
            val virtualSize: Int = estimateVirtualSize(candidateTx, chainId)
            val desiredTotalFeeSatoshis: Long = calculateRbfRequiredTotalFeeSatoshis(
                oldTotalFeeSatoshis = oldTotalFeeSatoshis,
                replacementVirtualSize = virtualSize,
                targetFeeRateSatPerVb = targetFeeRateSatPerVb,
            )
            val feeDelta: Coin = Coin.valueOf(desiredTotalFeeSatoshis - oldTotalFeeSatoshis)
            if (feeDelta.isZero || feeDelta.isNegative) {
                return cleanedRawHex
            }
            val updatedChange: Coin = if (changeIndex >= 0) {
                originalOutputs[changeIndex].value.add(extraInputAmount).subtract(feeDelta)
            } else {
                extraInputAmount.subtract(feeDelta)
            }
            if (updatedChange.isNegative) {
                continue
            }
            if (updatedChange.isLessThan(minimumChangeAmount) && extraCount < extraUtxos.size) {
                continue
            }
            val replacementTx = BtcTransaction()
            addInputs(replacementTx, originalInputs, usedExtraUtxos)
            originalOutputs.forEachIndexed { index, output ->
                if (index != changeIndex) {
                    replacementTx.addOutput(output.value, Script.parse(output.scriptBytes))
                } else if (updatedChange >= minimumChangeAmount) {
                    replacementTx.addOutput(updatedChange, Script.parse(output.scriptBytes))
                }
            }
            if (changeIndex < 0 && updatedChange >= minimumChangeAmount) {
                replacementTx.addOutput(updatedChange, Script.parse(fromScriptBytes))
            }
            return replacementTx.serialize().toHex()
        }
        throw InsufficientBtcBalanceException()
    }

    fun buildCancelReplacement(
        chainId: String = Constants.ChainId.BITCOIN_CHAIN_ID,
        rawTransactionHex: String,
        fromAddress: String,
        localUtxos: List<WalletOutput>,
        feeRate: BigDecimal,
        minimumChangeSatoshis: Long = minimumChangeSatoshis(chainId),
        maxExtraInputs: Int = 2,
    ): String {
        validateUtxos(chainId, fromAddress, localUtxos)
        val cleanedRawHex: String = rawTransactionHex.removePrefix("0x").trim()
        require(chainId != Constants.ChainId.PEARL_CHAIN_ID || isReplaceable(cleanedRawHex)) {
            "Pearl transaction does not signal RBF"
        }
        val originalTx: BtcTransaction = BtcTransaction.read(ByteBuffer.wrap(cleanedRawHex.hexStringToByteArray()))
        val originalInputs: List<TransactionInput> = originalTx.inputs
        val extraUtxos: List<WalletOutput> = findAdditionalUtxos(originalInputs, localUtxos, maxExtraInputs)
        val selfScript: Script = buildOutputScript(chainId, fromAddress)
        val minimumChangeAmount: Coin = Coin.valueOf(minimumChangeSatoshis)
        val originalInputAmount: Coin = calculateInputAmount(originalInputs, localUtxos)
        val originalOutputAmount: Coin = originalTx.outputs.fold(Coin.ZERO) { acc: Coin, output: TransactionOutput ->
            acc.add(output.value)
        }
        val oldTotalFeeSatoshis: Long = originalInputAmount.subtract(originalOutputAmount).value
        val targetFeeRateSatPerVb: Long = feeRate.setScale(0, RoundingMode.UP).longValueExact()
        for (extraCount: Int in 0..extraUtxos.size) {
            val usedExtraUtxos: List<WalletOutput> = extraUtxos.take(extraCount)
            val extraInputAmount: Coin = usedExtraUtxos.fold(Coin.ZERO) { acc: Coin, utxo: WalletOutput ->
                acc.add(Coin.parseCoin(utxo.amount))
            }
            val candidateInputAmount: Coin = originalInputAmount.add(extraInputAmount)
            val sizeTx = BtcTransaction()
            addInputs(sizeTx, originalInputs, usedExtraUtxos)
            sizeTx.addOutput(minimumChangeAmount, selfScript)
            val replacementVSize: Int = estimateVirtualSize(sizeTx, chainId)
            val desiredTotalFeeSatoshis: Long = calculateRbfRequiredTotalFeeSatoshis(
                oldTotalFeeSatoshis = oldTotalFeeSatoshis,
                replacementVirtualSize = replacementVSize,
                targetFeeRateSatPerVb = targetFeeRateSatPerVb,
            )
            val sendToSelf: Coin = candidateInputAmount.subtract(Coin.valueOf(desiredTotalFeeSatoshis))
            if (sendToSelf.isNegative || sendToSelf.isZero || sendToSelf.isLessThan(minimumChangeAmount)) {
                if (extraCount < extraUtxos.size) {
                    continue
                }
                throw InsufficientBtcBalanceException()
            }
            val oneOutputTx = BtcTransaction()
            addInputs(oneOutputTx, originalInputs, usedExtraUtxos)
            oneOutputTx.addOutput(sendToSelf, selfScript)
            return oneOutputTx.serialize().toHex()
        }
        throw InsufficientBtcBalanceException()
    }

    private fun addInputs(tx: BtcTransaction, inputs: List<TransactionInput>, extraUtxos: List<WalletOutput>) {
        for (input: TransactionInput in inputs) {
            val outPoint = TransactionOutPoint(input.outpoint.index(), input.outpoint.hash())
            val txInput = TransactionInput(tx, byteArrayOf(), outPoint)
            tx.addInput(txInput.withSequence(RBF_SEQUENCE))
        }
        for (utxo: WalletOutput in extraUtxos) {
            val outPoint = TransactionOutPoint(utxo.outputIndex, Sha256Hash.wrap(utxo.transactionHash))
            val txInput = TransactionInput(tx, byteArrayOf(), outPoint)
            tx.addInput(txInput.withSequence(RBF_SEQUENCE))
        }
    }

    fun isReplaceable(rawTransactionHex: String): Boolean {
        val cleanedRawHex: String = rawTransactionHex.removePrefix("0x").trim()
        if (cleanedRawHex.isBlank()) return false
        val transaction: BtcTransaction = runCatching {
            BtcTransaction.read(ByteBuffer.wrap(cleanedRawHex.hexStringToByteArray()))
        }.getOrNull() ?: return false
        return transaction.inputs.any { input -> input.sequenceNumber <= RBF_SEQUENCE }
    }

    fun canCancel(
        chainId: String,
        rawTransactionHex: String,
        fromAddress: String,
    ): Boolean {
        if (chainId !in Constants.Web3UtxoChainIds) return false
        val cleanedRawHex: String = rawTransactionHex.removePrefix("0x").trim()
        if (cleanedRawHex.isBlank()) return false
        val transaction: BtcTransaction = runCatching {
            BtcTransaction.read(ByteBuffer.wrap(cleanedRawHex.hexStringToByteArray()))
        }.getOrNull() ?: return false
        if (transaction.outputs.size != 1) return true
        val selfScript: Script = runCatching { buildOutputScript(chainId, fromAddress) }.getOrNull() ?: return false
        return !transaction.outputs.single().scriptBytes.contentEquals(selfScript.program())
    }

    fun virtualSize(
        chainId: String,
        rawTransactionHex: String,
    ): Int {
        require(chainId in Constants.Web3UtxoChainIds) { "Unsupported UTXO chain: $chainId" }
        val cleanedRawHex: String = rawTransactionHex.removePrefix("0x").trim()
        val transaction: BtcTransaction = BtcTransaction.read(ByteBuffer.wrap(cleanedRawHex.hexStringToByteArray()))
        return estimateVirtualSize(transaction, chainId)
    }

    fun fee(
        rawTransactionHex: String,
        localUtxos: List<WalletOutput>,
    ): BigDecimal {
        val cleanedRawHex: String = rawTransactionHex.removePrefix("0x").trim()
        val transaction: BtcTransaction = BtcTransaction.read(ByteBuffer.wrap(cleanedRawHex.hexStringToByteArray()))
        return BigDecimal.valueOf(calculateFeeSatoshi(transaction, localUtxos)).divide(satoshisPerBtc)
    }

    private fun buildOutputScript(chainId: String, address: String): Script =
        ScriptBuilder.createOutputScript(parseAddress(chainId, address))

    internal fun minimumTransferAmount(chainId: String): BigDecimal =
        BigDecimal.valueOf(minimumOutputSatoshis(chainId))
            .divide(satoshisPerBtc)
            .stripTrailingZeros()

    private fun minimumChangeSatoshis(chainId: String): Long = minimumOutputSatoshis(chainId)

    private fun minimumOutputSatoshis(chainId: String): Long =
        when (chainId) {
            Constants.ChainId.BITCOIN_CHAIN_ID -> BTC_MINIMUM_OUTPUT_SATOSHIS
            Constants.ChainId.PEARL_CHAIN_ID -> PEARL_MINIMUM_OUTPUT_SATOSHIS
            else -> throw IllegalArgumentException("Unsupported UTXO chain: $chainId")
        }

    private fun validateUtxos(chainId: String, fromAddress: String, localUtxos: List<WalletOutput>) {
        require(chainId in Constants.Web3UtxoChainIds) { "Unsupported UTXO chain: $chainId" }
        if (localUtxos.isEmpty()) throw InsufficientBtcBalanceException()
        require(localUtxos.all { it.assetId == chainId && it.address == fromAddress }) {
            "UTXOs do not belong to the selected chain and address"
        }
    }

    private fun parseAddress(chainId: String, address: String): Address =
        when (chainId) {
            Constants.ChainId.BITCOIN_CHAIN_ID -> AddressParser.getDefault(BitcoinNetwork.MAINNET).parseAddress(address)
            Constants.ChainId.PEARL_CHAIN_ID -> PearlKeyGenerator.parseAddress(address)
            else -> throw IllegalArgumentException("Unsupported UTXO chain: $chainId")
        }

    private fun estimateVirtualSize(transaction: BtcTransaction, chainId: String): Int {
        if (chainId != Constants.ChainId.PEARL_CHAIN_ID) return transaction.vsize
        val originalInputs = transaction.inputs.toList()
        originalInputs.forEachIndexed { index, input ->
            transaction.replaceInput(index, input.withWitness(org.bitcoinj.core.TransactionWitness.of(ByteArray(64))))
        }
        val virtualSize = transaction.vsize
        originalInputs.forEachIndexed(transaction::replaceInput)
        return virtualSize
    }

    private fun calculateInputAmount(inputs: List<TransactionInput>, localUtxos: List<WalletOutput>): Coin {
        var total: Coin = Coin.ZERO
        for (input: TransactionInput in inputs) {
            val utxo = localUtxos.firstOrNull { local ->
                local.transactionHash.equals(input.outpoint.hash().toString(), ignoreCase = true) &&
                    local.outputIndex == input.outpoint.index()
            } ?: throw IllegalArgumentException("Missing UTXO ${input.outpoint.hash()}:${input.outpoint.index()}")
            total = total.add(Coin.parseCoin(utxo.amount))
        }
        return total
    }

    private fun findAdditionalUtxos(existingInputs: List<TransactionInput>, localUtxos: List<WalletOutput>, maxCount: Int): List<WalletOutput> {
        val result: MutableList<WalletOutput> = mutableListOf()
        for (utxo: WalletOutput in localUtxos) {
            if (utxo.status != "unspent") continue
            val exists: Boolean = existingInputs.any { input ->
                utxo.transactionHash.equals(input.outpoint.hash().toString(), ignoreCase = true) &&
                    utxo.outputIndex == input.outpoint.index()
            }
            if (!exists) {
                result.add(utxo)
            }
            if (result.size >= maxCount) {
                break
            }
        }
        return result
    }

    private fun calculateFeeSatoshi(tx: BtcTransaction, localUtxos: List<WalletOutput>): Long {
        val inputAmount: Coin = calculateInputAmount(tx.inputs, localUtxos)
        val outputAmount: Coin = tx.outputs.fold(Coin.ZERO) { acc, output -> acc.add(output.value) }
        return inputAmount.subtract(outputAmount).value
    }
}

class InsufficientBtcBalanceException(
    val feeBtc: BigDecimal = BigDecimal.ZERO,
    val utxoTotalBtc: BigDecimal = BigDecimal.ZERO,
) : IllegalArgumentException("insufficient btc balance")
