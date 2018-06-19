package one.mixin.android.ui.conversation

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import kotlinx.android.synthetic.main.fragment_add_sticker.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.R
import one.mixin.android.extension.loadImage
import one.mixin.android.ui.common.BaseFragment
import org.jetbrains.anko.textColor
import javax.inject.Inject

class StickerAddFragment: BaseFragment() {
    companion object {
        const val TAG = "StickerAddFragment"
        const val ARGS_URI = "args_uri"

        fun newInstance(uri: Uri) = StickerAddFragment().apply {
            arguments = bundleOf(ARGS_URI to uri)
        }
    }

    private val uri: Uri by lazy { arguments!!.getParcelable<Uri>(ARGS_URI) }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val stickerViewModel: ConversationViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ConversationViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_add_sticker, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.right_tv.textColor = Color.BLACK
        title_view.left_ib.setOnClickListener { activity?.onBackPressed() }
        title_view.right_animator.setOnClickListener {
            // TODO
        }
        sticker_iv.loadImage(uri)
    }
}