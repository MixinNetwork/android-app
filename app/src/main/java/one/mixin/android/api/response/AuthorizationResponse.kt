package one.mixin.android.api.response

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.vo.App
import one.mixin.android.vo.Scope

@SuppressLint("ParcelCreator")
@Parcelize
class AuthorizationResponse(
    @SerializedName("authorization_id")
    val authorizationId: String,
    @SerializedName("authorization_code")
    val authorizationCode: String,
    val scopes: List<String>,
    @SerializedName("code_id")
    val codeId: String,
    val app: App,
    @SerializedName("created_at")
    val createAt: String,
    @SerializedName("accessed_at")
    val accessedAt: String,
) : Parcelable

fun AuthorizationResponse.getScopes(
    ctx: Context,
): ArrayList<Scope> {
    val scopes = arrayListOf<Scope>()
    scopes.addAll(
        this.scopes.map {
            Scope.generateScopeFromString(ctx, it)
        }
    )
    return scopes
}
