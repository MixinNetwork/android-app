package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.autoDispose
import kotlinx.android.synthetic.main.fragment_authentications.*
import kotlinx.android.synthetic.main.item_auth.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.extension.navTo
import one.mixin.android.ui.common.BaseViewModelFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.App

class AuthenticationsFragment : BaseViewModelFragment<SettingViewModel>() {
    companion object {
        const val TAG = "AuthenticationsFragment"

        fun newInstance() = AuthenticationsFragment()
    }

    override fun getModelClass() = SettingViewModel::class.java

    private var list: MutableList<App>? = null
    private var authResponseList: MutableList<AuthorizationResponse>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_authentications, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        val adapter = AuthenticationAdapter(object : OnAppClick {
            override fun onClick(app: App, position: Int) {
                val auth = authResponseList?.get(position) ?: return
                val fragment = PermissionListFragment.newInstance(app, auth)
                fragment.deauthCallback = object : PermissionListFragment.DeauthCallback {
                    override fun onSuccess() {
                        list?.removeAt(position)
                        authResponseList?.removeAt(position)
                        auth_rv.adapter?.notifyItemRemoved(position)
                    }
                }
                navTo(fragment, PermissionListFragment.TAG)
            }
        })
        viewModel.authorizations().autoDispose(stopScope).subscribe({ list ->
            if (list.isSuccess) {
                this.list = list.data?.map {
                    it.app
                }?.run {
                    MutableList(this.size) {
                        this[it]
                    }
                }
                if (this.list?.isNotEmpty() == true) {
                    auth_va.displayedChild = 0
                } else {
                    auth_va.displayedChild = 1
                }
                adapter.submitList(this.list)

                authResponseList = list.data?.toMutableList()
            } else {
                auth_va.displayedChild = 1
            }
            progress.visibility = View.GONE
        }, {
            progress.visibility = View.GONE
            auth_va.displayedChild = 1
            ErrorHandler.handleError(it)
        })
        auth_rv.adapter = adapter
    }

    class AuthenticationAdapter(private val onAppClick: OnAppClick) : ListAdapter<App, ItemHolder>(App.DIFF_CALLBACK) {
        override fun onBindViewHolder(itemHolder: ItemHolder, pos: Int) {
            itemHolder.bindTo(getItem(pos), onAppClick)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder =
            ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_auth, parent, false))
    }

    interface OnAppClick {
        fun onClick(app: App, position: Int)
    }

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindTo(app: App, onAppClick: OnAppClick) {
            itemView.avatar.setInfo(app.name, app.iconUrl, app.appId)
            itemView.name_tv.text = app.name
            itemView.number_tv.text = app.appNumber
            itemView.setOnClickListener {
                onAppClick.onClick(app, adapterPosition)
            }
        }
    }
}
