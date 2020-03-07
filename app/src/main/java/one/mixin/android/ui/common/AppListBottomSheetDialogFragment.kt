package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_app_list_bottom_sheet.view.*
import kotlinx.android.synthetic.main.item_app_list.view.*
import kotlinx.android.synthetic.main.view_round_title.view.*
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.inflate
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.withArgs
import one.mixin.android.vo.App
import one.mixin.android.widget.BottomSheet

class AppListBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "AppListBottomSheetDialogFragment"
        const val ARGS_APP_LIST = "args_app_list"
        const val ARGS_TITLE = "args_title"

        fun newInstance(
            appList: List<App>,
            title: String
        ) = AppListBottomSheetDialogFragment().withArgs {
            putParcelableArrayList(ARGS_APP_LIST, ArrayList(appList))
            putString(ARGS_TITLE, title)
        }
    }

    private val appList by lazy {
        arguments!!.getParcelableArrayList<App>(ARGS_APP_LIST)
    }
    private val title: String by lazy {
        arguments!!.getString(ARGS_TITLE)!!
    }

    private val adapter = AppListAdapter {
        openApp(it)
    }

    private fun openApp(userId: String) {
        lifecycleScope.launch {
            bottomViewModel.suspendFindUserById(userId)?.let { user ->
                UserBottomSheetDialogFragment.newInstance(user)
                    .showNow(parentFragmentManager, UserBottomSheetDialogFragment.TAG)
                dismiss()
            }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_app_list_bottom_sheet, null)
        (dialog as BottomSheet).setCustomView(contentView)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.title_view.right_iv.setOnClickListener { dismiss() }
        contentView.title_tv.text = title
        contentView.recycler_view.layoutManager = LinearLayoutManager(requireContext())
        contentView.recycler_view.adapter = adapter
        adapter.submitList(appList)
    }
}

class AppListAdapter(private val onClickListener: (String) -> Unit) :
    ListAdapter<App, AppHolder>(App.DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        AppHolder(parent.inflate(R.layout.item_app_list, false))

    override fun onBindViewHolder(holder: AppHolder, position: Int) {
        getItem(position)?.let { app ->
            holder.bind(app)
            holder.itemView.setOnClickListener {
                onClickListener(app.appId)
            }
        }
    }
}

class AppHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(app: App) {
        itemView.name_tv.text = app.name
        itemView.desc_tv.text = app.description
        itemView.avatar.loadImage(app.iconUrl)
    }
}
