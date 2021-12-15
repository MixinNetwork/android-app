package one.mixin.android.ui.setting

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.api.response.AuthorizationResponse
import one.mixin.android.databinding.FragmentAuthenticationsBinding
import one.mixin.android.databinding.ItemAuthBinding
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.equalsIgnoreCase
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.highLight
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

    private var keyWord: String = ""
        set(value) {
            field = value
            dataChange()
        }

    private val appCallback = AppItemCallback("")

    private val adapter = AuthenticationAdapter(
        appCallback,
        object : OnAppClick {
            override fun onClick(app: App) {
                val auth = authResponseList?.find { it.app.appId == app.appId } ?: return
                val fragment = PermissionListFragment.newInstance(app, auth)
                fragment.deauthCallback = object : PermissionListFragment.DeauthCallback {
                    override fun onSuccess() {
                        list?.removeIf { it.appId == app.appId }
                        authResponseList?.removeIf { it.app.appId == app.appId }
                        dataChange()
                    }
                }
                binding.searchEt.hideKeyboard()
                navTo(fragment, PermissionListFragment.TAG)
            }
        }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener { activity?.onBackPressed() }

        binding.apply {
            searchEt.addTextChangedListener(
                object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                    override fun afterTextChanged(s: Editable) {
                        keyWord = s.toString()
                        adapter.filter = keyWord
                        appCallback.filter = keyWord
                    }
                }
            )

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
                        dataChange()

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

    private fun dataChange() {
        adapter.submitList(
            if (keyWord.isNotBlank()) {
                list?.filter {
                    it.name.containsIgnoreCase(keyWord) || it.appNumber.containsIgnoreCase(keyWord)
                }?.sortedByDescending { it.name.equalsIgnoreCase(keyWord) || it.appNumber.equalsIgnoreCase(keyWord) }
            } else {
                list?.toList()
            }
        )
    }

    class AuthenticationAdapter(callback: AppItemCallback, private val onAppClick: OnAppClick) : ListAdapter<App, ItemHolder>(callback) {
        var filter = ""

        override fun onBindViewHolder(itemHolder: ItemHolder, pos: Int) {
            itemHolder.bindTo(getItem(pos), filter, onAppClick)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder =
            ItemHolder(ItemAuthBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    interface OnAppClick {
        fun onClick(app: App)
    }

    class ItemHolder(private val itemBinding: ItemAuthBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bindTo(app: App, filter: String, onAppClick: OnAppClick) {
            itemBinding.apply {
                avatar.setInfo(app.name, app.iconUrl, app.appId)
                nameTv.text = app.name
                nameTv.highLight(filter)
                numberTv.text = app.appNumber
                numberTv.highLight(filter)
            }
            itemView.setOnClickListener {
                onAppClick.onClick(app)
            }
        }
    }

    class AppItemCallback(var filter: String) : DiffUtil.ItemCallback<App>() {
        override fun areItemsTheSame(oldItem: App, newItem: App) =
            (
                oldItem.name.contains(filter, true) == newItem.name.contains(filter, true) &&
                    oldItem.appNumber.contains(filter, true) == newItem.appNumber.contains(filter, true)
                )

        override fun areContentsTheSame(oldItem: App, newItem: App) = false
    }
}
