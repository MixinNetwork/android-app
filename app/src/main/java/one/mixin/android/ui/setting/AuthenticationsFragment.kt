package one.mixin.android.ui.setting

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_authentications.*
import kotlinx.android.synthetic.main.item_auth.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.loadImage
import one.mixin.android.ui.address.adapter.ItemCallback
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.vo.App
import javax.inject.Inject

class AuthenticationsFragment : BaseFragment() {
    companion object {
        const val TAG = "AuthenticationsFragment"

        fun newInstance() = AuthenticationsFragment()
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val settingViewModel: SettingViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(SettingViewModel::class.java)
    }

    private var list: List<App>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_authentications, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        val adapter = AuthenticationAdapter()
        settingViewModel.apps.observe(this, Observer { list ->
            this.list = list
            adapter.submitList(list)
        })
        ItemTouchHelper(ItemCallback(object : ItemCallback.ItemCallbackListener {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder) {
                val item = list!![viewHolder.adapterPosition]
                settingViewModel.deauthApp(item.appId)
            }
        })).apply { attachToRecyclerView(auth_rv) }
        auth_rv.adapter = adapter
    }

    class AuthenticationAdapter : ListAdapter<App, ItemHolder>(App.DIFF_CALLBACK) {
        override fun onBindViewHolder(itemHolder: ItemHolder, pos: Int) {
            itemHolder.bindTo(getItem(pos))
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder =
            ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_auth, parent, false))
    }

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindTo(app: App) {
            itemView.avatar.loadImage(app.icon_url)
            itemView.name_tv.text = app.name
        }
    }
}