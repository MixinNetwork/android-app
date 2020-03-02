package one.mixin.android.ui.auth

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.ArraySet
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.autoDispose
import kotlinx.android.synthetic.main.fragment_auth.view.*
import kotlinx.android.synthetic.main.item_third_login_scope.view.*
import kotlinx.android.synthetic.main.view_round_title.view.*
import one.mixin.android.R
import one.mixin.android.api.request.AuthorizeRequest
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.extension.isWebUrl
import one.mixin.android.extension.loadCircleImage
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.Scope
import one.mixin.android.vo.Scope.Companion.SCOPES
import one.mixin.android.vo.convertName
import one.mixin.android.widget.BottomSheet

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
    }

    private val scopes: List<Scope> by lazy {
        requireArguments().getParcelableArrayList<Scope>(ARGS_SCOPES)!!
    }

    private val auth: AuthorizationResponse by lazy {
        requireArguments().getParcelable<AuthorizationResponse>(ARGS_AUTHORIZATION)!!
    }

    private val scopeAdapter: ScopeAdapter by lazy {
        ScopeAdapter(scopes)
    }

    private var success = false

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_auth, null)
        dialog as BottomSheet
        dialog.setCustomView(contentView)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.title_view.right_iv.setOnClickListener { dismiss() }
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
            bottomViewModel.authorize(request).autoDispose(stopScope).subscribe({ r ->
                contentView.confirm_anim?.displayedChild = POS_TEXT
                contentView.confirm_anim.isEnabled = true
                if (r.isSuccess && r.data != null) {
                    val redirectUri = r.data!!.app.redirectUri
                    redirect(redirectUri, r.data!!.authorization_code)
                    success = true
                    dismiss()
                } else {
                    ErrorHandler.handleMixinError(r.errorCode, r.errorDescription)
                }
            }, { t: Throwable ->
                contentView.confirm_anim?.displayedChild = POS_TEXT
                contentView.confirm_anim.isEnabled = true
                ErrorHandler.handleError(t)
            })
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (!success && isAdded) {
            val request = AuthorizeRequest(auth.authorizationId, listOf())
            bottomViewModel.authorize(request)
                .autoDispose(stopScope)
                .subscribe({
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
            addAll(SCOPES)
        }
        var onScopeListener: OnScopeListener? = null

        override fun getItemCount(): Int = scopes.size

        override fun onBindViewHolder(holder: ScopeViewHolder, position: Int) {
            val scope = scopes[position]
            holder.itemView.title.text = scope.convertName(holder.itemView.context)
            holder.itemView.desc.text = scope.desc
            holder.itemView.cb.isChecked = checkedScopes.contains(scope.name)
            if (scope.name == SCOPES[0]) {
                holder.itemView.cb.isEnabled = false
            } else {
                holder.itemView.cb.isEnabled = true
                holder.itemView.setOnClickListener { onScopeListener?.onScope(holder.itemView, position) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScopeViewHolder =
            ScopeViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_third_login_scope, parent, false))
    }

    class ScopeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    interface OnScopeListener {
        fun onScope(itemView: View, position: Int)
    }
}
