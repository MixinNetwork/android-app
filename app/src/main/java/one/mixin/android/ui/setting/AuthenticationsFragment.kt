package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.databinding.FragmentAuthenticationsBinding
import one.mixin.android.databinding.ItemAuthBinding
import one.mixin.android.extension.navTo
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.App

@AndroidEntryPoint
class AuthenticationsFragment : BaseFragment(R.layout.fragment_authentications) {
    companion object {
        const val TAG = "AuthenticationsFragment"

        fun newInstance() = AuthenticationsFragment()
    }

    private val viewModel by viewModels<SettingViewModel>()
    private val binding by viewBinding(FragmentAuthenticationsBinding::bind)

    private var list: MutableList<App>? = null
    private var authResponseList: MutableList<AuthorizationResponse>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener { activity?.onBackPressed() }
        val adapter = AuthenticationAdapter(
            object : OnAppClick {
                override fun onClick(app: App, position: Int) {
                    val auth = authResponseList?.get(position) ?: return
                    val fragment = PermissionListFragment.newInstance(app, auth)
                    fragment.deauthCallback = object : PermissionListFragment.DeauthCallback {
                        override fun onSuccess() {
                            list?.removeAt(position)
                            authResponseList?.removeAt(position)
                            binding.authRv.adapter?.notifyItemRemoved(position)
                        }
                    }
                    navTo(fragment, PermissionListFragment.TAG)
                }
            }
        )
        binding.apply {
            viewModel.authorizations().autoDispose(stopScope).subscribe(
                { list ->
                    if (list.isSuccess) {
                        this@AuthenticationsFragment.list = list.data?.map {
                            it.app
                        }?.run {
                            MutableList(this.size) {
                                this[it]
                            }
                        }
                        if (this@AuthenticationsFragment.list?.isNotEmpty() == true) {
                            authVa.displayedChild = 0
                        } else {
                            authVa.displayedChild = 1
                        }
                        adapter.submitList(this@AuthenticationsFragment.list)

                        authResponseList = list.data?.toMutableList()
                    } else {
                        authVa.displayedChild = 1
                    }
                    progress.visibility = View.GONE
                },
                {
                    progress.visibility = View.GONE
                    authVa.displayedChild = 1
                    ErrorHandler.handleError(it)
                }
            )
            authRv.adapter = adapter
        }
    }

    class AuthenticationAdapter(private val onAppClick: OnAppClick) : ListAdapter<App, ItemHolder>(App.DIFF_CALLBACK) {
        override fun onBindViewHolder(itemHolder: ItemHolder, pos: Int) {
            itemHolder.bindTo(getItem(pos), onAppClick)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder =
            ItemHolder(ItemAuthBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    interface OnAppClick {
        fun onClick(app: App, position: Int)
    }

    class ItemHolder(private val itemBinding: ItemAuthBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bindTo(app: App, onAppClick: OnAppClick) {
            itemBinding.apply {
                avatar.setInfo(app.name, app.iconUrl, app.appId)
                nameTv.text = app.name
                numberTv.text = app.appNumber
            }
            itemView.setOnClickListener {
                onAppClick.onClick(app, absoluteAdapterPosition)
            }
        }
    }
}
