package one.mixin.android.ui.setting

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.autoDisposable
import kotlinx.android.synthetic.main.fragment_authentications.*
import kotlinx.android.synthetic.main.item_auth.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_authentications, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        val adapter = AuthenticationAdapter(object : OnAppClick {
            override fun onClick(app: App, position: Int) {
                showDialog(app, position)
            }
        })
        viewModel.authorizations().autoDisposable(stopScope).subscribe({ list ->
            if (list.isSuccess) {
                this.list = list.data?.map {
                    it.app
                }?.run {
                    MutableList(this.size) {
                        this[it]
                    }
                }
                adapter.submitList(this.list)
            }
            progress.visibility = View.GONE
        }, {
            progress.visibility = View.GONE
            ErrorHandler.handleError(it)
        })
        auth_rv.adapter = adapter
    }

    private fun showDialog(app: App, position: Int) {
        AlertDialog.Builder(requireContext(), R.style.MixinAlertDialogTheme)
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setMessage(getString(R.string.setting_auth_cancel_msg, app.name))
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                viewModel.deauthApp(app.appId).autoDisposable(stopScope).subscribe({}, {})
                list?.removeAt(position)
                auth_rv.adapter?.notifyItemRemoved(position)
                dialog.dismiss()
            }.create().apply {
                setOnShowListener {
                    getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.RED)
                }
            }.show()
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
            itemView.avatar.setInfo(app.name, app.icon_url, app.appId)
            itemView.name_tv.text = app.name
            itemView.number_tv.text = app.appNumber
            itemView.setOnClickListener {
                onAppClick.onClick(app, adapterPosition)
            }
        }
    }
}