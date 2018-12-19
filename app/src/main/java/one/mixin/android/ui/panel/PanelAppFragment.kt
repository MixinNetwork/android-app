package one.mixin.android.ui.panel

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_panel_app.*
import one.mixin.android.Constants.Mixin_Conversation_ID_HEADER
import one.mixin.android.R
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.conversation.web.WebBottomSheetDialogFragment
import one.mixin.android.ui.conversation.web.WebBottomSheetDialogFragment.Companion.CONVERSATION_ID
import one.mixin.android.ui.conversation.web.WebBottomSheetDialogFragment.Companion.URL

class PanelAppFragment : Fragment() {
    private val conversationId: String? by lazy {
        arguments!!.getString(CONVERSATION_ID)
    }
    private lateinit var url: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_panel_app, container, false)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        web_view.settings.javaScriptEnabled = true
        web_view.settings.domStorageEnabled = true
        web_view.addJavascriptInterface(WebBottomSheetDialogFragment.WebAppInterface(context!!, conversationId), "MixinContext")

        url = arguments!!.getString(URL)!!
        load(url)
    }

    fun load(url: String) {
        val extraHeaders = HashMap<String, String>()
        conversationId?.let {
            extraHeaders[Mixin_Conversation_ID_HEADER] = it
        }
        web_view.loadUrl(url, extraHeaders)
    }

    companion object {
        const val TAG = "PanelAppFragment"

        fun newInstance(url: String, conversationId: String?) = PanelAppFragment().withArgs {
            putString(URL, url)
            putString(CONVERSATION_ID, conversationId)
        }
    }
}