package one.mixin.android.ui.auth

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.collection.ArraySet
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.AuthorizeRequest
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.databinding.FragmentAuthBinding
import one.mixin.android.databinding.ItemThirdLoginScopeBinding
import one.mixin.android.extension.isWebUrl
import one.mixin.android.extension.loadCircleImage
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Scope
import one.mixin.android.vo.Scope.Companion.SCOPES
import one.mixin.android.vo.convertName
import one.mixin.android.widget.BottomSheet
import timber.log.Timber

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

    private val binding by viewBinding(FragmentAuthBinding::inflate)

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        dialog as BottomSheet
        dialog.setCustomView(contentView)

        binding.apply {
            titleView.rightIv.setOnClickListener { dismiss() }
            avatar.loadCircleImage(auth.app.iconUrl, R.mipmap.ic_launcher_round)
            scopeRv.adapter = scopeAdapter
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
            confirmAnim.setOnClickListener {
                confirmAnim.displayedChild = POS_PB
                confirmAnim.isEnabled = false
                val request = AuthorizeRequest(auth.authorizationId, scopeAdapter.checkedScopes.toList())
                lifecycleScope.launch {
                    bottomViewModel // init on main thread
                    handleMixinResponse(
                        invokeNetwork = { bottomViewModel.authorize(request) },
                        switchContext = Dispatchers.IO,
                        successBlock = {
                            val data = it.data ?: return@handleMixinResponse
                            val redirectUri = data.app.redirectUri
                            redirect(redirectUri, data.authorization_code)
                            success = true
                            dismiss()
                        },
                        doAfterNetworkSuccess = {
                            confirmAnim.displayedChild = POS_TEXT
                            confirmAnim.isEnabled = true
                        },
                        exceptionBlock = {
                            confirmAnim.displayedChild = POS_TEXT
                            confirmAnim.isEnabled = true
                            return@handleMixinResponse false
                        }
                    )
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (!success && isAdded) {
            val request = AuthorizeRequest(auth.authorizationId, listOf())
            lifecycleScope.launch {
                bottomViewModel // init on main thread
                handleMixinResponse(
                    invokeNetwork = { bottomViewModel.authorize(request) },
                    switchContext = Dispatchers.IO,
                    successBlock = {
                        val data = it.data ?: return@handleMixinResponse
                        redirect(data.app.redirectUri, data.authorization_code)
                    }
                )
            }
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
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(redirect.toString())).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        flags = Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER
                    }
                }
                context?.startActivity(intent)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private class ScopeAdapter(val scopes: List<Scope>) : RecyclerView.Adapter<ScopeViewHolder>() {
        val checkedScopes = ArraySet<String>().apply {
            addAll(scopes.map { it.name })
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
