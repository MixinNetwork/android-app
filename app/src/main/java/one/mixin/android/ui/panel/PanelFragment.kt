package one.mixin.android.ui.panel

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import one.mixin.android.R
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.conversation.ConversationFragment.Companion.CONVERSATION_ID
import one.mixin.android.ui.panel.listener.OnSendContactsListener
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.widget.gallery.Gallery
import one.mixin.android.widget.gallery.MimeType
import one.mixin.android.widget.gallery.engine.impl.GlideEngine
import one.mixin.android.widget.gallery.internal.entity.CaptureStrategy
import one.mixin.android.widget.gallery.ui.GalleryFragment

class PanelFragment : Fragment() {

    private val conversationId by lazy { arguments!!.getString(CONVERSATION_ID) }

    private var currFragment: Fragment? = null

    private val panelTabHeight by lazy {
        requireContext().resources.getDimensionPixelOffset(R.dimen.panel_tab_height)
    }

    var keyboardHeight = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_panel, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        showGalleryFragment()
    }

    fun isExpanded() = if (currFragment != null && currFragment is PanelBarFragment) {
        (currFragment as PanelBarFragment).expanded
    } else {
        false
    }

    fun expand() {
        if (currFragment != null && currFragment is PanelBarFragment) {
            (currFragment as PanelBarFragment).expand()
        }
    }

    fun collapse() {
        if (currFragment != null && currFragment is PanelBarFragment) {
            (currFragment as PanelBarFragment).collapse()
        }
    }

    fun showGalleryFragment() {
        Gallery.from(this@PanelFragment)
            .choose(MimeType.ofMedia())
            .imageEngine(GlideEngine())
            .capture(true)
            .captureStrategy(CaptureStrategy(true, "one.mixin.messenger.provider"))
        var galleryFragment = requireFragmentManager().findFragmentByTag(GalleryFragment.TAG) as? GalleryFragment
        if (galleryFragment == null) {
            galleryFragment = GalleryFragment.newInstance()
            galleryFragment.onGalleryFragmentCallback = onGalleryFragment
        }
        galleryFragment.minHeight = keyboardHeight - panelTabHeight
        requireFragmentManager().inTransaction {
            replace(R.id.panel_container, galleryFragment, GalleryFragment.TAG)
        }
        currFragment = galleryFragment
    }

    fun showTransferFragment() {
        callback?.onTransferClick()
    }

    fun showVoiceFragment() {
        var voiceFragment = requireFragmentManager().findFragmentByTag(PanelVoiceFragment.TAG) as? PanelVoiceFragment
        if (voiceFragment == null) {
            voiceFragment = PanelVoiceFragment.newInstance()
            voiceFragment.onVoiceCallback = onVoiceFragment
        }
        voiceFragment.setHeight(keyboardHeight - panelTabHeight)
        requireFragmentManager().inTransaction {
            replace(R.id.panel_container, voiceFragment, PanelVoiceFragment.TAG)
        }
        currFragment = voiceFragment
    }

    fun showFileFragment() {
        callback?.onFileClick()
    }

    fun showContactFragment() {
        var contactFragment = requireFragmentManager().findFragmentByTag(PanelContactFragment.TAG) as? PanelContactFragment
        if (contactFragment == null) {
            contactFragment = PanelContactFragment.newInstance()
            contactFragment.onSendContactsListener = object : OnSendContactsListener {
                override fun onSendContacts(message: ForwardMessage) {
                    callback?.onSendContact(message)
                }
            }
        }
        contactFragment.minHeight = keyboardHeight - panelTabHeight
        requireFragmentManager().inTransaction {
            replace(R.id.panel_container, contactFragment, PanelContactFragment.TAG)
        }
        currFragment = contactFragment
    }

    fun showAppFragment(panelTab: PanelTab) {
        var appFragment = requireFragmentManager().findFragmentByTag(PanelAppFragment.TAG) as? PanelAppFragment
        if (appFragment == null) {
            appFragment = PanelAppFragment.newInstance(panelTab.homeUri!!, conversationId)
        } else {
            appFragment.load(panelTab.homeUri!!)
        }
        appFragment.minHeight = keyboardHeight - panelTabHeight
        requireFragmentManager().inTransaction {
            replace(R.id.panel_container, appFragment, PanelAppFragment.TAG)
        }
        currFragment = appFragment
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
        fun onGalleryClick(uri: Uri, isVideo: Boolean)
        fun onCameraClick(imageUri: Uri)
        fun onVoiceClick()
        fun onTransferClick()
        fun onFileClick()
        fun onSendContact(message: ForwardMessage)
    }

    companion object {
        const val TAG = "PanelFragment"

        fun newInstance(
            conversationId: String
        ) = PanelFragment().withArgs {
            putString(CONVERSATION_ID, conversationId)
        }
    }
}