package one.mixin.android.ui.home.bot

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.manager.SupportRequestManagerFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.uber.autodispose.android.lifecycle.scope
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_bot_manager.*
import kotlinx.android.synthetic.main.fragment_bot_manager.view.*
import kotlinx.android.synthetic.main.fragment_bot_manager.view.bot_dock
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.di.Injectable
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.putString
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.SystemUIManager
import one.mixin.android.vo.App
import one.mixin.android.widget.MixinBottomSheetDialog
import one.mixin.android.widget.bot.BotDock

class BotManagerBottomSheetDialogFragment : BottomSheetDialogFragment(), BotDock.OnDockListener, Injectable {

    companion object {
        const val TAG = "BorManagerBottomSheetDialogFragment"
    }

    private lateinit var contentView: View

    private val stopScope = scope(Lifecycle.Event.ON_STOP)

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val botManagerViewModel: BotManagerViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(BotManagerViewModel::class.java)
    }

    override fun getTheme() = R.style.MixinBottomSheet

    private var behavior: BottomSheetBehavior<*>? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MixinBottomSheetDialog(requireContext(), theme).apply {
            dismissWithAnimation = true
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_bot_manager, null)
        dialog.setContentView(contentView)
        val params = (contentView.parent as View).layoutParams as CoordinatorLayout.LayoutParams
        behavior = params.behavior as? BottomSheetBehavior<*>
        if (behavior != null && behavior is BottomSheetBehavior<*>) {
            val defaultPeekHeight = getPeekHeight(contentView, behavior!!)
            behavior?.peekHeight = if (defaultPeekHeight == 0) {
                500.dp
            } else defaultPeekHeight
            behavior?.addBottomSheetCallback(bottomSheetBehaviorCallback)
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            dialog.window?.setGravity(Gravity.BOTTOM)
        }
        initView()
        loadData()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().booleanFromAttribute(R.attr.flag_night)
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        if (activity is UrlInterpreterActivity) {
            var realFragmentCount = 0
            parentFragmentManager.fragments.forEach { f ->
                if (f !is SupportRequestManagerFragment) {
                    realFragmentCount++
                }
            }
            if (realFragmentCount <= 0) {
                activity?.finish()
            }
        }
    }

    private fun initView() {
        contentView.bot_close.setOnClickListener {
            dismiss()
        }
        contentView.bot_dock.setOnDragListener(bottomListAdapter.dragInstance)
        contentView.bot_rv.layoutManager = GridLayoutManager(requireContext(), 4)
        contentView.bot_rv.adapter = bottomListAdapter
        contentView.bot_rv.setOnDragListener(bottomListAdapter.dragInstance)
        contentView.bot_dock.setOnDockListener(this)
    }

    private fun loadData() {
        lifecycleScope.launch {
            val defaultApps = mutableListOf<BotInterface>(InternalWallet, InternalCamera, InternalScan)
            val topApps = mutableListOf<BotInterface>()
            val topIds = mutableListOf<String>()
            defaultSharedPreferences.getString(TOP_BOT, null)?.let {
                val ids = GsonHelper.customGson.fromJson(it, Array<String>::class.java)
                ids.forEach { id ->
                    topIds.add(id)
                    when (id) {
                        VALUE_WALLET -> {
                            topApps.add(InternalWallet)
                            defaultApps.remove(InternalWallet)
                        }
                        VALUE_CAMERA -> {
                            topApps.add(InternalCamera)
                            defaultApps.remove(InternalCamera)
                        }
                        VALUE_SCAN -> {
                            topApps.add(InternalScan)
                            defaultApps.remove(InternalScan)
                        }
                        else -> {
                            botManagerViewModel.findAppById(id)?.let { app ->
                                topApps.add(Bot(app))
                            }
                        }
                    }
                }
            }

            contentView.bot_dock.apps = topApps
            defaultApps.addAll(botManagerViewModel.getTopApps(topIds))
            bottomListAdapter.list = defaultApps
        }
    }

    private val bottomListAdapter by lazy {
        BotManagerAdapter()
    }

    fun getPeekHeight(contentView: View, behavior: BottomSheetBehavior<*>): Int = 0

    fun onStateChanged(bottomSheet: View, newState: Int) {}

    fun onSlide(bottomSheet: View, slideOffset: Float) {}

    private val bottomSheetBehaviorCallback = object : BottomSheetBehavior.BottomSheetCallback() {

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            this@BotManagerBottomSheetDialogFragment.onStateChanged(bottomSheet, newState)
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            this@BotManagerBottomSheetDialogFragment.onSlide(bottomSheet, slideOffset)
        }
    }

    override fun onDockChange(apps: List<BotInterface>) {
        saveTopApps(apps)
        loadData()
    }

    private fun saveTopApps(apps: List<BotInterface>) {
        apps.map {
            if (it is App) {
                it.appId
            } else {
                (it as Bot).id
            }
        }.apply {
            defaultSharedPreferences.putString(TOP_BOT, GsonHelper.customGson.toJson(this))
        }
    }
}
