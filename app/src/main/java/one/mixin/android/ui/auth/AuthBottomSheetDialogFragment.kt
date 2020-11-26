package one.mixin.android.ui.auth

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.ArraySet
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_auth.view.*
import one.mixin.android.R
import one.mixin.android.api.request.AuthorizeRequest
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.databinding.ItemThirdLoginScopeBinding
import one.mixin.android.extension.isWebUrl
import one.mixin.android.extension.loadCircleImage
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.Scope
import one.mixin.android.vo.Scope.Companion.SCOPES
import one.mixin.android.vo.convertName
import one.mixin.android.widget.BottomSheet

@AndroidEntryPoint
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
        requireArguments().getParcelableArrayList(ARGS_SCOPES)!!
    }

    private val auth: AuthorizationResponse by lazy {
        requireArguments().getParcelable(ARGS_AUTHORIZATION)!!
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

        contentView.title_view.rightIv.setOnClickListener { dismiss() }
        contentView.avatar.loadCircleImage(auth.app.iconUrl, R.mipmap.ic_launcher_round)
        contentView.scope_rv.adapter = scopeAdapter
        scopeAdapter.onScopeListener = object : OnScopeListener {
            override fun onScope(binding: ItemThirdLoginScopeBinding, position: Int) {
                binding.cb.isChecked = !binding.cb.isChecked
                if (binding.cb.isChecked) {
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
            bottomViewModel.authorize(request).autoDispose(stopScope).subscribe(
                { r ->
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
                },
                { t: Throwable ->
                    contentView.confirm_anim?.displayedChild = POS_TEXT
                    contentView.confirm_anim.isEnabled = true
                    ErrorHandler.handleError(t)
                }
            )
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (!success && isAdded) {
            val request = AuthorizeRequest(auth.authorizationId, listOf())
            bottomViewModel.authorize(request)
                .autoDispose(stopScope)
                .subscribe(
                    {
                        if (it.isSuccess && it.data != null) {
                            redirect(it.data!!.app.redirectUri, it.data!!.authorization_code)
                        }
                    },
                    {
                        ErrorHandler.handleError(it)
                    }
                )
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
            holder.bind.title.text = scope.convertName(holder.itemView.context)
            holder.bind.desc.text = scope.desc
            holder.bind.cb.isChecked = checkedScopes.contains(scope.name)
            if (scope.name == SCOPES[0]) {
                holder.bind.cb.isEnabled = false
            } else {
                holder.bind.cb.isEnabled = true
                holder.itemView.setOnClickListener { onScopeListener?.onScope(holder.bind, position) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScopeViewHolder =
            ScopeViewHolder(ItemThirdLoginScopeBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    class ScopeViewHolder(val bind: ItemThirdLoginScopeBinding) : RecyclerView.ViewHolder(bind.root)

    interface OnScopeListener {
        fun onScope(binding: ItemThirdLoginScopeBinding, position: Int)
    }
}
