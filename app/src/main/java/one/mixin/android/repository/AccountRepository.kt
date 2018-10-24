package one.mixin.android.repository

import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.AccountRequest
import one.mixin.android.api.request.AccountUpdateRequest
import one.mixin.android.api.request.AuthorizeRequest
import one.mixin.android.api.request.DeauthorRequest
import one.mixin.android.api.request.PinRequest
import one.mixin.android.api.request.StickerAddRequest
import one.mixin.android.api.request.VerificationRequest
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.api.response.ConversationResponse
import one.mixin.android.api.response.VerificationResponse
import one.mixin.android.api.service.AccountService
import one.mixin.android.api.service.AuthorizationService
import one.mixin.android.api.service.ConversationService
import one.mixin.android.api.service.GiphyService
import one.mixin.android.api.service.UserService
import one.mixin.android.db.AppDao
import one.mixin.android.db.StickerAlbumDao
import one.mixin.android.db.StickerDao
import one.mixin.android.db.StickerRelationshipDao
import one.mixin.android.db.UserDao
import one.mixin.android.db.insertUpdate
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.util.encryptPin
import one.mixin.android.vo.Account
import one.mixin.android.vo.Sticker
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
    private val authService: AuthorizationService,
    private val stickerDao: StickerDao,
    private val stickerAlbumDao: StickerAlbumDao,
    private val stickerRelationshipDao: StickerRelationshipDao,
    private val giphyService: GiphyService
) {

    fun verification(request: VerificationRequest): Observable<MixinResponse<VerificationResponse>> =
        accountService.verification(request)

    fun create(id: String, request: AccountRequest): Observable<MixinResponse<Account>> =
        accountService.create(id, request)

    fun changePhone(id: String, request: AccountRequest): Observable<MixinResponse<Account>> =
        accountService.changePhone(id, request)

    fun update(request: AccountUpdateRequest): Observable<MixinResponse<Account>> =
        accountService.update(request)

    fun join(conversationId: String): Observable<MixinResponse<ConversationResponse>> {
        return conversationService.join(conversationId)
    }

    fun searchCode(code: String): Observable<Pair<String, Any>> =
        accountService.code(code).subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
            .map { response ->
                if (!response.isSuccess) {
                    ErrorHandler.handleMixinError(response.errorCode)
                    return@map Pair("", "")
                }
                val result: Pair<String, Any>
                val type = response.data?.get("type")?.asString
                result = when (type) {
                    QrCodeType.user.name -> {
                        val user = Gson().fromJson(response.data, User::class.java)
                        userDao.insertUpdate(user, appDao)
                        Pair(type, user)
                    }
                    QrCodeType.conversation.name -> {
                        val conversationResponse = Gson().fromJson(response.data, ConversationResponse::class.java)
                        Pair(type, conversationResponse)
                    }
                    QrCodeType.authorization.name -> {
                        val resp = Gson().fromJson(response.data, AuthorizationResponse::class.java)
                        Pair(type, resp)
                    }
                    else -> Pair("", "")
                }
                result
            }.doOnError {
                ErrorHandler.handleError(it)
            }

    fun search(query: String): Observable<MixinResponse<User>> =
        userService.search(query)

    fun logout(): Observable<MixinResponse<Unit>> =
        accountService.logout()

    fun findUsersByType(relationship: String) = userDao.findUsersByType(relationship)

    fun redeem(code: String): Observable<MixinResponse<Account>> = accountService.invitations(code)

    fun updatePin(request: PinRequest) = accountService.updatePin(request)

    fun verifyPin(code: String): Observable<MixinResponse<Account>> =
        accountService.verifyPin(PinRequest(encryptPin(Session.getPinToken()!!, code)!!))
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun authorize(request: AuthorizeRequest) = authService.authorize(request)

    fun deAuthorize(deauthorRequest: DeauthorRequest) = authService.deAuthorize(deauthorRequest)

    fun getSystemAlbums() = stickerAlbumDao.getSystemAlbums()

    fun getPersonalAlbums() = stickerAlbumDao.getPersonalAlbums()

    fun observeStickers(id: String) = stickerRelationshipDao.observeStickersByAlbumId(id)

    fun observePersonalStickers() = stickerRelationshipDao.observePersonalStickers()

    fun recentUsedStickers() = stickerDao.recentUsedStickers()

    fun updateUsedAt(stickerId: String, at: String) = stickerDao.updateUsedAt(stickerId, at)

    fun addSticker(request: StickerAddRequest) = accountService.addSticker(request)

    fun addStickerLocal(sticker: Sticker, albumId: String) {
        stickerDao.insertUpdate(sticker)
        stickerRelationshipDao.insert(StickerRelationship(albumId, sticker.stickerId))
    }

    fun trendingGifs(limit: Int, offset: Int) = giphyService.trendingGifs(limit, offset)

    fun searchGifs(query: String, limit: Int, offset: Int) = giphyService.searchGifs(query, limit, offset)
}