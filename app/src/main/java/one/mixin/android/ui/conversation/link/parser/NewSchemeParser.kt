package one.mixin.android.ui.conversation.link.parser
import androidx.core.net.toUri
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.isUUID
import one.mixin.android.extension.navTo
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.putString
import one.mixin.android.pay.parseExternalTransferUri
import one.mixin.android.session.Session
import one.mixin.android.ui.common.QrScanBottomSheetDialogFragment
import one.mixin.android.ui.common.UtxoConsolidationBottomSheetDialogFragment
import one.mixin.android.ui.common.WaitingBottomSheetDialogFragment
import one.mixin.android.ui.common.biometric.AddressTransferBiometricItem
import one.mixin.android.ui.common.biometric.AssetBiometricItem
import one.mixin.android.ui.common.biometric.NftBiometricItem
import one.mixin.android.ui.common.biometric.TransferBiometricItem
import one.mixin.android.ui.common.biometric.WithdrawBiometricItem
import one.mixin.android.ui.common.biometric.buildAddressBiometricItem
import one.mixin.android.ui.common.biometric.buildTransferBiometricItem
import one.mixin.android.ui.conversation.link.CollectionBottomSheetDialogFragment
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.wallet.AssetListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.AssetListBottomSheetDialogFragment.Companion.ASSET_PREFERENCE
import one.mixin.android.ui.wallet.AssetListBottomSheetDialogFragment.Companion.TYPE_FROM_TRANSFER
import one.mixin.android.ui.wallet.InputFragment
import one.mixin.android.ui.wallet.NetworkFee
import one.mixin.android.ui.wallet.transfer.TransferBottomSheetDialogFragment
import one.mixin.android.ui.wallet.transfer.TransferInvoiceBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.Address
import one.mixin.android.vo.InscriptionCollection
import one.mixin.android.vo.InscriptionItem
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.toUser
import timber.log.Timber
import java.math.BigDecimal
import java.util.UUID

class NewSchemeParser(
    private val bottomSheet: LinkBottomSheetDialogFragment,
) {
    companion object {
        const val INSCRIPTION_NOT_FOUND = -2
        const val INSUFFICIENT_BALANCE = -1
        const val FAILURE = 0
        const val SUCCESS = 1
    }

    private val linkViewModel = bottomSheet.linkViewModel

    suspend fun parse(
        text: String,
        from: Int,
    ): Result<Int> {
        try {
            val urlQueryParser = UrlQueryParser(text.toUri(), from)

            bottomSheet.syncUtxo()
            val payType = urlQueryParser.payType
            val asset = urlQueryParser.asset
            val amount = urlQueryParser.amount
            val traceId = urlQueryParser.trace ?: UUID.randomUUID().toString()
            if ((asset != null && amount != null) || urlQueryParser.inscription != null || urlQueryParser.inscriptionCollection != null) {
                val status = getPaymentStatus(traceId) ?: return Result.failure(ParserError(FAILURE))
                if (status == PaymentStatus.paid.name) return Result.failure(ParserError(FAILURE, message = bottomSheet.getString(R.string.pay_paid)))
                val token: TokenItem?
                if (urlQueryParser.inscription == null && urlQueryParser.inscriptionCollection == null) {
                    token = checkToken(asset!!) ?: return Result.failure(ParserError(FAILURE)) // TODO 404?
                    val tokensExtra = linkViewModel.findTokensExtra(asset)
                    if (tokensExtra == null) {
                        return Result.failure(ParserError(INSUFFICIENT_BALANCE, token.symbol))
                    } else if (BigDecimal(tokensExtra.balance ?: "0") < BigDecimal(amount)) {
                        return Result.failure(ParserError(INSUFFICIENT_BALANCE, token.symbol))
                    }
                } else {
                    token = null
                }

                if (payType == PayType.Uuid) {
                    val user = linkViewModel.refreshUser(urlQueryParser.userId) ?: return Result.failure(ParserError(FAILURE))
                    if (urlQueryParser.inscriptionCollection != null) {
                        checkInscriptionCollection(urlQueryParser.inscriptionCollection!!) ?: return Result.failure(ParserError(INSCRIPTION_NOT_FOUND, message = bottomSheet.getString(R.string.collectible_not_found)))
                        CollectionBottomSheetDialogFragment.newInstance(urlQueryParser.inscriptionCollection!!, traceId, urlQueryParser.userId, urlQueryParser.memo).showNow(bottomSheet.parentFragmentManager, CollectionBottomSheetDialogFragment.TAG)
                        return Result.success(SUCCESS)
                    }
                    val biometricItem =
                        if (urlQueryParser.inscription != null) {
                            buildInscriptionTransfer(urlQueryParser, user.userId, traceId)
                        } else {
                            TransferBiometricItem(listOf(user), 1, traceId, token, requireNotNull(amount), urlQueryParser.memo, status, null, urlQueryParser.returnTo, reference = urlQueryParser.reference)
                        }
                    checkRawTransaction(biometricItem)
                } else if (payType == PayType.MixAddress) {
                    val mixAddress = urlQueryParser.mixAddress
                    if (mixAddress.uuidMembers.isNotEmpty()) {
                        val users = linkViewModel.findOrRefreshUsers(mixAddress.uuidMembers)
                        if (users.isEmpty() || users.size < mixAddress.uuidMembers.size) {
                            return Result.failure(ParserError(FAILURE))
                        }
                        val biometricItem = TransferBiometricItem(users, mixAddress.threshold, traceId, token, requireNotNull(amount), urlQueryParser.memo, status, null, urlQueryParser.returnTo, reference = urlQueryParser.reference)
                        checkRawTransaction(biometricItem)
                    } else if (mixAddress.xinMembers.isNotEmpty()) {
                        val addressTransferBiometricItem = AddressTransferBiometricItem(mixAddress.xinMembers.first().string(), traceId, token, requireNotNull(amount), urlQueryParser.memo, status, urlQueryParser.returnTo, reference = urlQueryParser.reference)
                        checkRawTransaction(addressTransferBiometricItem)
                    } else {
                        return Result.failure(ParserError(FAILURE))
                    }
                } else {
                    // TODO verify address?
                    val addressTransferBiometricItem = AddressTransferBiometricItem(urlQueryParser.lastPath, traceId, token, requireNotNull(amount), urlQueryParser.memo, status, urlQueryParser.returnTo, reference = urlQueryParser.reference)
                    checkRawTransaction(addressTransferBiometricItem)
                }
            } else if (payType  == PayType.Invoice) {
                val invoice = urlQueryParser.mixInvoice
                invoice.recipient.members().filter {
                    it.isUUID()
                }.forEach {
                    linkViewModel.refreshUser(it) ?: return Result.failure(ParserError(FAILURE))
                }
                val rawTransaction = linkViewModel.firstUnspentTransaction()
                if (rawTransaction != null) {
                    WaitingBottomSheetDialogFragment.newInstance().showNow(bottomSheet.parentFragmentManager, WaitingBottomSheetDialogFragment.TAG)
                    return Result.success(SUCCESS)
                }
                val traces = invoice.entries.map { it.traceId }
                val response = linkViewModel.transactionsFetch(traces)
                if (response.isSuccess && response.data.isNullOrEmpty().not()) {
                    return if ((response.data?.size ?: 0) == traces.size) {
                        Result.failure(ParserError(FAILURE, message = bottomSheet.getString(R.string.pay_paid)))
                    } else {
                        Result.failure(ParserError(FAILURE))
                    }
                }
                invoice.entries.forEach { entry ->
                    if (!checkUtxo(entry.assetId, entry.amountString())) {
                        return Result.success(SUCCESS)
                    }
                }
                val bottom = TransferInvoiceBottomSheetDialogFragment.newInstance(invoice.toString())
                bottom.show(bottomSheet.parentFragmentManager, TransferInvoiceBottomSheetDialogFragment.TAG)
                return Result.success(SUCCESS)
            } else {
                val token = asset?.let { checkToken(it) ?: return Result.failure(ParserError(FAILURE)) }
                if (token == null) {
                    val bottom = AssetListBottomSheetDialogFragment.newInstance(TYPE_FROM_TRANSFER)
                        .apply {
                            asyncOnAsset = { selectedAsset ->
                                bottomSheet.requireContext().defaultSharedPreferences.putString(ASSET_PREFERENCE, selectedAsset.assetId)
                                val inputFragment = createInputFragment(selectedAsset, payType, urlQueryParser, amount, traceId, from)
                                if (inputFragment != null) {
                                    activity?.addFragment(this, inputFragment, InputFragment.TAG)
                                }
                            }
                        }
                    bottom.show(bottomSheet.parentFragmentManager, AssetListBottomSheetDialogFragment.TAG)
                } else {
                    val inputFragment = createInputFragment(token, payType, urlQueryParser, amount, traceId, from)
                    if (inputFragment == null) return Result.failure(ParserError(FAILURE))
                    bottomSheet.navTo(inputFragment, InputFragment.TAG)
                }
            }
            return Result.success(SUCCESS)
        } catch (e: Exception) {
            Timber.e(e)
            return Result.failure(e)
        }
    }

    private suspend fun createInputFragment(asset: TokenItem, payType:PayType, urlQueryParser: UrlQueryParser, amount: String? = null, traceId: String, from: Int): InputFragment? {
        return when (payType) {
            PayType.Uuid -> {
                val user = linkViewModel.refreshUser(urlQueryParser.userId) ?: return null
                InputFragment.newInstance(buildTransferBiometricItem(user, asset, amount ?: "", traceId, urlQueryParser.memo, urlQueryParser.returnTo, reference = urlQueryParser.reference))
            }
            PayType.MixAddress -> {
                val mixAddress = urlQueryParser.mixAddress
                when {
                    mixAddress.uuidMembers.isNotEmpty() -> {
                        if (mixAddress.uuidMembers.size == 1) {
                            val user = linkViewModel.refreshUser(mixAddress.uuidMembers.first()) ?: return null
                            InputFragment.newInstance(buildTransferBiometricItem(user, asset, amount ?: "", traceId, urlQueryParser.memo, urlQueryParser.returnTo, reference = urlQueryParser.reference))
                        } else {
                            val users = linkViewModel.findOrRefreshUsers(mixAddress.uuidMembers)
                            if (users.isEmpty() || users.size < mixAddress.uuidMembers.size) return null
                            val item = TransferBiometricItem(users, mixAddress.threshold, traceId, asset, amount ?: "", urlQueryParser.memo, PaymentStatus.pending.name, null, urlQueryParser.returnTo, reference = urlQueryParser.reference)
                            InputFragment.newInstance(item)
                        }
                    }
                    mixAddress.xinMembers.size == 1 -> {
                        InputFragment.newInstance(buildAddressBiometricItem(mixAddress.xinMembers.first().string(), traceId, asset, amount ?: "", urlQueryParser.memo, urlQueryParser.returnTo, from, reference = urlQueryParser.reference))
                    }
                    else -> null
                }
            }
            else -> InputFragment.newInstance(buildAddressBiometricItem(urlQueryParser.lastPath, traceId, asset, amount ?: "", urlQueryParser.memo, urlQueryParser.returnTo, from, reference = urlQueryParser.reference))
        }
    }

    private suspend fun buildInscriptionTransfer(
        urlQueryParser: UrlQueryParser,
        userId: String,
        traceId: String,
    ): NftBiometricItem {
        val inscriptionHash = urlQueryParser.inscription ?: throw ParserError(FAILURE)
        val inscription = checkInscription(inscriptionHash) ?: throw ParserError(FAILURE)
        val assetId = urlQueryParser.asset

        val inscriptionCollection = checkInscriptionCollection(inscription.collectionHash) ?: throw ParserError(FAILURE)
        val releaseAmount =
            if (urlQueryParser.amount != null) {
                val amount = BigDecimal(urlQueryParser.amount)
                val preAmount = inscriptionCollection.preAmount
                if (amount <= BigDecimal.ZERO || amount > preAmount) throw ParserError(FAILURE)
                if (amount == preAmount) null else{
                    // specify asset for release inscription
                    if (assetId == null) throw ParserError(FAILURE)
                    urlQueryParser.amount
                }
            } else {
                null
            }
        if (releaseAmount != null && userId != Session.getAccountId()) throw ParserError(FAILURE)
        val receiver = linkViewModel.refreshUser(userId) ?: throw ParserError(FAILURE)
        val output = linkViewModel.findUnspentOutputByHash(inscriptionHash) ?: throw ParserError(INSCRIPTION_NOT_FOUND, message = bottomSheet.getString(R.string.collectible_not_found))
        val token = if (assetId != null) {
            checkToken(assetId)
        } else {
            checkTokenByCollectionHash(inscription.collectionHash, inscription.inscriptionHash)
        } ?: throw ParserError(FAILURE)
        if (token.collectionHash != inscription.collectionHash) {
            throw ParserError(FAILURE)
        }
        val nftBiometricItem =
            NftBiometricItem(
                asset = token,
                traceId = traceId,
                amount = output.amount,
                memo = urlQueryParser.memo,
                state = PaymentStatus.pending.name,
                receivers = listOf(receiver),
                reference = null,
                inscriptionItem = inscription,
                inscriptionCollection = inscriptionCollection,
                releaseAmount = releaseAmount,
            )
        return nftBiometricItem
    }

    suspend fun parseExternalTransferUrl(url: String) {
        var errorMsg: String? = null
        val result =
            parseExternalTransferUri(url, { assetId, destination ->
                handleMixinResponse(
                    invokeNetwork = {
                        linkViewModel.validateExternalAddress(assetId, destination, null)
                    },
                    successBlock = {
                        return@handleMixinResponse it.data
                    },
                )
            }, { assetId, destination ->
                handleMixinResponse(
                    invokeNetwork = {
                        linkViewModel.getFees(assetId, destination)
                    },
                    successBlock = {
                        return@handleMixinResponse it.data
                    },
                )
            }, { assetKey ->
                val assetId = linkViewModel.findAssetIdByAssetKey(assetKey)
                if (assetId == null) {
                    errorMsg = bottomSheet.getString(R.string.external_pay_no_asset_found)
                }
                return@parseExternalTransferUri assetId
            }, { assetId ->
                handleMixinResponse(
                    invokeNetwork = {
                        linkViewModel.getAssetPrecisionById(assetId)
                    },
                    successBlock = {
                        return@handleMixinResponse it.data
                    },
                )
            }, { assetId, amount, feeAssetId, feeAmount ->
                if (feeAssetId != null && feeAmount != null) {
                    val tokensExtra = linkViewModel.findTokensExtra(feeAssetId)
                    if (tokensExtra == null) {
                        errorMsg = bottomSheet.getString(R.string.insufficient_balance)
                    } else if (BigDecimal(tokensExtra.balance ?: "0") < feeAmount) {
                        errorMsg = bottomSheet.getString(R.string.insufficient_balance)
                    }
                }

                val tokensExtra = linkViewModel.findTokensExtra(assetId)
                if (tokensExtra == null) {
                    errorMsg = bottomSheet.getString(R.string.insufficient_balance)
                } else if (BigDecimal(tokensExtra.balance ?: "0") < amount) {
                    errorMsg = bottomSheet.getString(R.string.insufficient_balance)
                }
            })

        errorMsg?.let {
            bottomSheet.showError(it)
            return
        }

        if (result == null) {
            QrScanBottomSheetDialogFragment.newInstance(url)
                .show(bottomSheet.parentFragmentManager, QrScanBottomSheetDialogFragment.TAG)
        } else {
            val asset = checkToken(result.assetId)
            if (asset == null) {
                bottomSheet.showError(R.string.Asset_not_found)
                bottomSheet.dismiss()
                return
            }
            val feeAsset = checkToken(result.feeAssetId!!)
            if (feeAsset == null) {
                bottomSheet.showError(R.string.Asset_not_found)
                bottomSheet.dismiss()
                return
            }

            val traceId = UUID.randomUUID().toString()
            val status = getPaymentStatus(traceId)
            if (status == null) {
                bottomSheet.showError()
                bottomSheet.dismiss()
                return
            }
            val amount = result.amount
            val destination = result.destination

            val address = Address("", "address", asset.assetId, destination, "ExternalAddress", nowInUtc(), null, null)
            val fee = NetworkFee(feeAsset, result.fee!!.toPlainString())
            val withdrawBiometricItem = WithdrawBiometricItem(address, fee, null, traceId, asset, amount, result.memo, status, null)
            checkRawTransaction(withdrawBiometricItem)
        }
        bottomSheet.dismiss()
    }

    private suspend fun checkRawTransaction(biometricItem: AssetBiometricItem) {
        val rawTransaction = linkViewModel.firstUnspentTransaction()
        if (rawTransaction != null) {
            WaitingBottomSheetDialogFragment.newInstance().showNow(bottomSheet.parentFragmentManager, WaitingBottomSheetDialogFragment.TAG)
        } else {
            if (biometricItem is TransferBiometricItem && biometricItem.users.size == 1) {
                val pair = linkViewModel.findLatestTrace(biometricItem.users.first().userId, null, null, biometricItem.amount, biometricItem.asset?.assetId ?: "")
                biometricItem.trace = pair.first
            }
            checkUtxo(biometricItem) {
                showPreconditionBottom(biometricItem)
            }
        }
    }

    private suspend fun checkUtxo(
        t: AssetBiometricItem,
        callback: () -> Unit,
    ) {
        val token = t.asset ?: return
        var amount = t.amount
        if (amount.isBlank()) {
            callback.invoke()
        }
        val consolidationAmount = linkViewModel.checkUtxoSufficiency(token.assetId, amount)
        if (consolidationAmount != null) {
            UtxoConsolidationBottomSheetDialogFragment.newInstance(buildTransferBiometricItem(Session.getAccount()!!.toUser(), t.asset, consolidationAmount, UUID.randomUUID().toString(), null, null))
                .show(bottomSheet.parentFragmentManager, UtxoConsolidationBottomSheetDialogFragment.TAG)
        } else {
            callback.invoke()
        }
    }

    private suspend fun checkUtxo(assetId: String, amount: String): Boolean {
        val consolidationAmount = linkViewModel.checkUtxoSufficiency(assetId, amount)
        if (consolidationAmount != null) {
            val asset = checkToken(assetId) ?: return false
            UtxoConsolidationBottomSheetDialogFragment.newInstance(buildTransferBiometricItem(Session.getAccount()!!.toUser(), asset, consolidationAmount, UUID.randomUUID().toString(), null, null))
                .show(bottomSheet.parentFragmentManager, UtxoConsolidationBottomSheetDialogFragment.TAG)
            return false
        }
        return true
    }

    private fun showPreconditionBottom(biometricItem: AssetBiometricItem) {
        val bottom = TransferBottomSheetDialogFragment.newInstance(biometricItem)
        bottom.show(bottomSheet.parentFragmentManager, TransferBottomSheetDialogFragment.TAG)
        bottomSheet.dismiss()
    }

    private suspend fun checkToken(assetId: String): TokenItem? {
        var asset = linkViewModel.findAssetItemById(assetId)
        if (asset == null) {
            asset = linkViewModel.refreshAsset(assetId)
        } else {
            linkViewModel.syncAsset(assetId)
        }
        if (asset != null && asset.assetId != asset.chainId && linkViewModel.findAssetItemById(asset.chainId) == null) {
            linkViewModel.refreshAsset(asset.chainId)
        }
        return linkViewModel.findAssetItemById(assetId)
    }

    private suspend fun checkTokenByCollectionHash(collectionHash: String, instantiationHash: String): TokenItem? {
        var asset = linkViewModel.findAssetItemByCollectionHash(collectionHash)
        if (asset == null) {
            asset = linkViewModel.refreshAssetByInscription(collectionHash, instantiationHash)
        }
        if (asset != null && asset.assetId != asset.chainId && linkViewModel.findAssetItemById(asset.chainId) == null) {
            linkViewModel.refreshAsset(asset.chainId)
        }
        return linkViewModel.findAssetItemByCollectionHash(collectionHash)
    }

    private suspend fun checkInscription(hash: String): InscriptionItem? {
        var inscription = linkViewModel.findInscriptionByHash(hash)
        if (inscription == null) {
            inscription = linkViewModel.refreshInscriptionItem(hash)
            return inscription
        } else {
            return inscription
        }
    }

    private suspend fun checkInscriptionCollection(hash: String): InscriptionCollection? {
        var inscriptionCollection = linkViewModel.findInscriptionCollectionByHash(hash)
        if (inscriptionCollection == null) {
            inscriptionCollection = linkViewModel.refreshInscriptionCollection(hash)
            return inscriptionCollection
        } else {
            return inscriptionCollection
        }
    }

    private suspend fun getPaymentStatus(traceId: String): String? {
        var isTraceNotFound = false
        val tx =
            handleMixinResponse(
                invokeNetwork = { linkViewModel.getTransactionsById(traceId) },
                successBlock = { r -> r.data },
                failureBlock = {
                    isTraceNotFound = it.errorCode == ErrorHandler.NOT_FOUND
                    return@handleMixinResponse isTraceNotFound
                },
            )
        return if (isTraceNotFound) {
            PaymentStatus.pending.name
        } else if (tx != null) {
            PaymentStatus.paid.name
        } else {
            null
        }
    }
}
