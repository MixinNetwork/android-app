package one.mixin.android.ui.auth

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.v7.widget.RecyclerView
import android.util.ArraySet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.uber.autodispose.kotlin.autoDisposable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_auth.view.*
import kotlinx.android.synthetic.main.item_third_login_scope.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.api.request.AuthorizeRequest
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.extension.displaySize
import one.mixin.android.extension.isWebUrl
import one.mixin.android.extension.loadCircleImage
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.ui.qr.CaptureFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.vo.Asset
import one.mixin.android.widget.BottomSheet
import org.jetbrains.anko.backgroundResource
import org.jetbrains.anko.collections.forEachWithIndex
import org.jetbrains.anko.imageResource

class AuthBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "AuthBottomSheetDialogFragment"

        const val POS_TEXT = 0
        const val POS_PB = 1

        const val ARGS_SCOPES = "args_scopes"
        const val ARGS_AUTHORIZATION = "args_authorization"

        fun newInstance(scopes: ArrayList<Scope>, auth: AuthorizationResponse) =
            AuthBottomSheetDialogFragment().withArgs {
                putParcelableArrayList(ARGS_SCOPES, scopes)
                putParcelable(ARGS_AUTHORIZATION, auth)
            }

        fun handleAuthorization(
            ctx: Context,
            authorization: AuthorizationResponse,
            assets: List<Asset>
        ): ArrayList<Scope> {
            val scopes = arrayListOf<Scope>()
            val user = Session.getAccount() ?: return scopes
            for (s in authorization.scopes) {
                when (s) {
                    CaptureFragment.SCOPES[0] ->
                        scopes.add(Scope(s, ctx.getString(R.string.auth_profile_content,
                            user.full_name, user.identity_number)))
                    CaptureFragment.SCOPES[1] ->
                        scopes.add(Scope(s, user.phone))
                    CaptureFragment.SCOPES[2] -> {
                        val sb = StringBuilder()
                        assets.forEachWithIndex { i, a ->
                            if (i > 1) return@forEachWithIndex

                            sb.append("${a.balance} ${a.symbol}")
                            if (i != assets.size - 1 && i < 1) {
                                sb.append(", ")
                            }
                        }
                        if (assets.size > 2) {
                            scopes.add(Scope(s, ctx.getString(R.string.auth_assets_more, sb.toString())))
                        } else {
                            scopes.add(Scope(s, sb.toString()))
                        }
                    }
                    CaptureFragment.SCOPES[3] ->
                        scopes.add(Scope(s, ctx.getString(R.string.auth_apps_read_description)))
                    CaptureFragment.SCOPES[4] ->
                        scopes.add(Scope(s, ctx.getString(R.string.auth_apps_write_description)))
                    CaptureFragment.SCOPES[5] ->
                        scopes.add(Scope(s, ctx.getString(R.string.auth_apps_read_description)))
                }
            }
            return scopes
        }
    }

    private val scopes: List<Scope> by lazy {
        arguments!!.getParcelableArrayList<Scope>(ARGS_SCOPES)
    }

    private val auth: AuthorizationResponse by lazy {
        arguments!!.getParcelable<AuthorizationResponse>(ARGS_AUTHORIZATION)
    }

    private val scopeAdapter: ScopeAdapter by lazy {
        ScopeAdapter(scopes)
    }

    private val miniHeight by lazy {
        context!!.displaySize().y * 3 / 4
    }

    private val maxHeight by lazy {
        context!!.displaySize().y - context!!.statusBarHeight()
    }

    private var success = false

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_auth, null)
        contentView.title_view.backgroundResource = R.drawable.bg_round_top_white
        dialog as BottomSheet
        dialog.setCustomView(contentView)
        dialog.setCustomViewHeight(miniHeight)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.title_view.setSubTitle(auth.app.name, getString(R.string.auth_request))
        contentView.title_view.left_ib.setOnClickListener { dialog.dismiss() }
        contentView.title_view.right_animator.setOnClickListener {
            val expand = (dialog as BottomSheet).getCustomViewHeight() == maxHeight
            if (expand) {
                contentView.title_view.right_ib.imageResource = R.drawable.ic_zoom_in
                (dialog as BottomSheet).setCustomViewHeight(miniHeight)
            } else {
                contentView.title_view.right_ib.imageResource = R.drawable.ic_zoom_out
                (dialog as BottomSheet).setCustomViewHeight(maxHeight)
            }
        }
        contentView.avatar.loadCircleImage(auth.app.icon_url, R.mipmap.ic_launcher_round)
        contentView.scope_rv.adapter = scopeAdapter
        scopeAdapter.onScopeListener = object : OnScopeListener {
            override fun onScope(itemView: View, position: Int) {
                itemView.cb.isChecked = !itemView.cb.isChecked
                if (itemView.cb.isChecked) {
                    scopeAdapter.checkedScopes.add(scopes[position].name)
                } else {
                    scopeAdapter.checkedScopes.remove(scopes[position].name)
                }
            }
        }
        contentView.confirm_anim.setOnClickListener {
            contentView.confirm_anim.displayedChild = POS_PB
            contentView.confirm_anim.isEnabled = false
            val request = AuthorizeRequest(auth.authorizationId, scopeAdapter.checkedScopes.toList())
            bottomViewModel.authorize(request).autoDisposable(scopeProvider).subscribe({ r ->
                contentView.confirm_anim?.displayedChild = POS_TEXT
                contentView.confirm_anim.isEnabled = true
                if (r.isSuccess && r.data != null) {
                    val redirectUri = r.data!!.app.redirectUri
                    redirect(redirectUri, r.data!!.authorization_code)
                    success = true
                    dismiss()
                } else {
                    ErrorHandler.handleMixinError(r.errorCode)
                }
            }, { t: Throwable ->
                contentView.confirm_anim?.displayedChild = POS_TEXT
                contentView.confirm_anim.isEnabled = true
                ErrorHandler.handleError(t)
            })
        }
    }

    @SuppressLint("CheckResult")
    override fun onDismiss(dialog: DialogInterface?) {
        if (!success && isAdded) {
            val request = AuthorizeRequest(auth.authorizationId, listOf())
            bottomViewModel.authorize(request).subscribe({
                if (it.isSuccess && it.data != null) {
                    redirect(it.data!!.app.redirectUri, it.data!!.authorization_code)
                }
            }, {
                ErrorHandler.handleError(it)
            })
        }
        super.onDismiss(dialog)
    }

    private fun redirect(uri: String, code: String?) {
        if (!uri.isWebUrl()) {
            val builder = Uri.parse(uri).buildUpon()
            val redirect = if (code.isNullOrEmpty()) {
                builder.appendQueryParameter("error", "access_denied").build()
            } else {
                builder.appendQueryParameter("code", code).build()
            }
            val intent = Intent.parseUri(redirect.toString(), Intent.URI_INTENT_SCHEME)
            val info = context?.packageManager?.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (info != null) {
                context?.startActivity(intent)
            }
        }
    }

    private class ScopeAdapter(val scopes: List<Scope>) : RecyclerView.Adapter<ScopeViewHolder>() {
        val checkedScopes = ArraySet<String>().apply {
            addAll(CaptureFragment.SCOPES)
        }
        var onScopeListener: OnScopeListener? = null

        override fun getItemCount(): Int = scopes.size

        override fun onBindViewHolder(holder: ScopeViewHolder, position: Int) {
            val scope = scopes[position]
            holder.itemView.title.text = x(holder.itemView.context, scope.name)
            holder.itemView.desc.text = scope.desc
            holder.itemView.cb.isChecked = checkedScopes.contains(scope.name)
            if (scope.name == CaptureFragment.SCOPES[0]) {
                holder.itemView.cb.isEnabled = false
            } else {
                holder.itemView.cb.isEnabled = true
                holder.itemView.setOnClickListener { onScopeListener?.onScope(holder.itemView, position) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScopeViewHolder =
            ScopeViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_third_login_scope, parent, false))

        private fun x(ctx: Context, scope: String): String {
            val id = when (scope) {
                CaptureFragment.SCOPES[0] -> R.string.auth_public_profile
                CaptureFragment.SCOPES[1] -> R.string.auth_phone_number
                CaptureFragment.SCOPES[2] -> R.string.auth_assets
                CaptureFragment.SCOPES[3] -> R.string.auth_app_read
                CaptureFragment.SCOPES[4] -> R.string.auth_apps_write
                CaptureFragment.SCOPES[5] -> R.string.auth_permission_contacts_read
                else -> R.string.auth_public_profile
            }
            return ctx.getString(id)
        }
    }

    class ScopeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    @SuppressLint("ParcelCreator")
    @Parcelize
    data class Scope(val name: String, val desc: String) : Parcelable

    interface OnScopeListener {
        fun onScope(itemView: View, position: Int)
    }
}
