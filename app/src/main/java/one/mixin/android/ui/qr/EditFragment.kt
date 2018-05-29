package one.mixin.android.ui.qr

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.fragment_edit.*
import one.mixin.android.R
import one.mixin.android.extension.copy
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.hasNavigationBar
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File

class EditFragment : BaseFragment() {

    companion object {
        const val TAG = "EditFragment"
        const val ARGS_PATH = "args_path"

        fun newInstance(path: String): EditFragment {
            return EditFragment().withArgs {
                putString(ARGS_PATH, path)
            }
        }
    }

    private val path: String by lazy {
        arguments!!.getString(ARGS_PATH)
    }

    private var callback: Callback? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context !is Callback) {
            throw IllegalArgumentException("")
        }
        callback = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_edit, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        send_fl.post {
            val params = send_fl.layoutParams as RelativeLayout.LayoutParams
            val b = send_fl.bottom + params.bottomMargin
            val hasNavigationBar = context!!.hasNavigationBar(b)
            if (hasNavigationBar) {
                val navigationBarHeight = context!!.navigationBarHeight()
                send_fl.translationY = -navigationBarHeight.toFloat()
                download_iv.translationY = -navigationBarHeight.toFloat()
            }
        }
        close_iv.setOnClickListener { activity?.onBackPressed() }
        download_iv.setOnClickListener {
            RxPermissions(activity!!)
                .request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe { granted ->
                    if (granted) {
                        doAsync {
                            val outFile = context!!.getImagePath().createImageTemp()
                            File(path).copy(outFile)

                            uiThread { context?.toast(R.string.save_success) }
                        }
                    } else {
                        context?.openPermissionSetting()
                    }
                }
        }
        send_fl.setOnClickListener {
            ForwardActivity.show(context!!, arrayListOf(ForwardMessage(
                ForwardCategory.IMAGE.name, mediaUrl = path)), true)
        }

        preview_iv.setImageBitmap(callback?.getBitmap())
    }

    override fun onBackPressed(): Boolean {
        callback?.resumeCapture()
        return super.onBackPressed()
    }

    interface Callback {
        fun getBitmap(): Bitmap?
        fun resumeCapture()
    }
}