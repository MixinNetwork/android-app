package one.mixin.android.ui.conversation.link.parser
import androidx.core.net.toUri
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.TransferRequest
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
import one.mixin.android.ui.wallet.WalletActivity
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.SchemeBottomSheet

class NewSchemeParser(
    private val bottomSheet: SchemeBottomSheet,
    private val linkViewModel : BottomSheetViewModel
) {
    companion object {
        const val INSCRIPTION_NOT_FOUND = -2
        const val INSUFFICIENT_BALANCE = -1
        const val FAILURE = 0
        const val SUCCESS = 1
    }


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
                    token = checkAsset(asset!!) ?: return Result.failure(ParserError(FAILURE)) // TODO 404?
                    val tokensExtra = linkViewModel.findTokensExtra(asset)
                    if (urlQueryParser.inscription != null || urlQueryParser.inscriptionCollection != null) {
                        return Result.failure(ParserError(INSUFFICIENT_BALANCE, token.symbol))
                    } else if (tokensExtra == null) {
                        return Result.failure(BalanceError(AssetBiometricItem(token, traceId, amount ?: "0", urlQueryParser.memo, status, urlQueryParser.reference)))
                    } else if (BigDecimal(tokensExtra.balance ?: "0") < BigDecimal(amount)) {
                        return Result.failure(BalanceError(AssetBiometricItem(token, traceId, amount ?: "0", urlQueryParser.memo, status, urlQueryParser.reference)))
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
                    if ((response.data?.size ?: 0) == traces.size) {
                        return Result.failure(ParserError(FAILURE, message = bottomSheet.getString(R.string.pay_paid)))
                    }
                    val responseRequestIds = response.data?.map { it.requestId }
                    val isValid = traces.size > (responseRequestIds?.size ?: 0) &&
                        traces.subList(0, responseRequestIds?.size ?: 0) == responseRequestIds
                    if (!isValid) {
                        return Result.failure(ParserError(FAILURE))
                    }
                }
                var result: Result<Int>? = null
                coroutineScope {
                    val assetMap = invoice.groupByAssetId()
                    for ((assetId, amount) in assetMap) {
                        if (result != null) continue
                        
                        val token = checkAsset(assetId)
                        if (token == null) {
                            result = Result.failure(ParserError(FAILURE))
                            continue
                        }
                        
                        val tokensExtra = linkViewModel.findTokensExtra(assetId)
                        if (tokensExtra == null) {
                            result = Result.failure(BalanceError(AssetBiometricItem(token, traceId, amount, urlQueryParser.memo, PaymentStatus.pending.name, urlQueryParser.reference)))
                            continue
                        } else if (BigDecimal(tokensExtra.balance ?: "0") < BigDecimal(amount)) {
                            result = Result.failure(BalanceError(AssetBiometricItem(token, traceId, amount, urlQueryParser.memo, PaymentStatus.pending.name, urlQueryParser.reference)))
                            continue
                        }
                        
                        if (!checkUtxo(assetId, amount)) {
                            result = Result.success(SUCCESS)
                            continue
                        }
                    }
                }
                
                if (result != null) {
                    return result
                }
                
                val bottom = TransferInvoiceBottomSheetDialogFragment.newInstance(invoice.toString())
                bottom.show(bottomSheet.parentFragmentManager, TransferInvoiceBottomSheetDialogFragment.TAG)
                return Result.success(SUCCESS)
            } else {
                val token = asset?.let { checkAsset(it) ?: return Result.failure(ParserError(FAILURE)) }
                if (token == null) {
                    val bottom = AssetListBottomSheetDialogFragment.newInstance(TYPE_FROM_TRANSFER)
                        .apply {
                            asyncOnAsset = { selectedAsset ->
                                bottomSheet.requireContext().defaultSharedPreferences.putString(ASSET_PREFERENCE, selectedAsset.assetId)
                                val biometricItem = createBiometricItem(selectedAsset, payType, urlQueryParser, amount, traceId, from)
                                if (biometricItem != null) {
                                    WalletActivity.navigateToWalletActivity(this.requireActivity(), biometricItem)
                                }
                            }
                        }
                    bottom.show(bottomSheet.parentFragmentManager, AssetListBottomSheetDialogFragment.TAG)
                } else {
                    val biometricItem = createBiometricItem(token, payType, urlQueryParser, amount, traceId, from)
                    if (biometricItem == null) return Result.failure(ParserError(FAILURE))
                    WalletActivity.navigateToWalletActivity(bottomSheet.requireActivity(), biometricItem)
                }
            }
            return Result.success(SUCCESS)
        } catch (e: Exception) {
            Timber.e(e)
            return Result.failure(e)
        }
    }

    private suspend fun createBiometricItem(asset: TokenItem, payType:PayType, urlQueryParser: UrlQueryParser, amount: String? = null, traceId: String, from: Int): AssetBiometricItem? {
        return when (payType) {
            PayType.Uuid -> {
                val user = linkViewModel.refreshUser(urlQueryParser.userId) ?: return null
                buildTransferBiometricItem(user, asset, amount ?: "", traceId, urlQueryParser.memo, urlQueryParser.returnTo, reference = urlQueryParser.reference)
            }
            PayType.MixAddress -> {
                val mixAddress = urlQueryParser.mixAddress
                when {
                    mixAddress.uuidMembers.isNotEmpty() -> {
                        if (mixAddress.uuidMembers.size == 1) {
                            val user = linkViewModel.refreshUser(mixAddress.uuidMembers.first()) ?: return null
                            buildTransferBiometricItem(user, asset, amount ?: "", traceId, urlQueryParser.memo, urlQueryParser.returnTo, reference = urlQueryParser.reference)
                        } else {
                            val users = linkViewModel.findOrRefreshUsers(mixAddress.uuidMembers)
                            if (users.isEmpty() || users.size < mixAddress.uuidMembers.size) return null
                            TransferBiometricItem(users, mixAddress.threshold, traceId, asset, amount ?: "", urlQueryParser.memo, PaymentStatus.pending.name, null, urlQueryParser.returnTo, reference = urlQueryParser.reference)
                        }
                    }
                    mixAddress.xinMembers.size == 1 -> {
                        buildAddressBiometricItem(mixAddress.xinMembers.first().string(), traceId, asset, amount ?: "", urlQueryParser.memo, urlQueryParser.returnTo, from, reference = urlQueryParser.reference)
                    }
                    else -> null
                }
            }
            else -> buildAddressBiometricItem(urlQueryParser.lastPath, traceId, asset, amount ?: "", urlQueryParser.memo, urlQueryParser.returnTo, from, reference = urlQueryParser.reference)
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
            checkAsset(assetId)
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
        var insufficientId: String? = null
        val result =
            parseExternalTransferUri(url, { assetId, chainId, destination ->
                handleMixinResponse(
                    invokeNetwork = {
                        linkViewModel.validateExternalAddress(assetId, chainId, destination, null)
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
                        insufficientId = feeAssetId
                    } else if (BigDecimal(tokensExtra.balance ?: "0") < feeAmount) {
                        errorMsg = bottomSheet.getString(R.string.insufficient_balance)
                        insufficientId = feeAssetId
                    }
                }

                val tokensExtra = linkViewModel.findTokensExtra(assetId)
                if (tokensExtra == null) {
                    errorMsg = bottomSheet.getString(R.string.insufficient_balance)
                    insufficientId = assetId
                } else if (BigDecimal(tokensExtra.balance ?: "0") < amount) {
                    errorMsg = bottomSheet.getString(R.string.insufficient_balance)
                    insufficientId = assetId
                }

            }, { url ->
                linkViewModel.paySuspend(
                    TransferRequest(
                        assetId = Constants.ChainId.LIGHTNING_NETWORK_CHAIN_ID, rawPaymentUrl = url
                    )
                ).data
            })

        if (insufficientId == null) {
            errorMsg?.let {
                bottomSheet.showError(it)
                return
            }
        }

        if (result == null) {
            QrScanBottomSheetDialogFragment.newInstance(url)
                .show(bottomSheet.parentFragmentManager, QrScanBottomSheetDialogFragment.TAG)
        } else {
            val asset = checkAsset(result.assetId)
            if (asset == null) {
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
            if (amount == null) {
                // This only exists when processing External URL within the App
                bottomSheet.navTo(InputFragment.newInstance(asset, destination, null), InputFragment.TAG)
                bottomSheet.dismiss()
                return
            }

            val feeAsset = checkAsset(result.feeAssetId!!)
            if (feeAsset == null) {
                bottomSheet.showError(R.string.Asset_not_found)
                bottomSheet.dismiss()
                return
            }

            val address = Address("", "address", asset.assetId, asset.chainId, destination, "ExternalAddress", nowInUtc(), null, null)
            val fee = NetworkFee(feeAsset, result.fee!!.toPlainString())

            val withdrawBiometricItem = WithdrawBiometricItem(address, fee, null, traceId, asset, amount, result.memo, status, null)
            if (insufficientId != null) {
                throw BalanceError(withdrawBiometricItem, errorMsg ?: "")
            }
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
            val asset = checkAsset(assetId) ?: return false
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

    private suspend fun checkAsset(assetId: String): TokenItem? {
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
