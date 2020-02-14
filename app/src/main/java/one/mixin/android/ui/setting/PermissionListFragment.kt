package one.mixin.android.ui.setting

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.autoDispose
import kotlinx.android.synthetic.main.fragment_permission_list.*
import kotlinx.android.synthetic.main.item_permission_list.view.*
import kotlinx.android.synthetic.main.layout_permission_list_foot.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.api.response.getScopes
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.fullDate
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.auth.AuthBottomSheetDialogFragment.Companion.ARGS_AUTHORIZATION
import one.mixin.android.ui.common.BaseViewModelFragment
import one.mixin.android.ui.common.recyclerview.FooterListAdapter
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.App
import one.mixin.android.vo.Scope
import one.mixin.android.vo.convertName

class PermissionListFragment : BaseViewModelFragment<SettingViewModel>() {
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
        arguments!!.getParcelable<App>(ARGS_APP)!!
    }
    private val auth: AuthorizationResponse by lazy {
        arguments!!.getParcelable<AuthorizationResponse>(ARGS_AUTHORIZATION)!!
    }

    override fun getModelClass() = SettingViewModel::class.java

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_permission_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        permission_rv.layoutManager = LinearLayoutManager(requireContext())
        val foot = layoutInflater.inflate(R.layout.layout_permission_list_foot, permission_rv, false)
        foot.deauthorize_rl.setOnClickListener { showDialog(app) }
        foot.time_tv.text = getString(R.string.setting_auth_access, auth.createAt.fullDate(), auth.accessedAt.fullDate())
        val adapter = PermissionListAdapter()
        permission_rv.adapter = adapter
        adapter.footerView = foot
        loadData(adapter)
    }

    private fun loadData(adapter: PermissionListAdapter) = lifecycleScope.launch {
        val assets = viewModel.simpleAssetsWithBalance()
        val scopes = auth.getScopes(requireContext(), assets)
        adapter.submitList(scopes)
    }

    private fun showDialog(app: App) {
        alertDialogBuilder()
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setMessage(getString(R.string.setting_auth_cancel_msg, app.name))
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                viewModel.deauthApp(app.appId).autoDispose(stopScope).subscribe({
                    deauthCallback?.onSuccess()
                    activity?.onBackPressed()
                }, {
                    ErrorHandler.handleError(it)
                })
                dialog.dismiss()
            }.create().apply {
                setOnShowListener {
                    getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.RED)
                }
            }.show()
    }

    class PermissionListAdapter : FooterListAdapter<Scope, RecyclerView.ViewHolder>(Scope.DIFF_CALLBACK) {
        override fun getNormalViewHolder(context: Context, parent: ViewGroup) =
            ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_permission_list, parent, false))

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
            (holder as? ItemHolder)?.bindTo(getItem(pos))
        }
    }

    class ItemHolder(itemView: View) : NormalHolder(itemView) {
        fun bindTo(scope: Scope) {
            itemView.name_tv.text = scope.convertName(itemView.context)
            itemView.number_tv.text = scope.desc
        }
    }

    var deauthCallback: DeauthCallback? = null

    interface DeauthCallback {
        fun onSuccess()
    }
}
