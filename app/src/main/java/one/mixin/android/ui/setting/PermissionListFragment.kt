package one.mixin.android.ui.setting

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.api.response.getScopes
import one.mixin.android.databinding.FragmentPermissionListBinding
import one.mixin.android.databinding.ItemPermissionListBinding
import one.mixin.android.databinding.LayoutPermissionListFootBinding
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.auth.AuthBottomSheetDialogFragment.Companion.ARGS_AUTHORIZATION
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.recyclerview.FooterListAdapter
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.setting.SettingActivity.Companion.ARGS_SUCCESS
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.App
import one.mixin.android.vo.Scope
import one.mixin.android.vo.convertName

@AndroidEntryPoint
class PermissionListFragment : BaseFragment(R.layout.fragment_permission_list) {
    companion object {
        const val TAG = "PermissionListFragment"
        const val ARGS_APP = "args_app"

        fun newInstance(
            app: App,
            authorization: AuthorizationResponse
        ) = PermissionListFragment().withArgs {
            putParcelable(ARGS_APP, app)
            putParcelable(ARGS_AUTHORIZATION, authorization)
        }
    }

    private val app: App by lazy {
        requireArguments().getParcelable(ARGS_APP)!!
    }
    private val auth: AuthorizationResponse by lazy {
        requireArguments().getParcelable(ARGS_AUTHORIZATION)!!
    }

    private val viewModel by viewModels<SettingViewModel>()
    private val binding by viewBinding(FragmentPermissionListBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            titleView.leftIb.setOnClickListener { activity?.onBackPressed() }
            permissionRv.layoutManager = LinearLayoutManager(requireContext())
            val footBinding = LayoutPermissionListFootBinding.inflate(layoutInflater, permissionRv, false).apply {
                revokeRl.setOnClickListener { showDialog(app) }
                timeTv.text = getString(R.string.setting_auth_access, auth.createAt.fullDate(), auth.accessedAt.fullDate())
            }
            val adapter = PermissionListAdapter()
            permissionRv.adapter = adapter
            adapter.footerView = footBinding.root
            loadData(adapter)
        }
    }

    private fun loadData(adapter: PermissionListAdapter) = lifecycleScope.launch {
        val assets = viewModel.simpleAssetsWithBalance()
        val scopes = auth.getScopes(requireContext(), assets)
        adapter.submitList(scopes)
    }

    private fun showDialog(app: App) {
        alertDialogBuilder()
            .setNegativeButton(R.string.Cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setMessage(getString(R.string.setting_auth_cancel_msg, app.name))
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                val pb = indeterminateProgressDialog(message = R.string.pb_dialog_message).apply {
                    setCancelable(false)
                }
                viewModel.deauthApp(app.appId).autoDispose(stopScope).subscribe(
                    {
                        clearRelatedCookies(app)
                        deauthCallback?.onSuccess(app.homeUri)

                        pb.dismiss()
                        activity?.setResult(
                            Activity.RESULT_OK,
                            Intent().apply {
                                putExtra(ARGS_SUCCESS, true)
                            }
                        )
                        activity?.onBackPressed()
                    },
                    {
                        pb.dismiss()
                        ErrorHandler.handleError(it)
                    }
                )
                dialog.dismiss()
            }.create().apply {
                setOnShowListener {
                    getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.RED)
                }
            }.show()
    }

    private fun clearRelatedCookies(app: App) {
        val cm = CookieManager.getInstance()
        val cookieString = cm.getCookie(app.homeUri)
        if (cookieString.isNullOrBlank()) return

        val cookies = cookieString.split(";")
        val keys = mutableListOf<String>()
        cookies.forEach { c ->
            val kv = c.split("=")
            keys.add(kv[0].trim())
        }
        keys.forEach { k ->
            cm.setCookie(app.homeUri, "$k=")
        }
    }

    class PermissionListAdapter : FooterListAdapter<Scope, RecyclerView.ViewHolder>(Scope.DIFF_CALLBACK) {
        override fun getNormalViewHolder(context: Context, parent: ViewGroup) =
            ItemHolder(ItemPermissionListBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
            (holder as? ItemHolder)?.bindTo(getItem(pos))
        }
    }

    class ItemHolder(private val itemBinding: ItemPermissionListBinding) : NormalHolder(itemBinding.root) {
        fun bindTo(scope: Scope) {
            itemBinding.apply {
                nameTv.text = scope.convertName(itemView.context)
                numberTv.text = scope.desc
            }
        }
    }

    var deauthCallback: DeauthCallback? = null

    interface DeauthCallback {
        fun onSuccess(url: String)
    }
}
