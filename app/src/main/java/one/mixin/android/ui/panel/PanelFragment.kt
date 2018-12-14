package one.mixin.android.ui.panel

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_panel.*
import one.mixin.android.R
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationViewModel
import one.mixin.android.ui.panel.adapter.PanelTabAdapter
import one.mixin.android.vo.App
import one.mixin.android.widget.gallery.Gallery
import one.mixin.android.widget.gallery.MimeType
import one.mixin.android.widget.gallery.engine.impl.GlideEngine
import one.mixin.android.widget.gallery.internal.entity.CaptureStrategy
import one.mixin.android.widget.gallery.ui.GalleryFragment
import javax.inject.Inject

class PanelFragment : BaseFragment() {
    companion object {
        const val TAG = "PanelFragment"

        const val ARGS_IS_GROUP = "is_group"
        const val ARGS_IS_BOT = "is_bot"
        const val ARGS_IS_SELF_CREATED_BOT = "is_self_created_bot"

        fun newInstance(
            isGroup: Boolean,
            isBot: Boolean,
            isSelfCreatedBot: Boolean
        ) = PanelFragment().withArgs {
            putBoolean(ARGS_IS_GROUP, isGroup)
            putBoolean(ARGS_IS_BOT, isBot)
            putBoolean(ARGS_IS_SELF_CREATED_BOT, isSelfCreatedBot)
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val chatViewModel: ConversationViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ConversationViewModel::class.java)
    }

    private val isGroup by lazy { arguments!!.getBoolean(ARGS_IS_GROUP) }
    private val isBot by lazy { arguments!!.getBoolean(ARGS_IS_BOT) }
    private val isSelfCreatedBot by lazy { arguments!!.getBoolean(ARGS_IS_SELF_CREATED_BOT) }

    private val panelTabAdapter by lazy { PanelTabAdapter(isGroup, isBot, isSelfCreatedBot) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_panel, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Gallery.from(this@PanelFragment)
            .choose(MimeType.ofMedia())
            .imageEngine(GlideEngine())
            .capture(true)
            .captureStrategy(CaptureStrategy(true, "one.mixin.messenger.provider"))
        panel_tab_rv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        panel_tab_rv.adapter = panelTabAdapter

        showGalleryFragment()
        callback?.toggleExpand(true)
        panelTabAdapter.onPanelTabListener = object : PanelTabAdapter.OnPanelTabListener {
            override fun onPanelTabClick(panelTab: PanelTab) {
                when (panelTab.type) {
                    PanelTabType.Gallery -> showGalleryFragment()
                    PanelTabType.Transfer -> showTransferFragment()
                    PanelTabType.Voice -> showVoiceFragment()
                    PanelTabType.File -> showFileFragment(panelTab)
                    PanelTabType.Contact -> showContactFragment()
                    PanelTabType.App -> showAppFragment(panelTab)
                }
                callback?.toggleExpand(panelTab.expandable)
            }
        }
    }

    private fun showGalleryFragment() {
        var galleryFragment = requireFragmentManager().findFragmentByTag(GalleryFragment.TAG) as? GalleryFragment
        if (galleryFragment == null) {
            galleryFragment = GalleryFragment.newInstance()
            galleryFragment.onGalleryFragmentCallback = onGalleryFragment
        }
        requireFragmentManager().inTransaction {
            replace(R.id.panel_tab_container, galleryFragment, GalleryFragment.TAG)
        }
    }

    private fun showTransferFragment() {
        callback?.onTransferClick()
    }

    private fun showVoiceFragment() {
        var voiceFragment = requireFragmentManager().findFragmentByTag(PanelVoiceFragment.TAG) as? PanelVoiceFragment
        if (voiceFragment == null) {
            voiceFragment = PanelVoiceFragment.newInstance()
            voiceFragment.onVoiceCallback = onVoiceFragment
        }
        requireFragmentManager().inTransaction {
            replace(R.id.panel_tab_container, voiceFragment, PanelVoiceFragment.TAG)
        }
    }

    private fun showFileFragment(panelTab: PanelTab) {
    }

    private fun showContactFragment() {
        var contactFragment = requireFragmentManager().findFragmentByTag(PanelContactFragment.TAG) as? PanelContactFragment
        if (contactFragment == null) {
            contactFragment = PanelContactFragment.newInstance()
        }
        requireFragmentManager().inTransaction {
            replace(R.id.panel_tab_container, contactFragment, PanelVoiceFragment.TAG)
        }
    }

    private fun showAppFragment(panelTab: PanelTab) {
    }

    fun setAppList(appList: List<App>) {
        panelTabAdapter.appList = appList
    }

    private val onGalleryFragment = object : GalleryFragment.OnGalleryFragmentCallback {
        override fun onGalleryClick(uri: Uri, isVideo: Boolean) {
            callback?.onGalleryClick(uri, isVideo)
        }

        override fun onCameraClick(imageUri: Uri) {
            callback?.onCameraClick(imageUri)
        }
    }

    private val onVoiceFragment = object : PanelVoiceFragment.OnVoiceCallback {
        override fun onVoiceClick() {
            callback?.onVoiceClick()
        }
    }

    var callback: Callback? = null

    interface Callback {
        fun toggleExpand(expandable: Boolean)
        fun onGalleryClick(uri: Uri, isVideo: Boolean)
        fun onCameraClick(imageUri: Uri)
        fun onVoiceClick()
        fun onTransferClick()
    }
}