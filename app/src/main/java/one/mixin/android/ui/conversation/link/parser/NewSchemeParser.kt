package one.mixin.android.ui.conversation.link.parser
import androidx.core.net.toUri
import kotlinx.coroutines.coroutineScope
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.isUUID
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.putString
import one.mixin.android.pay.parseExternalTransferUri
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BottomSheetViewModel
import one.mixin.android.ui.common.QrScanBottomSheetDialogFragment
import one.mixin.android.ui.common.SchemeBottomSheet
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
import one.mixin.android.ui.wallet.NetworkFee
import one.mixin.android.ui.wallet.TokenListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TokenListBottomSheetDialogFragment.Companion.ASSET_PREFERENCE
import one.mixin.android.ui.wallet.TokenListBottomSheetDialogFragment.Companion.TYPE_FROM_TRANSFER
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.ui.wallet.transfer.TransferBottomSheetDialogFragment
import one.mixin.android.ui.wallet.transfer.TransferInvoiceBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.getMixinErrorStringByCode
import one.mixin.android.vo.Address
import one.mixin.android.vo.InscriptionCollection
import one.mixin.android.vo.InscriptionItem
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.vo.toUser
import timber.log.Timber
import java.math.BigDecimal
import java.util.UUID

class NewSchemeParser(
    private val bottomSheet: SchemeBottomSheet,
    private val linkViewModel : BottomSheetViewModel
) {
    companion object {
        private const val TAG = "NewSchemeParser"
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
            Timber.d("$TAG parse start text=$text from=$from")
            val urlQueryParser = UrlQueryParser(text.toUri(), from)

            bottomSheet.syncUtxo()
            val payType = urlQueryParser.payType
            val asset = urlQueryParser.asset
            val amount = urlQueryParser.amount
            val traceId = urlQueryParser.trace ?: UUID.randomUUID().toString()
            Timber.d("$TAG parse resolved ${describeUrlQuery(urlQueryParser, payType, asset, amount, traceId)}")
            if ((asset != null && amount != null) || urlQueryParser.inscription != null || urlQueryParser.inscriptionCollection != null) {
                Timber.d("$TAG parse entering direct payment branch payType=$payType asset=$asset amount=$amount traceId=$traceId inscription=${urlQueryParser.inscription} inscriptionCollection=${urlQueryParser.inscriptionCollection}")
                val status = getPaymentStatus(traceId) ?: run {
                    Timber.w("$TAG payment status unavailable traceId=$traceId")
                    return Result.failure(ParserError(FAILURE))
                }
                if (status == PaymentStatus.paid.name) {
                    Timber.w("$TAG payment already paid traceId=$traceId")
                    return Result.failure(ParserError(FAILURE, message = bottomSheet.getString(R.string.pay_paid)))
                }
                val token: TokenItem?
                if (urlQueryParser.inscription == null && urlQueryParser.inscriptionCollection == null) {
                    token = checkAsset(asset!!) ?: run {
                        Timber.w("$TAG asset check failed assetId=$asset traceId=$traceId")
                        return Result.failure(ParserError(FAILURE))
                    } // TODO 404?
                    val tokensExtra = linkViewModel.findTokensExtra(asset)
                    Timber.d("$TAG balance check assetId=$asset token=${token.symbol} balance=${tokensExtra?.balance} amount=$amount traceId=$traceId")
                    if (urlQueryParser.inscription != null || urlQueryParser.inscriptionCollection != null) {
                        Timber.w("$TAG inscription branch reported insufficient balance token=${token.symbol} traceId=$traceId")
                        return Result.failure(ParserError(INSUFFICIENT_BALANCE, token.symbol))
                    } else if (tokensExtra == null) {
                        Timber.w("$TAG balance missing for assetId=$asset traceId=$traceId")
                        return Result.failure(BalanceError(AssetBiometricItem(token, traceId, amount ?: "0", urlQueryParser.memo, status, urlQueryParser.reference)))
                    } else if (BigDecimal(tokensExtra.balance ?: "0") < BigDecimal(amount)) {
                        Timber.w("$TAG insufficient balance assetId=$asset balance=${tokensExtra.balance} amount=$amount traceId=$traceId")
                        return Result.failure(BalanceError(AssetBiometricItem(token, traceId, amount ?: "0", urlQueryParser.memo, status, urlQueryParser.reference)))
                    }
                } else {
                    token = null
                    Timber.d("$TAG parse using inscription flow without fungible token traceId=$traceId")
                }

                if (payType == PayType.Uuid) {
                    val user = linkViewModel.refreshUser(urlQueryParser.userId) ?: run {
                        Timber.w("$TAG refresh user failed userId=${urlQueryParser.userId} traceId=$traceId")
                        return Result.failure(ParserError(FAILURE))
                    }
                    Timber.d("$TAG uuid payment target userId=${user.userId} traceId=$traceId")
                    if (urlQueryParser.inscriptionCollection != null) {
                        checkInscriptionCollection(urlQueryParser.inscriptionCollection!!) ?: run {
                            Timber.w("$TAG inscription collection not found hash=${urlQueryParser.inscriptionCollection} traceId=$traceId")
                            return Result.failure(ParserError(INSCRIPTION_NOT_FOUND, message = bottomSheet.getString(R.string.collectible_not_found)))
                        }
                        Timber.d("$TAG showing inscription collection bottom hash=${urlQueryParser.inscriptionCollection} userId=${urlQueryParser.userId} traceId=$traceId")
                        CollectionBottomSheetDialogFragment.newInstance(urlQueryParser.inscriptionCollection!!, traceId, urlQueryParser.userId, urlQueryParser.memo).showNow(bottomSheet.parentFragmentManager, CollectionBottomSheetDialogFragment.TAG)
                        return Result.success(SUCCESS)
                    }
                    val biometricItem =
                        if (urlQueryParser.inscription != null) {
                            buildInscriptionTransfer(urlQueryParser, user.userId, traceId).also {
                                Timber.d("$TAG built inscription biometric item ${describeBiometricItem(it)}")
                            }
                        } else {
                            TransferBiometricItem(listOf(user), 1, traceId, token, requireNotNull(amount), urlQueryParser.memo, status, null, urlQueryParser.returnTo, reference = urlQueryParser.reference).also {
                                Timber.d("$TAG built uuid transfer biometric item ${describeBiometricItem(it)}")
                            }
                        }
                    checkRawTransaction(biometricItem)
                } else if (payType == PayType.MixAddress) {
                    val mixAddress = urlQueryParser.mixAddress
                    Timber.d("$TAG mix address payment threshold=${mixAddress.threshold} uuidMembers=${mixAddress.uuidMembers.size} xinMembers=${mixAddress.xinMembers.size} traceId=$traceId")
                    if (mixAddress.uuidMembers.isNotEmpty()) {
                        val users = linkViewModel.findOrRefreshUsers(mixAddress.uuidMembers)
                        if (users.isEmpty() || users.size < mixAddress.uuidMembers.size) {
                            Timber.w("$TAG mix address user refresh incomplete expected=${mixAddress.uuidMembers.size} actual=${users.size} traceId=$traceId")
                            return Result.failure(ParserError(FAILURE))
                        }
                        val biometricItem = TransferBiometricItem(users, mixAddress.threshold, traceId, token, requireNotNull(amount), urlQueryParser.memo, status, null, urlQueryParser.returnTo, reference = urlQueryParser.reference)
                        Timber.d("$TAG built mix-address transfer biometric item ${describeBiometricItem(biometricItem)}")
                        checkRawTransaction(biometricItem)
                    } else if (mixAddress.xinMembers.isNotEmpty()) {
                        val addressTransferBiometricItem = AddressTransferBiometricItem(mixAddress.xinMembers.first().string(), mixAddress.threshold.toInt(), traceId, token, requireNotNull(amount), urlQueryParser.memo, status, urlQueryParser.returnTo, reference = urlQueryParser.reference)
                        Timber.d("$TAG built xin-address biometric item ${describeBiometricItem(addressTransferBiometricItem)}")
                        checkRawTransaction(addressTransferBiometricItem)
                    } else {
                        Timber.w("$TAG mix address had no supported members traceId=$traceId")
                        return Result.failure(ParserError(FAILURE))
                    }
                } else {
                    // TODO verify address?
                    val addressTransferBiometricItem = AddressTransferBiometricItem(urlQueryParser.lastPath, 1, traceId, token, requireNotNull(amount), urlQueryParser.memo, status, urlQueryParser.returnTo, reference = urlQueryParser.reference)
                    Timber.d("$TAG built direct address biometric item ${describeBiometricItem(addressTransferBiometricItem)}")
                    checkRawTransaction(addressTransferBiometricItem)
                }
            } else if (payType  == PayType.Invoice) {
                val invoice = urlQueryParser.mixInvoice
                Timber.d("$TAG parse entering invoice branch entries=${invoice.entries.size} traceId=$traceId")
                invoice.recipient.members().filter {
                    it.isUUID()
                }.forEach { userId ->
                    linkViewModel.refreshUser(userId) ?: run {
                        Timber.w("$TAG invoice recipient refresh failed userId=$userId traceId=$traceId")
                        return Result.failure(ParserError(FAILURE))
                    }
                }
                val rawTransaction = linkViewModel.firstSignedTransaction()
                if (rawTransaction != null) {
                    Timber.d("$TAG invoice found pending raw transaction traceId=$traceId")
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

                        Timber.d("$TAG invoice checking assetId=$assetId amount=$amount traceId=$traceId")
                        val token = checkAsset(assetId)
                        if (token == null) {
                            result = Result.failure(ParserError(FAILURE))
                            Timber.w("$TAG invoice asset missing assetId=$assetId traceId=$traceId")
                            continue
                        }

                        val tokensExtra = linkViewModel.findTokensExtra(assetId)
                        if (tokensExtra == null) {
                            result = Result.failure(BalanceError(AssetBiometricItem(token, traceId, amount, urlQueryParser.memo, PaymentStatus.pending.name, urlQueryParser.reference)))
                            Timber.w("$TAG invoice balance missing assetId=$assetId traceId=$traceId")
                            continue
                        } else if (BigDecimal(tokensExtra.balance ?: "0") < BigDecimal(amount)) {
                            result = Result.failure(BalanceError(AssetBiometricItem(token, traceId, amount, urlQueryParser.memo, PaymentStatus.pending.name, urlQueryParser.reference)))
                            Timber.w("$TAG invoice insufficient balance assetId=$assetId balance=${tokensExtra.balance} amount=$amount traceId=$traceId")
                            continue
                        }

                        if (!checkUtxo(assetId, amount)) {
                            result = Result.success(SUCCESS)
                            Timber.d("$TAG invoice triggered utxo consolidation assetId=$assetId amount=$amount traceId=$traceId")
                            continue
                        }
                    }
                }

                if (result != null) {
                    return result
                }

                val bottom = TransferInvoiceBottomSheetDialogFragment.newInstance(invoice.toString())
                Timber.d("$TAG showing invoice transfer bottom traceId=$traceId")
                bottom.show(bottomSheet.parentFragmentManager, TransferInvoiceBottomSheetDialogFragment.TAG)
                return Result.success(SUCCESS)
            } else {
                val token = asset?.let { assetId ->
                    checkAsset(assetId) ?: run {
                        Timber.w("$TAG optional asset check failed assetId=$assetId traceId=$traceId")
                        return Result.failure(ParserError(FAILURE))
                    }
                }
                Timber.d("$TAG parse entering asset-selection-or-wallet branch payType=$payType asset=$asset resolvedToken=${token?.assetId} amount=$amount traceId=$traceId")
                if (token == null) {
                    val bottom = TokenListBottomSheetDialogFragment.newInstance(TYPE_FROM_TRANSFER)
                        .apply {
                            asyncOnAsset = { selectedAsset ->
                                try {
                                    this@apply.requireContext().defaultSharedPreferences.putString(ASSET_PREFERENCE, selectedAsset.assetId)
                                    val biometricItem = createBiometricItem(selectedAsset, payType, urlQueryParser, amount, traceId, from)
                                    if (biometricItem != null) {
                                        val hostActivity = this@apply.activity ?: bottomSheet.activity
                                        if (hostActivity != null) {
                                            WalletActivity.navigateToWalletActivity(hostActivity, biometricItem)
                                        }
                                    }
                                } catch (_: Exception) {
                                }
                            }
                        }
                    Timber.d("$TAG showing token picker for pay link payType=$payType amount=$amount traceId=$traceId")
                    try {
                        bottom.show(bottomSheet.parentFragmentManager, TokenListBottomSheetDialogFragment.TAG)
                    } catch (_: Exception) {
                        return Result.failure(ParserError(FAILURE))
                    }
                } else {
                    val biometricItem = createBiometricItem(token, payType, urlQueryParser, amount, traceId, from)
                    if (biometricItem == null) {
                        return Result.failure(ParserError(FAILURE))
                    }
                    val hostActivity = bottomSheet.activity
                    if (hostActivity == null) {
                        return Result.failure(ParserError(FAILURE))
                    }
                    WalletActivity.navigateToWalletActivity(hostActivity, biometricItem)
                }
            }
            Timber.d("$TAG parse finished successfully traceId=$traceId payType=$payType")
            return Result.success(SUCCESS)
        } catch (e: Exception) {
            Timber.e(e, "$TAG parse failed text=$text from=$from")
            return Result.failure(e)
        }
    }

    private suspend fun createBiometricItem(asset: TokenItem, payType:PayType, urlQueryParser: UrlQueryParser, amount: String? = null, traceId: String, from: Int): AssetBiometricItem? {
        Timber.d("$TAG createBiometricItem start assetId=${asset.assetId} symbol=${asset.symbol} payType=$payType amount=$amount traceId=$traceId from=$from lastPath=${urlQueryParser.lastPath}")
        return when (payType) {
            PayType.Uuid -> {
                val user = linkViewModel.refreshUser(urlQueryParser.userId) ?: run {
                    Timber.w("$TAG createBiometricItem failed to refresh uuid userId=${urlQueryParser.userId} traceId=$traceId")
                    return null
                }
                buildTransferBiometricItem(user, asset, amount ?: "", traceId, urlQueryParser.memo, urlQueryParser.returnTo, reference = urlQueryParser.reference).also {
                    Timber.d("$TAG createBiometricItem built uuid item ${describeBiometricItem(it)}")
                }
            }
            PayType.MixAddress -> {
                val mixAddress = urlQueryParser.mixAddress
                when {
                    mixAddress.uuidMembers.isNotEmpty() -> {
                        if (mixAddress.uuidMembers.size == 1) {
                            val user = linkViewModel.refreshUser(mixAddress.uuidMembers.first()) ?: run {
                                Timber.w("$TAG createBiometricItem failed to refresh single mix userId=${mixAddress.uuidMembers.first()} traceId=$traceId")
                                return null
                            }
                            buildTransferBiometricItem(user, asset, amount ?: "", traceId, urlQueryParser.memo, urlQueryParser.returnTo, reference = urlQueryParser.reference).also {
                                Timber.d("$TAG createBiometricItem built single mix-address item ${describeBiometricItem(it)}")
                            }
                        } else {
                            val users = linkViewModel.findOrRefreshUsers(mixAddress.uuidMembers)
                            if (users.isEmpty() || users.size < mixAddress.uuidMembers.size) {
                                Timber.w("$TAG createBiometricItem failed to refresh mix users expected=${mixAddress.uuidMembers.size} actual=${users.size} traceId=$traceId")
                                return null
                            }
                            TransferBiometricItem(users, mixAddress.threshold, traceId, asset, amount ?: "", urlQueryParser.memo, PaymentStatus.pending.name, null, urlQueryParser.returnTo, reference = urlQueryParser.reference).also {
                                Timber.d("$TAG createBiometricItem built multi-user mix item ${describeBiometricItem(it)}")
                            }
                        }
                    }
                    mixAddress.xinMembers.size == 1 -> {
                        buildAddressBiometricItem(mixAddress.xinMembers.first().string(), traceId, asset, amount ?: "", urlQueryParser.memo, mixAddress.threshold.toInt(), urlQueryParser.returnTo, reference = urlQueryParser.reference).also {
                            Timber.d("$TAG createBiometricItem built xin member item ${describeBiometricItem(it)}")
                        }
                    }
                    else -> {
                        Timber.w("$TAG createBiometricItem unsupported mix address members traceId=$traceId")
                        null
                    }
                }
            }
            else -> buildAddressBiometricItem(urlQueryParser.lastPath, traceId, asset, amount ?: "", urlQueryParser.memo, 1, urlQueryParser.returnTo, reference = urlQueryParser.reference).also {
                Timber.d("$TAG createBiometricItem built direct address item ${describeBiometricItem(it)}")
            }

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
                val response = linkViewModel.paySuspend(
                    TransferRequest(
                        assetId = Constants.ChainId.LIGHTNING_NETWORK_CHAIN_ID, rawPaymentUrl = url
                    )
                )
                response.error?.let {
                    errorMsg = MixinApplication.appContext.getMixinErrorStringByCode(it.code, it.description)
                }
                response.data
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
                bottomSheet.requireActivity().let {
                    WalletActivity.showInputForAddress(it, asset, destination, null)
                }
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
        val rawTransaction = linkViewModel.firstSignedTransaction()
        if (rawTransaction != null) {
            Timber.d("$TAG checkRawTransaction found pending raw transaction ${describeBiometricItem(biometricItem)}")
            WaitingBottomSheetDialogFragment.newInstance().showNow(bottomSheet.parentFragmentManager, WaitingBottomSheetDialogFragment.TAG)
        } else {
            if (biometricItem is TransferBiometricItem && biometricItem.users.size == 1) {
                val pair = linkViewModel.findLatestTrace(biometricItem.users.first().userId, null, null, biometricItem.amount, biometricItem.asset?.assetId ?: "")
                biometricItem.trace = pair.first
                Timber.d("$TAG checkRawTransaction updated latest trace trace=${biometricItem.trace} ${describeBiometricItem(biometricItem)}")
            }
            checkUtxo(biometricItem) {
                Timber.d("$TAG checkRawTransaction passed utxo check ${describeBiometricItem(biometricItem)}")
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
            Timber.d("$TAG checkUtxo skipped because amount blank ${describeBiometricItem(t)}")
            callback.invoke()
        }
        Timber.d("$TAG checkUtxo start assetId=${token.assetId} amount=$amount traceId=${t.traceId}")
        val consolidationAmount = linkViewModel.checkUtxoSufficiency(token.assetId, amount)
        if (consolidationAmount != null) {
            Timber.d("$TAG checkUtxo requires consolidation assetId=${token.assetId} amount=$amount consolidationAmount=$consolidationAmount traceId=${t.traceId}")
            UtxoConsolidationBottomSheetDialogFragment.newInstance(buildTransferBiometricItem(Session.getAccount()!!.toUser(), t.asset, consolidationAmount, UUID.randomUUID().toString(), null, null))
                .show(bottomSheet.parentFragmentManager, UtxoConsolidationBottomSheetDialogFragment.TAG)
        } else {
            Timber.d("$TAG checkUtxo passed assetId=${token.assetId} amount=$amount traceId=${t.traceId}")
            callback.invoke()
        }
    }

    private suspend fun checkUtxo(assetId: String, amount: String): Boolean {
        Timber.d("$TAG checkUtxo(assetId) start assetId=$assetId amount=$amount")
        val consolidationAmount = linkViewModel.checkUtxoSufficiency(assetId, amount)
        if (consolidationAmount != null) {
            Timber.d("$TAG checkUtxo(assetId) requires consolidation assetId=$assetId amount=$amount consolidationAmount=$consolidationAmount")
            val asset = checkAsset(assetId) ?: return false
            UtxoConsolidationBottomSheetDialogFragment.newInstance(buildTransferBiometricItem(Session.getAccount()!!.toUser(), asset, consolidationAmount, UUID.randomUUID().toString(), null, null))
                .show(bottomSheet.parentFragmentManager, UtxoConsolidationBottomSheetDialogFragment.TAG)
            return false
        }
        Timber.d("$TAG checkUtxo(assetId) passed assetId=$assetId amount=$amount")
        return true
    }

    private fun showPreconditionBottom(biometricItem: AssetBiometricItem) {
        Timber.d("$TAG showPreconditionBottom ${describeBiometricItem(biometricItem)}")
        val bottom = TransferBottomSheetDialogFragment.newInstance(biometricItem)
        bottom.show(bottomSheet.parentFragmentManager, TransferBottomSheetDialogFragment.TAG)
        bottomSheet.dismiss()
    }

    private suspend fun checkAsset(assetId: String): TokenItem? {
        Timber.d("$TAG checkAsset start assetId=$assetId")
        var asset = linkViewModel.findAssetItemById(assetId)
        if (asset == null) {
            Timber.d("$TAG checkAsset cache miss assetId=$assetId, refreshing")
            asset = linkViewModel.refreshAsset(assetId)
        } else {
            Timber.d("$TAG checkAsset cache hit assetId=$assetId chainId=${asset.chainId}, syncing")
            linkViewModel.syncAsset(assetId)
        }
        if (asset != null && asset.assetId != asset.chainId && linkViewModel.findAssetItemById(asset.chainId) == null) {
            Timber.d("$TAG checkAsset refreshing chain asset chainId=${asset.chainId} for assetId=$assetId")
            linkViewModel.refreshAsset(asset.chainId)
        }
        return linkViewModel.findAssetItemById(assetId).also {
            Timber.d("$TAG checkAsset result assetId=$assetId found=${it != null} symbol=${it?.symbol}")
        }
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
        Timber.d("$TAG getPaymentStatus start traceId=$traceId")
        var isTraceNotFound = false
        val tx =
            handleMixinResponse(
                invokeNetwork = { linkViewModel.getTransactionsById(traceId) },
                successBlock = { r -> r.data },
                failureBlock = {
                    isTraceNotFound = it.errorCode == ErrorHandler.NOT_FOUND
                    Timber.w("$TAG getPaymentStatus failed traceId=$traceId errorCode=${it.errorCode} traceNotFound=$isTraceNotFound")
                    return@handleMixinResponse isTraceNotFound
                },
            )
        return if (isTraceNotFound) {
            Timber.d("$TAG getPaymentStatus trace not found, treating as pending traceId=$traceId")
            PaymentStatus.pending.name
        } else if (tx != null) {
            Timber.d("$TAG getPaymentStatus found existing transaction traceId=$traceId")
            PaymentStatus.paid.name
        } else {
            Timber.w("$TAG getPaymentStatus returned null traceId=$traceId")
            null
        }
    }

    private fun describeUrlQuery(
        urlQueryParser: UrlQueryParser,
        payType: PayType,
        asset: String?,
        amount: String?,
        traceId: String,
    ): String {
        return "payType=$payType lastPath=${urlQueryParser.lastPath} asset=$asset amount=$amount traceId=$traceId memoPresent=${!urlQueryParser.memo.isNullOrBlank()} referencePresent=${urlQueryParser.reference != null} returnToPresent=${urlQueryParser.returnTo != null} inscription=${urlQueryParser.inscription} inscriptionCollection=${urlQueryParser.inscriptionCollection}"
    }

    private fun describeBiometricItem(item: AssetBiometricItem): String {
        return "type=${item.javaClass.simpleName} traceId=${item.traceId} assetId=${item.asset?.assetId} amount=${item.amount} memoPresent=${!item.memo.isNullOrBlank()} reference=${item.reference}"
    }
}
