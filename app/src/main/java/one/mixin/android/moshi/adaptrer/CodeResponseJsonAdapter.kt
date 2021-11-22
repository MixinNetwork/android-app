package one.mixin.android.moshi.adaptrer

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.internal.Util
import one.mixin.android.api.request.ParticipantRequest
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.api.response.ConversationResponse
import one.mixin.android.api.response.MultisigsResponse
import one.mixin.android.api.response.NonFungibleOutputResponse
import one.mixin.android.api.response.PaymentCodeResponse
import one.mixin.android.api.response.UserSession
import one.mixin.android.repository.QrCodeType
import one.mixin.android.vo.App
import one.mixin.android.vo.CircleConversation
import one.mixin.android.vo.CodeResponse
import one.mixin.android.vo.User
import java.lang.reflect.Constructor

class CodeResponseJsonAdapter(
    moshi: Moshi
) : JsonAdapter<CodeResponse>() {
    private val names = JsonReader.Options.of("type")

    override fun fromJson(reader: JsonReader): CodeResponse? {
        var type: String? = null
        reader.beginObject()
        val peek = reader.peekJson()
        loop@ while (peek.hasNext()) {
            when (peek.selectName(names)) {
                0 -> {
                    type = peek.nextString()
                    break@loop
                }
                else -> {
                    peek.skipName()
                    peek.skipValue()
                }
            }
        }
        var item: CodeResponse? = null
        when (type) {
            QrCodeType.authorization.name -> item = authorizationResponseFromJson(reader)
            QrCodeType.conversation.name -> item = conversationResponseFromJson(reader)
            QrCodeType.multisig_request.name -> item = multisigsResponseFromJson(reader)
            QrCodeType.non_fungible_request.name -> item = nonFungibleOutputResponseFromJson(reader)
            QrCodeType.payment.name -> item = paymentCodeResponseFromJson(reader)
            QrCodeType.user.name -> item = userFromJson(reader)
        }
        return item
    }

    private val stringAdapter: JsonAdapter<String> = moshi.adapter(
        String::class.java, emptySet(),
        "conversationId"
    )

    private val listOfParticipantRequestAdapter: JsonAdapter<List<ParticipantRequest>> =
        moshi.adapter(
            Types.newParameterizedType(List::class.java, ParticipantRequest::class.java),
            emptySet(), "participants"
        )

    private val nullableListOfUserSessionAdapter: JsonAdapter<List<UserSession>?> =
        moshi.adapter(
            Types.newParameterizedType(List::class.java, UserSession::class.java),
            emptySet(), "participantSessions"
        )

    private val nullableListOfCircleConversationAdapter: JsonAdapter<List<CircleConversation>?> =
        moshi.adapter(
            Types.newParameterizedType(List::class.java, CircleConversation::class.java),
            emptySet(), "circles"
        )

    private val appAdapter: JsonAdapter<App> = moshi.adapter(App::class.java, emptySet(), "app")

    private val listOfStringAdapter: JsonAdapter<List<String>> =
        moshi.adapter(
            Types.newParameterizedType(List::class.java, String::class.java), emptySet(),
            "scopes"
        )

    private val arrayOfStringAdapter: JsonAdapter<Array<String>> =
        moshi.adapter(Types.arrayOf(String::class.java), emptySet(), "receivers")

    private val intAdapter: JsonAdapter<Int> = moshi.adapter(Int::class.java, emptySet(), "threshold")

    private val conversationOptions: JsonReader.Options = JsonReader.Options.of(
        "conversation_id", "name",
        "category", "creator_id", "icon_url", "code_url", "announcement", "created_at",
        "participants", "participant_sessions", "circles", "mute_until"
    )

    private fun conversationResponseFromJson(reader: JsonReader): ConversationResponse {
        var conversationId: String? = null
        var name: String? = null
        var category: String? = null
        var creatorId: String? = null
        var iconUrl: String? = null
        var codeUrl: String? = null
        var announcement: String? = null
        var createdAt: String? = null
        var participants: List<ParticipantRequest>? = null
        var participantSessions: List<UserSession>? = null
        var circles: List<CircleConversation>? = null
        var muteUntil: String? = null
        while (reader.hasNext()) {
            when (reader.selectName(conversationOptions)) {
                0 -> conversationId = stringAdapter.fromJson(reader)
                    ?: throw Util.unexpectedNull("conversationId", "conversation_id", reader)
                1 -> name = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "name", "name",
                    reader
                )
                2 -> category = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "category",
                    "category", reader
                )
                3 -> creatorId = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "creatorId",
                    "creator_id", reader
                )
                4 -> iconUrl = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "iconUrl",
                    "icon_url", reader
                )
                5 -> codeUrl = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "codeUrl",
                    "code_url", reader
                )
                6 -> announcement = stringAdapter.fromJson(reader)
                    ?: throw Util.unexpectedNull("announcement", "announcement", reader)
                7 -> createdAt = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "createdAt",
                    "created_at", reader
                )
                8 -> participants = listOfParticipantRequestAdapter.fromJson(reader)
                    ?: throw Util.unexpectedNull("participants", "participants", reader)
                9 -> participantSessions = nullableListOfUserSessionAdapter.fromJson(reader)
                10 -> circles = nullableListOfCircleConversationAdapter.fromJson(reader)
                11 -> muteUntil = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "muteUntil",
                    "mute_until", reader
                )
                -1 -> {
                    // Unknown name, skip it.
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()
        return ConversationResponse(
            conversationId = conversationId ?: throw Util.missingProperty(
                "conversationId",
                "conversation_id", reader
            ),
            name = name ?: throw Util.missingProperty("name", "name", reader),
            category = category ?: throw Util.missingProperty("category", "category", reader),
            creatorId = creatorId ?: throw Util.missingProperty("creatorId", "creator_id", reader),
            iconUrl = iconUrl ?: throw Util.missingProperty("iconUrl", "icon_url", reader),
            codeUrl = codeUrl ?: throw Util.missingProperty("codeUrl", "code_url", reader),
            announcement = announcement ?: throw Util.missingProperty(
                "announcement", "announcement",
                reader
            ),
            createdAt = createdAt ?: throw Util.missingProperty("createdAt", "created_at", reader),
            participants = participants ?: throw Util.missingProperty(
                "participants", "participants",
                reader
            ),
            participantSessions = participantSessions,
            circles = circles,
            muteUntil = muteUntil ?: throw Util.missingProperty("muteUntil", "mute_until", reader)
        )
    }

    private val authorizationOptions: JsonReader.Options = JsonReader.Options.of(
        "authorization_id",
        "authorization_code", "scopes", "code_id", "app", "created_at", "accessed_at"
    )

    private fun authorizationResponseFromJson(reader: JsonReader): AuthorizationResponse {
        var authorizationId: String? = null
        var authorization_code: String? = null
        var scopes: List<String>? = null
        var codeId: String? = null
        var app: App? = null
        var createAt: String? = null
        var accessedAt: String? = null
        while (reader.hasNext()) {
            when (reader.selectName(authorizationOptions)) {
                0 -> authorizationId = stringAdapter.fromJson(reader)
                    ?: throw Util.unexpectedNull("authorizationId", "authorization_id", reader)
                1 -> authorization_code = stringAdapter.fromJson(reader)
                    ?: throw Util.unexpectedNull("authorization_code", "authorization_code", reader)
                2 -> scopes = listOfStringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "scopes",
                    "scopes", reader
                )
                3 -> codeId = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "codeId",
                    "code_id", reader
                )
                4 -> app = appAdapter.fromJson(reader) ?: throw Util.unexpectedNull("app", "app", reader)
                5 -> createAt = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "createAt",
                    "created_at", reader
                )
                6 -> accessedAt = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "accessedAt",
                    "accessed_at", reader
                )
                -1 -> {
                    // Unknown name, skip it.
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()
        return AuthorizationResponse(
            authorizationId = authorizationId ?: throw Util.missingProperty(
                "authorizationId",
                "authorization_id", reader
            ),
            authorization_code = authorization_code ?: throw Util.missingProperty(
                "authorization_code",
                "authorization_code", reader
            ),
            scopes = scopes ?: throw Util.missingProperty("scopes", "scopes", reader),
            codeId = codeId ?: throw Util.missingProperty("codeId", "code_id", reader),
            app = app ?: throw Util.missingProperty("app", "app", reader),
            createAt = createAt ?: throw Util.missingProperty("createAt", "created_at", reader),
            accessedAt = accessedAt ?: throw Util.missingProperty("accessedAt", "accessed_at", reader)
        )
    }

    private val paymentOptions: JsonReader.Options = JsonReader.Options.of(
        "code_id", "asset_id", "amount",
        "receivers", "threshold", "status", "memo", "trace_id", "created_at"
    )

    private fun paymentCodeResponseFromJson(reader: JsonReader): PaymentCodeResponse {
        var codeId: String? = null
        var assetId: String? = null
        var amount: String? = null
        var receivers: Array<String>? = null
        var threshold: Int? = null
        var status: String? = null
        var memo: String? = null
        var traceId: String? = null
        var createdAt: String? = null
        while (reader.hasNext()) {
            when (reader.selectName(paymentOptions)) {
                0 -> codeId = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "codeId",
                    "code_id", reader
                )
                1 -> assetId = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "assetId",
                    "asset_id", reader
                )
                2 -> amount = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "amount",
                    "amount", reader
                )
                3 -> receivers = arrayOfStringAdapter.fromJson(reader)
                    ?: throw Util.unexpectedNull("receivers", "receivers", reader)
                4 -> threshold = intAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "threshold",
                    "threshold", reader
                )
                5 -> status = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "status",
                    "status", reader
                )
                6 -> memo = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "memo", "memo",
                    reader
                )
                7 -> traceId = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "traceId",
                    "trace_id", reader
                )
                8 -> createdAt = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "createdAt",
                    "created_at", reader
                )
                -1 -> {
                    // Unknown name, skip it.
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()
        return PaymentCodeResponse(
            codeId = codeId ?: throw Util.missingProperty("codeId", "code_id", reader),
            assetId = assetId ?: throw Util.missingProperty("assetId", "asset_id", reader),
            amount = amount ?: throw Util.missingProperty("amount", "amount", reader),
            receivers = receivers ?: throw Util.missingProperty("receivers", "receivers", reader),
            threshold = threshold ?: throw Util.missingProperty("threshold", "threshold", reader),
            status = status ?: throw Util.missingProperty("status", "status", reader),
            memo = memo ?: throw Util.missingProperty("memo", "memo", reader),
            traceId = traceId ?: throw Util.missingProperty("traceId", "trace_id", reader),
            createdAt = createdAt ?: throw Util.missingProperty("createdAt", "created_at", reader)
        )
    }

    private val userOptions: JsonReader.Options = JsonReader.Options.of(
        "user_id", "identity_number",
        "relationship", "biography", "full_name", "avatar_url", "phone", "is_verified", "create_at",
        "mute_until", "has_pin", "app_id", "is_scam", "app"
    )

    private val nullableStringAdapter: JsonAdapter<String?> = moshi.adapter(
        String::class.java,
        emptySet(), "fullName"
    )

    private val nullableBooleanAdapter: JsonAdapter<Boolean?> =
        moshi.adapter(Boolean::class.javaObjectType, emptySet(), "isVerified")

    private val nullableAppAdapter: JsonAdapter<App?> = moshi.adapter(
        App::class.java, emptySet(),
        "app"
    )

    @Volatile
    private var constructorRef: Constructor<User>? = null

    private fun userFromJson(reader: JsonReader): User {
        var userId: String? = null
        var identityNumber: String? = null
        var relationship: String? = null
        var biography: String? = null
        var fullName: String? = null
        var avatarUrl: String? = null
        var phone: String? = null
        var isVerified: Boolean? = null
        var createdAt: String? = null
        var muteUntil: String? = null
        var hasPin: Boolean? = null
        var appId: String? = null
        var isScam: Boolean? = null
        var app: App? = null
        var appSet = false
        var mask0 = -1
        while (reader.hasNext()) {
            when (reader.selectName(userOptions)) {
                0 -> userId = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "userId",
                    "user_id", reader
                )
                1 -> identityNumber = stringAdapter.fromJson(reader)
                    ?: throw Util.unexpectedNull("identityNumber", "identity_number", reader)
                2 -> relationship = stringAdapter.fromJson(reader)
                    ?: throw Util.unexpectedNull("relationship", "relationship", reader)
                3 -> biography = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "biography",
                    "biography", reader
                )
                4 -> fullName = nullableStringAdapter.fromJson(reader)
                5 -> avatarUrl = nullableStringAdapter.fromJson(reader)
                6 -> phone = nullableStringAdapter.fromJson(reader)
                7 -> isVerified = nullableBooleanAdapter.fromJson(reader)
                8 -> createdAt = nullableStringAdapter.fromJson(reader)
                9 -> muteUntil = nullableStringAdapter.fromJson(reader)
                10 -> {
                    hasPin = nullableBooleanAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 10).inv()
                    mask0 = mask0 and 0xfffffbff.toInt()
                }
                11 -> {
                    appId = nullableStringAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 11).inv()
                    mask0 = mask0 and 0xfffff7ff.toInt()
                }
                12 -> {
                    isScam = nullableBooleanAdapter.fromJson(reader)
                    // $mask = $mask and (1 shl 12).inv()
                    mask0 = mask0 and 0xffffefff.toInt()
                }
                13 -> {
                    app = nullableAppAdapter.fromJson(reader)
                    appSet = true
                }
                -1 -> {
                    // Unknown name, skip it.
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()
        val result: User
        if (mask0 == 0xffffe3ff.toInt()) {
            // All parameters with defaults are set, invoke the constructor directly
            result = User(
                userId = userId ?: throw Util.missingProperty("userId", "user_id", reader),
                identityNumber = identityNumber ?: throw Util.missingProperty(
                    "identityNumber",
                    "identity_number", reader
                ),
                relationship = relationship ?: throw Util.missingProperty(
                    "relationship", "relationship",
                    reader
                ),
                biography = biography ?: throw Util.missingProperty("biography", "biography", reader),
                fullName = fullName,
                avatarUrl = avatarUrl,
                phone = phone,
                isVerified = isVerified,
                createdAt = createdAt,
                muteUntil = muteUntil,
                hasPin = hasPin,
                appId = appId,
                isScam = isScam
            )
        } else {
            // Reflectively invoke the synthetic defaults constructor
            @Suppress("UNCHECKED_CAST")
            val localConstructor: Constructor<User> = this.constructorRef
                ?: User::class.java.getDeclaredConstructor(
                String::class.java, String::class.java,
                String::class.java, String::class.java, String::class.java, String::class.java,
                String::class.java, Boolean::class.javaObjectType, String::class.java, String::class.java,
                Boolean::class.javaObjectType, String::class.java, Boolean::class.javaObjectType,
                Int::class.javaPrimitiveType, Util.DEFAULT_CONSTRUCTOR_MARKER
            ).also {
                this.constructorRef = it
            }
            result = localConstructor.newInstance(
                userId ?: throw Util.missingProperty("userId", "user_id", reader),
                identityNumber ?: throw Util.missingProperty("identityNumber", "identity_number", reader),
                relationship ?: throw Util.missingProperty("relationship", "relationship", reader),
                biography ?: throw Util.missingProperty("biography", "biography", reader),
                fullName,
                avatarUrl,
                phone,
                isVerified,
                createdAt,
                muteUntil,
                hasPin,
                appId,
                isScam,
                mask0,
                /* DefaultConstructorMarker */ null
            )
        }
        result.app = if (appSet) app else result.app
        return result
    }

    private val multisigsResponseOptions: JsonReader.Options = JsonReader.Options.of(
        "type", "code_id", "request_id",
        "action", "user_id", "asset_id", "amount", "senders", "receivers", "threshold", "state",
        "transaction_hash", "raw_transaction", "created_at", "memo"
    )

    private fun multisigsResponseFromJson(reader: JsonReader): MultisigsResponse {
        var type: String? = null
        var codeId: String? = null
        var requestId: String? = null
        var action: String? = null
        var userId: String? = null
        var assetId: String? = null
        var amount: String? = null
        var senders: Array<String>? = null
        var receivers: Array<String>? = null
        var threshold: Int? = null
        var state: String? = null
        var transactionHash: String? = null
        var rawTransaction: String? = null
        var createdAt: String? = null
        var memo: String? = null
        while (reader.hasNext()) {
            when (reader.selectName(multisigsResponseOptions)) {
                0 -> type = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "type", "type",
                    reader
                )
                1 -> codeId = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "codeId",
                    "code_id", reader
                )
                2 -> requestId = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "requestId",
                    "request_id", reader
                )
                3 -> action = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "action",
                    "action", reader
                )
                4 -> userId = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "userId",
                    "user_id", reader
                )
                5 -> assetId = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "assetId",
                    "asset_id", reader
                )
                6 -> amount = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "amount",
                    "amount", reader
                )
                7 -> senders = arrayOfStringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "senders",
                    "senders", reader
                )
                8 -> receivers = arrayOfStringAdapter.fromJson(reader)
                    ?: throw Util.unexpectedNull("receivers", "receivers", reader)
                9 -> threshold = intAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "threshold",
                    "threshold", reader
                )
                10 -> state = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "state", "state",
                    reader
                )
                11 -> transactionHash = stringAdapter.fromJson(reader)
                    ?: throw Util.unexpectedNull("transactionHash", "transaction_hash", reader)
                12 -> rawTransaction = stringAdapter.fromJson(reader)
                    ?: throw Util.unexpectedNull("rawTransaction", "raw_transaction", reader)
                13 -> createdAt = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "createdAt",
                    "created_at", reader
                )
                14 -> memo = nullableStringAdapter.fromJson(reader)
                -1 -> {
                    // Unknown name, skip it.
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()
        return MultisigsResponse(
            type = type ?: throw Util.missingProperty("type", "type", reader),
            codeId = codeId ?: throw Util.missingProperty("codeId", "code_id", reader),
            requestId = requestId ?: throw Util.missingProperty("requestId", "request_id", reader),
            action = action ?: throw Util.missingProperty("action", "action", reader),
            userId = userId ?: throw Util.missingProperty("userId", "user_id", reader),
            assetId = assetId ?: throw Util.missingProperty("assetId", "asset_id", reader),
            amount = amount ?: throw Util.missingProperty("amount", "amount", reader),
            senders = senders ?: throw Util.missingProperty("senders", "senders", reader),
            receivers = receivers ?: throw Util.missingProperty("receivers", "receivers", reader),
            threshold = threshold ?: throw Util.missingProperty("threshold", "threshold", reader),
            state = state ?: throw Util.missingProperty("state", "state", reader),
            transactionHash = transactionHash ?: throw Util.missingProperty(
                "transactionHash",
                "transaction_hash", reader
            ),
            rawTransaction = rawTransaction ?: throw Util.missingProperty(
                "rawTransaction",
                "raw_transaction", reader
            ),
            createdAt = createdAt ?: throw Util.missingProperty("createdAt", "created_at", reader),
            memo = memo
        )
    }

    private val nonFungibleOutputResponseOptions: JsonReader.Options = JsonReader.Options.of(
        "type", "request_id", "action",
        "user_id", "token_id", "amount", "transaction_hash", "raw_transaction", "output_id",
        "output_index", "senders_threshold", "senders", "receivers_threshold", "receivers", "memo",
        "state", "created_at", "updated_at", "signed_by", "signed_tx"
    )

    private fun nonFungibleOutputResponseFromJson(reader: JsonReader): NonFungibleOutputResponse {
        var type: String? = null
        var requestId: String? = null
        var action: String? = null
        var userId: String? = null
        var tokenId: String? = null
        var amount: String? = null
        var transactionHash: String? = null
        var rawTransaction: String? = null
        var outputId: String? = null
        var outputIndex: Int? = null
        var sendersThreshold: Int? = null
        var senders: Array<String>? = null
        var receiversThreshold: Int? = null
        var receivers: Array<String>? = null
        var memo: String? = null
        var state: String? = null
        var createdAt: String? = null
        var updatedAt: String? = null
        var signedBy: String? = null
        var signedTx: String? = null
        while (reader.hasNext()) {
            when (reader.selectName(nonFungibleOutputResponseOptions)) {
                0 -> type = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "type", "type",
                    reader
                )
                1 -> requestId = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "requestId",
                    "request_id", reader
                )
                2 -> action = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "action",
                    "action", reader
                )
                3 -> userId = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "userId",
                    "user_id", reader
                )
                4 -> tokenId = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "tokenId",
                    "token_id", reader
                )
                5 -> amount = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "amount",
                    "amount", reader
                )
                6 -> transactionHash = stringAdapter.fromJson(reader)
                    ?: throw Util.unexpectedNull("transactionHash", "transaction_hash", reader)
                7 -> rawTransaction = stringAdapter.fromJson(reader)
                    ?: throw Util.unexpectedNull("rawTransaction", "raw_transaction", reader)
                8 -> outputId = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "outputId",
                    "output_id", reader
                )
                9 -> outputIndex = intAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "outputIndex",
                    "output_index", reader
                )
                10 -> sendersThreshold = intAdapter.fromJson(reader)
                    ?: throw Util.unexpectedNull("sendersThreshold", "senders_threshold", reader)
                11 -> senders = arrayOfStringAdapter.fromJson(reader)
                    ?: throw Util.unexpectedNull("senders", "senders", reader)
                12 -> receiversThreshold = intAdapter.fromJson(reader)
                    ?: throw Util.unexpectedNull("receiversThreshold", "receivers_threshold", reader)
                13 -> receivers = arrayOfStringAdapter.fromJson(reader)
                    ?: throw Util.unexpectedNull("receivers", "receivers", reader)
                14 -> memo = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "memo", "memo",
                    reader
                )
                15 -> state = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "state", "state",
                    reader
                )
                16 -> createdAt = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "createdAt",
                    "created_at", reader
                )
                17 -> updatedAt = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "updatedAt",
                    "updated_at", reader
                )
                18 -> signedBy = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "signedBy",
                    "signed_by", reader
                )
                19 -> signedTx = stringAdapter.fromJson(reader) ?: throw Util.unexpectedNull(
                    "signedTx",
                    "signed_tx", reader
                )
                -1 -> {
                    // Unknown name, skip it.
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()
        return NonFungibleOutputResponse(
            type = type ?: throw Util.missingProperty("type", "type", reader),
            requestId = requestId ?: throw Util.missingProperty("requestId", "request_id", reader),
            action = action ?: throw Util.missingProperty("action", "action", reader),
            userId = userId ?: throw Util.missingProperty("userId", "user_id", reader),
            tokenId = tokenId ?: throw Util.missingProperty("tokenId", "token_id", reader),
            amount = amount ?: throw Util.missingProperty("amount", "amount", reader),
            transactionHash = transactionHash ?: throw Util.missingProperty(
                "transactionHash",
                "transaction_hash", reader
            ),
            rawTransaction = rawTransaction ?: throw Util.missingProperty(
                "rawTransaction",
                "raw_transaction", reader
            ),
            outputId = outputId ?: throw Util.missingProperty("outputId", "output_id", reader),
            outputIndex = outputIndex ?: throw Util.missingProperty(
                "outputIndex", "output_index",
                reader
            ),
            sendersThreshold = sendersThreshold ?: throw Util.missingProperty(
                "sendersThreshold",
                "senders_threshold", reader
            ),
            senders = senders ?: throw Util.missingProperty("senders", "senders", reader),
            receiversThreshold = receiversThreshold ?: throw Util.missingProperty(
                "receiversThreshold",
                "receivers_threshold", reader
            ),
            receivers = receivers ?: throw Util.missingProperty("receivers", "receivers", reader),
            memo = memo ?: throw Util.missingProperty("memo", "memo", reader),
            state = state ?: throw Util.missingProperty("state", "state", reader),
            createdAt = createdAt ?: throw Util.missingProperty("createdAt", "created_at", reader),
            updatedAt = updatedAt ?: throw Util.missingProperty("updatedAt", "updated_at", reader),
            signedBy = signedBy ?: throw Util.missingProperty("signedBy", "signed_by", reader),
            signedTx = signedTx ?: throw Util.missingProperty("signedTx", "signed_tx", reader)
        )
    }

    override fun toJson(writer: JsonWriter, value: CodeResponse?) {
        throw JsonDataException("Does not allow serialization")
    }
}
