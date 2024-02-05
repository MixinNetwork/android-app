package one.mixin.android.repository

import com.google.gson.Gson
import io.reactivex.Observable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.PIN_ERROR_MAX
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AccountRequest
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.api.request.AuthorizeRequest
import one.mixin.android.api.request.CollectibleRequest
import one.mixin.android.api.request.DeactivateRequest
import one.mixin.android.api.request.DeactivateVerificationRequest
import one.mixin.android.api.request.DeauthorRequest
import one.mixin.android.api.request.EmergencyRequest
import one.mixin.android.api.request.LogoutRequest
import one.mixin.android.api.request.PinRequest
import one.mixin.android.api.request.RawTransactionsRequest
import one.mixin.android.api.request.SessionRequest
import one.mixin.android.api.request.StickerAddRequest
import one.mixin.android.api.request.VerificationRequest
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.api.response.ConversationResponse
import one.mixin.android.api.response.MultisigsResponse
import one.mixin.android.api.response.NonFungibleOutputResponse
import one.mixin.android.api.response.PaymentCodeResponse
import one.mixin.android.api.response.VerificationResponse
import one.mixin.android.api.service.AccountService
import one.mixin.android.api.service.AuthorizationService
import one.mixin.android.api.service.ConversationService
import one.mixin.android.api.service.EmergencyService
import one.mixin.android.api.service.GiphyService
import one.mixin.android.api.service.UserService
import one.mixin.android.crypto.PinCipher
import one.mixin.android.db.AppDao
import one.mixin.android.db.FavoriteAppDao
import one.mixin.android.db.StickerAlbumDao
import one.mixin.android.db.StickerDao
import one.mixin.android.db.StickerRelationshipDao
import one.mixin.android.db.UserDao
import one.mixin.android.db.insertUpdate
import one.mixin.android.db.insertUpdateList
import one.mixin.android.db.withTransaction
import one.mixin.android.extension.nowInUtcNano
import one.mixin.android.extension.within24Hours
import one.mixin.android.session.Session
import one.mixin.android.tip.TipBody
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.Account
import one.mixin.android.vo.FavoriteApp
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.StickerAlbum
import one.mixin.android.vo.StickerAlbumAdded
import one.mixin.android.vo.StickerAlbumOrder
import one.mixin.android.vo.StickerRelationship
import one.mixin.android.vo.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository
    @Inject
    constructor(
        private val accountService: AccountService,
        private val userService: UserService,
        private val conversationService: ConversationService,
        private val userDao: UserDao,
        private val appDao: AppDao,
        private val favoriteAppDao: FavoriteAppDao,
        private val authService: AuthorizationService,
        private val stickerDao: StickerDao,
        private val stickerAlbumDao: StickerAlbumDao,
        private val stickerRelationshipDao: StickerRelationshipDao,
        private val giphyService: GiphyService,
        private val emergencyService: EmergencyService,
        private val pinCipher: PinCipher,
    ) {
        fun verificationObserver(request: VerificationRequest): Observable<MixinResponse<VerificationResponse>> =
            accountService.verificationObserver(request)

        suspend fun verification(request: VerificationRequest): MixinResponse<VerificationResponse> =
            accountService.verification(request)

        suspend fun create(
            id: String,
            request: AccountRequest,
        ): MixinResponse<Account> =
            accountService.create(id, request)

        suspend fun changePhone(
            id: String,
            request: AccountRequest,
        ): MixinResponse<Account> =
            accountService.changePhone(id, request)

        fun deactiveVerification(
            id: String,
            request: DeactivateVerificationRequest,
        ): Observable<MixinResponse<VerificationResponse>> =
            accountService.deactiveVerification(id, request)

        fun update(request: AccountUpdateRequest): Observable<MixinResponse<Account>> =
            accountService.update(request)

        fun updateSession(request: SessionRequest) = accountService.updateSession(request)

        fun deviceCheck() = accountService.deviceCheck()

        fun join(conversationId: String): Observable<MixinResponse<ConversationResponse>> {
            return conversationService.join(conversationId)
        }

        suspend fun getScheme(id: String) = accountService.getScheme(id)

        suspend fun searchCode(code: String): Pair<String, Any> {
            val response = accountService.code(code)
            if (!response.isSuccess) {
                ErrorHandler.handleMixinError(response.errorCode, response.errorDescription)
                return Pair("", "")
            }
            val result: Pair<String, Any>
            val type = response.data?.get("type")?.asString
            result =
                when (type) {
                    QrCodeType.user.name -> {
                        val user = Gson().fromJson(response.data, User::class.java)
                        userDao.insertUpdate(user, appDao)
                        Pair(type, user)
                    }
                    QrCodeType.conversation.name -> {
                        val conversationResponse =
                            Gson().fromJson(response.data, ConversationResponse::class.java)
                        Pair(type, conversationResponse)
                    }
                    QrCodeType.authorization.name -> {
                        val resp = Gson().fromJson(response.data, AuthorizationResponse::class.java)
                        Pair(type, resp)
                    }
                    QrCodeType.multisig_request.name -> {
                        val resp = Gson().fromJson(response.data, MultisigsResponse::class.java)
                        Pair(type, resp)
                    }
                    QrCodeType.non_fungible_request.name -> {
                        val resp = Gson().fromJson(response.data, NonFungibleOutputResponse::class.java)
                        Pair(type, resp)
                    }
                    QrCodeType.payment.name -> {
                        val resp = Gson().fromJson(response.data, PaymentCodeResponse::class.java)
                        Pair(type, resp)
                    }
                    else -> Pair("", "")
                }
            return result
        }

        fun search(query: String): Observable<MixinResponse<User>> = userService.search(query)

        suspend fun logout(sessionId: String) = accountService.logout(LogoutRequest(sessionId))

        fun findUsersByType(relationship: String) = userDao.findUsersByType(relationship)

        suspend fun updatePinSuspend(request: PinRequest) = accountService.updatePinSuspend(request)

        suspend fun verifyPin(code: String): MixinResponse<Account> =
            withContext(Dispatchers.IO) {
                val timestamp = nowInUtcNano()
                return@withContext if (Session.getTipPub().isNullOrBlank()) {
                    accountService.verifyPin(PinRequest(pinCipher.encryptPin(code, TipBody.forVerify(timestamp))))
                } else {
                    accountService.verifyPin(PinRequest(pinCipher.encryptPin(code, TipBody.forVerify(timestamp)), timestamp = timestamp))
                }
            }

        suspend fun deactivate(
            pin: String,
            verificationId: String,
        ): MixinResponse<Account> =
            withContext(Dispatchers.IO) {
                accountService.deactivate(
                    DeactivateRequest(
                        pinCipher.encryptPin(pin, TipBody.forUserDeactivate(verificationId)),
                        verificationId,
                    ),
                )
            }

        suspend fun authorize(
            authorizationId: String,
            scopes: List<String>,
            pin: String?,
        ): MixinResponse<AuthorizationResponse> =
            withContext(Dispatchers.IO) {
                authService.authorize(
                    AuthorizeRequest(
                        authorizationId,
                        scopes,
                        if (pin != null) {
                            pinCipher.encryptPin(pin, TipBody.forOAuthApprove(authorizationId))
                        } else {
                            null
                        },
                    ),
                )
            }

        fun deAuthorize(deauthorRequest: DeauthorRequest) = authService.deAuthorize(deauthorRequest)

        suspend fun getAuthorizationByAppId(appId: String) = authService.getAuthorizationByAppId(appId)

        fun observeSystemAddedAlbums() = stickerAlbumDao.observeSystemAddedAlbums()

        fun observeSystemAlbums() = stickerAlbumDao.observeSystemAlbums()

        fun observeSystemAlbumsAndStickers() = stickerAlbumDao.observeSystemAlbumsAndStickers()

        suspend fun getPersonalAlbums() = stickerAlbumDao.getPersonalAlbums()

        suspend fun findPersonalAlbumId() = stickerAlbumDao.findPersonalAlbumId()

        fun observeStickers(id: String) = stickerRelationshipDao.observeStickersByAlbumId(id)

        fun observeSystemStickersByAlbumId(id: String) = stickerRelationshipDao.observeSystemStickersByAlbumId(id)

        suspend fun findStickersByAlbumId(albumId: String) = stickerRelationshipDao.findStickersByAlbumId(albumId)

        suspend fun findStickerById(stickerId: String) = stickerDao.findStickerById(stickerId)

        fun observeStickerById(stickerId: String) = stickerDao.observeStickerById(stickerId)

        suspend fun findAlbumById(albumId: String) = stickerAlbumDao.findAlbumById(albumId)

        suspend fun findStickerSystemAlbumId(stickerId: String) = stickerRelationshipDao.findStickerSystemAlbumId(stickerId)

        fun observeAlbumById(albumId: String) = stickerAlbumDao.observeAlbumById(albumId)

        fun observeSystemAlbumById(albumId: String) = stickerAlbumDao.observeSystemAlbumById(albumId)

        suspend fun updateAlbumOrders(stickerAlbumOrders: List<StickerAlbumOrder>) {
            withTransaction {
                stickerAlbumOrders.forEach { o -> stickerAlbumDao.updateOrderedAt(o) }
            }
        }

        suspend fun updateAlbumAdded(stickerAlbumAdded: StickerAlbumAdded) = stickerAlbumDao.updateAdded(stickerAlbumAdded)

        suspend fun findMaxOrder() = stickerAlbumDao.findMaxOrder() ?: 0

        fun observePersonalStickers() = stickerRelationshipDao.observePersonalStickers()

        fun recentUsedStickers() = stickerDao.recentUsedStickers()

        suspend fun updateUsedAt(
            stickerId: String,
            at: String,
        ) = stickerDao.updateUsedAt(stickerId, at)

        fun addStickerAsync(request: StickerAddRequest) = accountService.addStickerAsync(request)

        suspend fun getStickersByAlbumIdSuspend(albumId: String) = accountService.getStickersByAlbumIdSuspend(albumId)

        suspend fun insertAlbumSuspend(album: StickerAlbum) = stickerAlbumDao.insertSuspend(album)

        suspend fun getAlbumByIdSuspend(albumId: String) = accountService.getAlbumByIdSuspend(albumId)

        fun addStickerWithoutRelationship(sticker: Sticker) {
            stickerDao.insertUpdate(sticker)
        }

        fun addRelationships(relationships: List<StickerRelationship>) {
            stickerRelationshipDao.insertList(relationships)
        }

        fun addStickerLocal(
            sticker: Sticker,
            albumId: String,
        ) {
            stickerDao.insertUpdate(sticker)
            stickerRelationshipDao.insert(StickerRelationship(albumId, sticker.stickerId))
        }

        fun trendingGifs(
            limit: Int,
            offset: Int,
        ) = giphyService.trendingGifs(limit, offset)

        fun searchGifs(
            query: String,
            limit: Int,
            offset: Int,
        ) =
            giphyService.searchGifs(query, limit, offset)

        suspend fun createEmergency(request: EmergencyRequest) = emergencyService.create(request)

        suspend fun createVerifyEmergency(
            id: String,
            request: EmergencyRequest,
        ) =
            emergencyService.createVerify(id, request)

        suspend fun loginVerifyEmergency(
            id: String,
            request: EmergencyRequest,
        ) =
            emergencyService.loginVerify(id, request)

        suspend fun showEmergency(pin: String) =
            emergencyService.show(PinRequest(pinCipher.encryptPin(pin, TipBody.forEmergencyContactRead())))

        suspend fun deleteEmergency(pin: String) = emergencyService.delete(PinRequest(pinCipher.encryptPin(pin, TipBody.forEmergencyContactRemove())))

        suspend fun getFiats() = accountService.getFiats()

        suspend fun getPinLogs(
            category: String? = null,
            offset: String? = null,
            limit: Int? = null,
        ) = accountService.getPinLogs(category, offset, limit)

        suspend fun errorCount(): Int =
            PIN_ERROR_MAX -
                withContext(Dispatchers.IO) {
                    val response = getPinLogs("PIN_INCORRECT", limit = PIN_ERROR_MAX)
                    if (response.isSuccess) {
                        val list = response.data ?: return@withContext 0
                        for ((index, item) in list.withIndex()) {
                            if (index == PIN_ERROR_MAX - 1 && item.createdAt.within24Hours()) {
                                return@withContext PIN_ERROR_MAX
                            } else if (item.createdAt.within24Hours()) {
                                continue
                            } else {
                                return@withContext index
                            }
                        }
                        return@withContext 0
                    } else {
                        return@withContext 0
                    }
                }

        suspend fun preferences(request: AccountUpdateRequest) = accountService.preferences(request)

        suspend fun signMultisigs(
            requestId: String,
            pinRequest: PinRequest,
        ) =
            accountService.signMultisigs(requestId, pinRequest)

        suspend fun unlockMultisigs(
            requestId: String,
            pinRequest: PinRequest,
        ) =
            accountService.unlockMultisigs(requestId, pinRequest)

        suspend fun cancelMultisigs(requestId: String) =
            accountService.cancelMultisigs(requestId)

        suspend fun getToken(tokenId: String) = accountService.getToken(tokenId)

        suspend fun signCollectibleTransfer(
            requestId: String,
            pinRequest: CollectibleRequest,
        ) =
            accountService.signCollectibleTransfer(requestId, pinRequest)

        suspend fun unlockCollectibleTransfer(
            requestId: String,
            pinRequest: CollectibleRequest,
        ) =
            accountService.unlockCollectibleTransfer(requestId, pinRequest)

        suspend fun cancelCollectibleTransfer(requestId: String) =
            accountService.cancelCollectibleTransfer(requestId)

        suspend fun transactions(rawTransactionsRequest: RawTransactionsRequest) =
            accountService.transactions(rawTransactionsRequest)

        suspend fun addFavoriteApp(appId: String) = userService.addFavoriteApp(appId)

        suspend fun insertFavoriteApp(favoriteApp: FavoriteApp) = favoriteAppDao.insert(favoriteApp)

        suspend fun insertFavoriteApps(
            userId: String,
            favoriteApps: List<FavoriteApp>,
        ) {
            favoriteAppDao.deleteByUserId(userId)
            favoriteAppDao.insertList(favoriteApps)
        }

        suspend fun getUserFavoriteApps(userId: String) = userService.getUserFavoriteApps(userId)

        fun observerFavoriteApps(userId: String) = appDao.observerFavoriteApps(userId)

        suspend fun getFavoriteAppsByUserId(userId: String) = appDao.getFavoriteAppsByUserId(userId)

        suspend fun getUnfavoriteApps() = appDao.getUnfavoriteApps(Session.getAccountId()!!)

        suspend fun removeFavoriteApp(appId: String) = userService.removeFavoriteApp(appId)

        suspend fun deleteByAppIdAndUserId(
            appId: String,
            userId: String,
        ) =
            favoriteAppDao.deleteByAppIdAndUserId(appId, userId)

        suspend fun getApps() = appDao.getApps()

        suspend fun refreshAppNotExist(appIds: List<String>) {
            appIds.filter { id ->
                appDao.findAppById(id) == null || userDao.suspendFindUserById(id) == null
            }.let { ids ->
                if (ids.isEmpty()) return

                val response = userService.fetchUsers(ids)
                if (response.isSuccess) {
                    response.data?.apply {
                        userDao.insertUpdateList(this, appDao)
                    }
                }
            }
        }

        suspend fun getAllExploreApps() = appDao.getAllExploreApps()

        suspend fun validateExternalAddress(
            assetId: String,
            destination: String,
            tag: String?,
        ) =
            accountService.validateExternalAddress(assetId, destination, tag)

        suspend fun refreshSticker(id: String): Sticker? {
            val sticker = stickerDao.findStickerById(id)
            if (sticker != null) return sticker

            return handleMixinResponse(
                invokeNetwork = {
                    accountService.getStickerByIdSuspend(id)
                },
                successBlock = {
                    it.data?.let { s ->
                        stickerDao.insertSuspend(s)
                        return@handleMixinResponse s
                    }
                },
            )
        }
    }
