package one.mixin.android.ui.home.bot

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.manager.SupportRequestManagerFragment
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.FragmentBotManagerBinding
import one.mixin.android.event.BotEvent
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toast
import one.mixin.android.job.TipCounterSyncedLiveData
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.showUserBottom
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.url.UrlInterpreterActivity
import one.mixin.android.util.rxpermission.RxPermissions
import one.mixin.android.vo.App
import one.mixin.android.vo.BotInterface
import one.mixin.android.widget.bot.BotDock
import javax.inject.Inject

@AndroidEntryPoint
class BotManagerFragment : BaseFragment(), BotDock.OnDockListener {
    companion object {
        const val TAG = "BorManagerBottomSheetDialogFragment"

        fun newInstance() = BotManagerFragment()
    }

    private val botManagerViewModel by viewModels<BotManagerViewModel>()

    @Inject
    lateinit var tipCounterSynced: TipCounterSyncedLiveData

    private var _binding: FragmentBotManagerBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentBotManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        loadData()
        RxBus.listen(BotEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(destroyScope)
            .subscribe {
                loadData()
            }
    }

    override fun onDetach() {
        super.onDetach()
        // UrlInterpreterActivity doesn't have a UI and needs it's son fragment to handle it's finish.
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
        binding.botRv.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.botRv.adapter = bottomListAdapter
        binding.botDock.setOnDockListener(this)
    }

    private fun loadData() {
        lifecycleScope.launch {
            val topApps = mutableListOf<BotInterface>(InternalCamera, InternalScan, InternalSupport)
            val topIds = mutableListOf(INTERNAL_CAMERA_ID, INTERNAL_SCAN_ID, INTERNAL_SUPPORT_ID)

            binding.botDock.apps = topApps
            val notTopApps = botManagerViewModel.getNotTopApps(topIds)
            if (notTopApps.isEmpty()) {
                binding.emptyFl.isVisible = true
                binding.botRv.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
            } else {
                binding.emptyFl.isVisible = false
            }
            bottomListAdapter.list = notTopApps
        }
    }

    private val bottomListAdapter by lazy {
        BotManagerAdapter(clickAction)
    }

    override fun onDockClick(app: BotInterface) {
        clickAction(app)
    }

    private val clickAction: (BotInterface) -> Unit = { app ->
        if (app is App) {
            lifecycleScope.launch {
                botManagerViewModel.findUserByAppId(app.appId)?.let { user ->
                    showUserBottom(parentFragmentManager, user)
                }
            }
        } else if (app is Bot) {
            when (app.id) {
                INTERNAL_CAMERA_ID -> {
                    openCamera(false)
                }

                INTERNAL_SCAN_ID -> {
                    openCamera(true)
                }

                INTERNAL_SUPPORT_ID -> {
                    lifecycleScope.launch {
                        val userTeamMixin = botManagerViewModel.refreshUser(Constants.TEAM_MIXIN_USER_ID)
                        if (userTeamMixin == null) {
                            toast(R.string.Data_error)
                        } else {
                            ConversationActivity.show(requireContext(), recipientId = Constants.TEAM_MIXIN_USER_ID)
                        }
                    }
                }
            }
        }
    }

    private fun openCamera(scan: Boolean) {
        RxPermissions(requireActivity())
            .request(Manifest.permission.CAMERA)
            .autoDispose(stopScope)
            .subscribe { granted ->
                if (granted) {
                    (requireActivity() as? MainActivity)?.showCapture(scan)
                } else {
                    context?.openPermissionSetting()
                }
            }
    }
}
