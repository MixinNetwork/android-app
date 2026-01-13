package one.mixin.android.web3.send

import one.mixin.android.api.response.web3.WalletOutput
import one.mixin.android.db.web3.vo.virtualSize
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.toHex
import org.bitcoinj.base.AddressParser
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

object BtcTransactionBuilder {

    private const val BTC_RBF_SEQUENCE: Long = 0xfffffffdL
    private val satoshisPerBtc: BigDecimal = BigDecimal("100000000")

    data class BuiltBtcTransaction(
        val rawHex: String,
        val feeBtc: BigDecimal,
        val virtualSize: Int,
        val changeAmount: Coin,
    )

    fun buildSendTransaction(
        fromAddress: String,
        toAddress: String,
        amountBtc: String,
        localUtxos: List<WalletOutput>,
        feeRate: BigDecimal,
        minimumChangeSatoshis: Long = 1000L,
    ): BuiltBtcTransaction {
        val addressParser = AddressParser.getDefault()
        val changeAddress = addressParser.parseAddress(fromAddress)
        val recipientAddress = addressParser.parseAddress(toAddress)
        val sendAmount = Coin.parseCoin(amountBtc)
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
                candidateTx.addInput(input)
            }
            virtualSize = candidateTx.virtualSize()
            val feeSatoshis: BigDecimal = feeRate.multiply(BigDecimal(virtualSize)).setScale(0, RoundingMode.UP)
            feeBtc = feeSatoshis.divide(satoshisPerBtc, 8, RoundingMode.HALF_UP)
            val targetAmount: Coin = sendAmount.add(Coin.parseCoin(feeBtc.toPlainString()))
            changeAmount = selectedAmount.subtract(targetAmount)
            if (changeAmount.isNegative) {
                continue
            }
            if (changeAmount.isGreaterThan(minimumChangeAmount) || changeAmount == minimumChangeAmount) {
                candidateTx.addOutput(changeAmount, changeAddress)
                virtualSize = candidateTx.virtualSize()
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
            throw IllegalArgumentException("insufficient balance")
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
            tx.addInput(input)
        }
        virtualSize = tx.virtualSize()
        val feeSatoshi: BigDecimal = BigDecimal.valueOf(calculateFeeSatoshi(tx, localUtxos))
        val finalFeeBtc: BigDecimal = feeSatoshi.divide(satoshisPerBtc)
        return BuiltBtcTransaction(rawHex = tx.serialize().toHex(), feeBtc = finalFeeBtc, virtualSize = virtualSize, changeAmount = changeAmount)
    }

    fun buildSpeedUpReplacement(
        rawTransactionHex: String,
        fromAddress: String,
        localUtxos: List<WalletOutput>,
        feeRate: BigDecimal,
        minimumChangeSatoshis: Long = 1000L,
        maxExtraInputs: Int = 2,
    ): String {
        val cleanedRawHex: String = rawTransactionHex.removePrefix("0x").trim()
        val originalTx: BtcTransaction = BtcTransaction.read(java.nio.ByteBuffer.wrap(cleanedRawHex.hexStringToByteArray()))
        val fromScriptBytes: ByteArray = buildP2wpkhScript(fromAddress).program()
        val originalInputs: List<TransactionInput> = originalTx.inputs
        val originalOutputs: List<TransactionOutput> = originalTx.outputs
        val extraUtxos: List<WalletOutput> = findAdditionalUtxos(originalInputs, localUtxos, maxExtraInputs)
        val minimumChangeAmount: Coin = Coin.valueOf(minimumChangeSatoshis)
        for (extraCount: Int in 0..extraUtxos.size) {
            val usedExtraUtxos: List<WalletOutput> = extraUtxos.take(extraCount)
            val extraInputAmount: Coin = usedExtraUtxos.fold(Coin.ZERO) { acc, utxo -> acc.add(Coin.parseCoin(utxo.amount)) }
            val inputAmount: Coin = calculateInputAmount(originalInputs, localUtxos).add(extraInputAmount)
            val outputAmount: Coin = originalOutputs.fold(Coin.ZERO) { acc, output -> acc.add(output.value) }
            val currentFee: Coin = inputAmount.subtract(outputAmount)
            val candidateTx = BtcTransaction()
            addInputs(candidateTx, originalInputs, usedExtraUtxos)
            for (output: TransactionOutput in originalOutputs) {
                candidateTx.addOutput(output.value, Script.parse(output.scriptBytes))
            }
            val virtualSize: Int = candidateTx.virtualSize()
            val desiredFeeSats: BigDecimal = feeRate.multiply(BigDecimal(virtualSize)).setScale(0, RoundingMode.UP)
            val desiredFee: Coin = Coin.valueOf(desiredFeeSats.longValueExact())
            val feeDelta: Coin = desiredFee.subtract(currentFee)
            if (feeDelta.isZero || feeDelta.isNegative) {
                return cleanedRawHex
            }
            val changeIndex: Int = originalOutputs.indexOfFirst { output -> output.scriptBytes.contentEquals(fromScriptBytes) }
            val currentChange: Coin? = if (changeIndex >= 0) originalOutputs[changeIndex].value else null
            val updatedChange: Coin? = if (changeIndex >= 0) {
                currentChange!!.add(extraInputAmount).subtract(feeDelta)
            } else if (!extraInputAmount.isZero) {
                extraInputAmount.subtract(feeDelta)
            } else {
                null
            }
            val isSubDustChange: Boolean = updatedChange != null && !updatedChange.isNegative && !updatedChange.isZero && updatedChange.isLessThan(minimumChangeAmount)
            if (isSubDustChange && extraCount < extraUtxos.size) {
                continue
            }
            val adjustedOutputs: List<Pair<Coin, Script>> = buildAdjustedOutputs(
                originalOutputs = originalOutputs,
                fromScriptBytes = fromScriptBytes,
                feeDelta = feeDelta,
                additionalInputAmount = extraInputAmount,
            ).map { (value, script) ->
                if (script.program().contentEquals(fromScriptBytes) && updatedChange != null) {
                    updatedChange to script
                } else {
                    value to script
                }
            }.filter { (value, script) ->
                if (script.program().contentEquals(fromScriptBytes)) {
                    value.isZero || value.isGreaterThan(minimumChangeAmount) || value == minimumChangeAmount
                } else {
                    true
                }
            }
            val replacementTx = BtcTransaction()
            addInputs(replacementTx, originalInputs, usedExtraUtxos)
            for ((value, script) in adjustedOutputs) {
                if (script.program().contentEquals(fromScriptBytes)) {
                    if (value.isGreaterThan(minimumChangeAmount) || value == minimumChangeAmount) {
                        replacementTx.addOutput(value, script)
                    }
                } else {
                    replacementTx.addOutput(value, script)
                }
            }
            return replacementTx.serialize().toHex()
        }
        throw IllegalArgumentException("insufficient balance")
    }

    fun buildCancelReplacement(
        rawTransactionHex: String,
        fromAddress: String,
        localUtxos: List<WalletOutput>,
        feeRate: BigDecimal,
        minimumChangeSatoshis: Long = 1000L,
        maxExtraInputs: Int = 2,
    ): String {
        val cleanedRawHex: String = rawTransactionHex.removePrefix("0x").trim()
        val originalTx: BtcTransaction = BtcTransaction.read(java.nio.ByteBuffer.wrap(cleanedRawHex.hexStringToByteArray()))
        val originalInputs: List<TransactionInput> = originalTx.inputs
        val extraUtxos: List<WalletOutput> = findAdditionalUtxos(originalInputs, localUtxos, maxExtraInputs)
        val selfScript: Script = buildP2wpkhScript(fromAddress)
        val originalOutputValues: List<Coin> = originalTx.outputs.map { it.value }
        val minimumChangeAmount: Coin = Coin.valueOf(minimumChangeSatoshis)
        for (extraCount: Int in 0..extraUtxos.size) {
            val usedExtraUtxos: List<WalletOutput> = extraUtxos.take(extraCount)
            val candidateInputAmount: Coin = calculateInputAmount(originalInputs, localUtxos).add(
                usedExtraUtxos.fold(Coin.ZERO) { acc, utxo -> acc.add(Coin.parseCoin(utxo.amount)) }
            )
            val replacementTx = BtcTransaction()
            addInputs(replacementTx, originalInputs, usedExtraUtxos)
            for (value: Coin in originalOutputValues) {
                replacementTx.addOutput(value, selfScript)
            }
            val virtualSize: Int = replacementTx.virtualSize()
            val desiredFeeSats: BigDecimal = feeRate.multiply(BigDecimal(virtualSize)).setScale(0, RoundingMode.UP)
            val desiredFee: Coin = Coin.valueOf(desiredFeeSats.longValueExact())
            val currentFee: Coin = candidateInputAmount.subtract(originalTx.outputs.fold(Coin.ZERO) { acc, output -> acc.add(output.value) })
            val feeDelta: Coin = desiredFee.subtract(currentFee)
            if (feeDelta.isZero || feeDelta.isNegative) {
                return cleanedRawHex
            }
            val maxIndex: Int = originalOutputValues.indices.maxByOrNull { index -> originalOutputValues[index].value } ?: -1
            if (maxIndex < 0) {
                throw IllegalArgumentException("insufficient balance")
            }
            val adjustedMax: Coin = originalOutputValues[maxIndex].subtract(feeDelta)
            if (adjustedMax.isNegative || adjustedMax.isZero) {
                continue
            }
            if (adjustedMax.isLessThan(minimumChangeAmount)) {
                if (extraCount < extraUtxos.size) {
                    continue
                }
                val sendToSelf: Coin = candidateInputAmount.subtract(desiredFee)
                if (sendToSelf.isNegative || sendToSelf.isZero || sendToSelf.isLessThan(minimumChangeAmount)) {
                    throw IllegalArgumentException("insufficient balance")
                }
                val oneOutputTx = BtcTransaction()
                addInputs(oneOutputTx, originalInputs, usedExtraUtxos)
                oneOutputTx.addOutput(sendToSelf, selfScript)
                return oneOutputTx.serialize().toHex()
            }
            val adjustedTx = BtcTransaction()
            addInputs(adjustedTx, originalInputs, usedExtraUtxos)
            originalOutputValues.forEachIndexed { index, value ->
                val outputValue: Coin = if (index == maxIndex) adjustedMax else value
                adjustedTx.addOutput(outputValue, selfScript)
            }
            return adjustedTx.serialize().toHex()
        }
        throw IllegalArgumentException("insufficient balance")
    }

    private fun addInputs(tx: BtcTransaction, inputs: List<TransactionInput>, extraUtxos: List<WalletOutput>) {
        for (input: TransactionInput in inputs) {
            val outPoint = TransactionOutPoint(input.outpoint.index(), input.outpoint.hash())
            val txInput = TransactionInput(tx, byteArrayOf(), outPoint)
            txInput.withSequence(BTC_RBF_SEQUENCE)
            tx.addInput(txInput)
        }
        for (utxo: WalletOutput in extraUtxos) {
            val outPoint = TransactionOutPoint(utxo.outputIndex, Sha256Hash.wrap(utxo.transactionHash))
            val txInput = TransactionInput(tx, byteArrayOf(), outPoint)
            txInput.withSequence(BTC_RBF_SEQUENCE)
            tx.addInput(txInput)
        }
    }

    private fun buildAdjustedOutputs(
        originalOutputs: List<TransactionOutput>,
        fromScriptBytes: ByteArray,
        feeDelta: Coin,
        additionalInputAmount: Coin,
    ): List<Pair<Coin, Script>> {
        val outputs: MutableList<Pair<Coin, Script>> = mutableListOf()
        val changeIndex: Int = originalOutputs.indexOfFirst { output -> output.scriptBytes.contentEquals(fromScriptBytes) }
        for ((index, output) in originalOutputs.withIndex()) {
            if (index != changeIndex) {
                outputs.add(output.value to Script.parse(output.scriptBytes))
                continue
            }
            val updatedChange: Coin = output.value.add(additionalInputAmount).subtract(feeDelta)
            outputs.add(updatedChange to Script.parse(output.scriptBytes))
        }
        if (changeIndex < 0 && !additionalInputAmount.isZero) {
            val updatedChange: Coin = additionalInputAmount.subtract(feeDelta)
            outputs.add(updatedChange to Script.parse(fromScriptBytes))
        }
        return outputs
    }

    private fun buildP2wpkhScript(address: String): Script {
        val addressParser: AddressParser = AddressParser.getDefault()
        val parsedAddress = addressParser.parseAddress(address)
        return ScriptBuilder.createOutputScript(parsedAddress)
    }

    private fun calculateInputAmount(inputs: List<TransactionInput>, localUtxos: List<WalletOutput>): Coin {
        var total: Coin = Coin.ZERO
        for (input: TransactionInput in inputs) {
            val utxo = localUtxos.firstOrNull { local ->
                local.transactionHash.equals(input.outpoint.hash().toString(), ignoreCase = true) &&
                    local.outputIndex == input.outpoint.index()
            } ?: continue
            total = total.add(Coin.parseCoin(utxo.amount))
        }
        return total
    }

    private fun findAdditionalUtxos(existingInputs: List<TransactionInput>, localUtxos: List<WalletOutput>, maxCount: Int): List<WalletOutput> {
        val result: MutableList<WalletOutput> = mutableListOf()
        for (utxo: WalletOutput in localUtxos) {
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
