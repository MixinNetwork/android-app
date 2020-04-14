package one.mixin.android.ui.home.bot

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.fragment_bot_manager.view.*
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.widget.BottomSheet

class BotManagerBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {

    companion object {
        const val TAG = "BorManagerBottomSheetDialogFragment"
    }

    private val botManagerViewModel: BotManagerViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(BotManagerViewModel::class.java)
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_bot_manager, null)

        (dialog as BottomSheet).setCustomView(contentView)
        initView()
    }

    private fun initView() {
        contentView.bot_close.setOnClickListener {
            dismiss()
        }
        contentView.bot_dock.setOnDragListener(bottomListAdapter.dragInstance)
        contentView.bot_rv.layoutManager = GridLayoutManager(requireContext(), 4)
        contentView.bot_rv.adapter = bottomListAdapter
        contentView.bot_rv.setOnDragListener(bottomListAdapter.dragInstance)
        lifecycleScope.launch {
            bottomListAdapter.list = botManagerViewModel.getApps()
        }
    }

    private val bottomListAdapter by lazy {
        BotManagerAdapter()
    }
}
