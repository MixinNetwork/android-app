package one.mixin.android.api.response

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import one.mixin.android.R
import one.mixin.android.util.Session
import one.mixin.android.vo.App
import one.mixin.android.vo.Asset
import one.mixin.android.vo.Scope
import one.mixin.android.vo.Scope.Companion.SCOPES

@SuppressLint("ParcelCreator")
@Parcelize
class AuthorizationResponse(
    @SerializedName("authorization_id")
    val authorizationId: String,
    val authorization_code: String,
    val scopes: List<String>,
    @SerializedName("code_id")
    val codeId: String,
    val app: App,
    @SerializedName("created_at")
    val createAt: String,
    @SerializedName("accessed_at")
    val accessedAt: String
) : Parcelable

fun AuthorizationResponse.getScopes(
    ctx: Context,
    assets: List<Asset>
): ArrayList<Scope> {
    val scopes = arrayListOf<Scope>()
    val user = Session.getAccount() ?: return scopes
    for (s in this.scopes) {
        when (s) {
            SCOPES[0] ->
                scopes.add(
                    Scope(
                        s, ctx.getString(
                            R.string.auth_profile_content,
                            user.full_name, user.identity_number
                        )
                    )
                )
            SCOPES[1] ->
                scopes.add(Scope(s, user.phone))
            SCOPES[2] ->
                scopes.add(
                    Scope(
                        s,
                        ctx.getString(R.string.auth_messages_represent_description)
                    )
                )
            SCOPES[3] ->
                scopes.add(
                    Scope(
                        s,
                        ctx.getString(R.string.auth_permission_contacts_read_description)
                    )
                )
            SCOPES[4] -> {
                val sb = StringBuilder()
                assets.forEachIndexed { i, a ->
                    if (i > 1) return@forEachIndexed

                    sb.append("${a.balance} ${a.symbol}")
                    if (i != assets.size - 1 && i < 1) {
                        sb.append(", ")
                    }
                }
                if (assets.size > 2) {
                    scopes.add(
                        Scope(
                            s,
                            ctx.getString(R.string.auth_assets_more, sb.toString())
                        )
                    )
                } else {
                    scopes.add(Scope(s, sb.toString()))
                }
            }
            SCOPES[5] ->
                scopes.add(
                    Scope(
                        s,
                        ctx.getString(R.string.auth_snapshot_read_description)
                    )
                )
            SCOPES[6] ->
                scopes.add(
                    Scope(
                        s,
                        ctx.getString(R.string.auth_apps_read_description)
                    )
                )
            SCOPES[7] ->
                scopes.add(
                    Scope(
                        s,
                        ctx.getString(R.string.auth_apps_write_description)
                    )
                )
        }
    }
    return scopes
}
