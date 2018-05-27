package one.mixin.android.ui.qr

import android.annotation.SuppressLint
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.facebook.rebound.SimpleSpringListener
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringConfig.fromOrigamiTensionAndFriction
import com.facebook.rebound.SpringSystem
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.CaptureManagerCallback
import com.journeyapps.barcodescanner.Size
import com.journeyapps.barcodescanner.SourceData
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_capture.*
import kotlinx.android.synthetic.main.view_custom_barcode_scannner.*
import one.mixin.android.R
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.api.response.PaymentStatus
import one.mixin.android.extension.closeSilently
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.getImageCachePath
import one.mixin.android.extension.hasNavigationBar
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.isUUID
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.rotate
import one.mixin.android.extension.toBytes
import one.mixin.android.extension.vibrate
import one.mixin.android.extension.withArgs
import one.mixin.android.extension.xYuv2Simple
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.url.isMixinUrl
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.vo.User
import one.mixin.android.widget.CameraOpView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.UI
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.yesButton
import java.io.FileOutputStream
import javax.inject.Inject

class CaptureFragment : BaseFragment() {
    companion object {
        const val TAG = "CaptureFragment"

        const val ARGS_FOR_ADDRESS = "args_for_address"
        const val ARGS_ADDRESS_RESULT = "args_address_result"
        const val RESULT_CODE = 0x0000c0df

        val SCOPES = arrayListOf("PROFILE:READ", "PHONE:READ", "ASSETS:READ", "APPS:READ", "APPS:WRITE", "CONTACTS:READ")

        fun newInstance(forAddress: Boolean = false) = CaptureFragment().withArgs {
            putBoolean(ARGS_FOR_ADDRESS, forAddress)
        }
    }

    private enum class Mode {
        SCAN,
        CAPTURE,
        RECORD
    }

    private val mCaptureManager: CaptureManager by lazy {
        CaptureManager(activity, zxing_barcode_scanner, captureCallback)
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val captureViewModel: CaptureViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(CaptureViewModel::class.java)
    }

    private val springSystem = SpringSystem.create()
    private val sprintConfig = fromOrigamiTensionAndFriction(80.0, 4.0)

    private val forAddress: Boolean by lazy { arguments!!.getBoolean(ARGS_FOR_ADDRESS) }

    private var mode = Mode.SCAN
    private var flashOpen = false

    private var callback: Callback? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context !is Callback) {
            throw IllegalArgumentException("")
        }
        callback = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_capture, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        op.post {
            val b = op.bottom
            val hasNavigationBar = context!!.hasNavigationBar(b)
            if (hasNavigationBar) {
                val navigationBarHeight = context!!.navigationBarHeight()
                op.translationY = -navigationBarHeight.toFloat()
            }
        }
        close.setOnClickListener { activity?.onBackPressed() }
        mCaptureManager.initializeFromIntent(activity!!.intent, savedInstanceState)
        mCaptureManager.decode()
        flash.setOnClickListener {
            if (flashOpen) {
                flash.setImageResource(R.drawable.ic_flash_off)
                zxing_barcode_scanner.setTorchOff()
            } else {
                flash.setImageResource(R.drawable.ic_flash_on)
                zxing_barcode_scanner.setTorchOn()
            }

            anim(flash)
            flashOpen = !flashOpen
        }
        close.setOnClickListener { activity?.onBackPressed() }
        switch_camera.setOnClickListener {
            anim(switch_camera)
            zxing_barcode_scanner.switchCamera()
        }
        op.setCameraOpCallback(object : CameraOpView.CameraOpCallback {
            override fun onClick() {
                mCaptureManager.capture()
                mode = Mode.CAPTURE
            }

            override fun onProgressStart() {
                context!!.vibrate(longArrayOf(0, 30))
                mCaptureManager.record()
                mode = Mode.RECORD
            }

            override fun onProgressStop() {
                mCaptureManager.pause()
                mode = Mode.SCAN
            }
        })

        val p = Point()
        activity?.windowManager?.defaultDisplay?.getSize(p)
        zxing_barcode_surface.framingRectSize = Size(p.x, p.y)
        pb.indeterminateDrawable.setColorFilter(ContextCompat.getColor(context!!, android.R.color.white),
            PorterDuff.Mode.SRC_IN)
    }

    private fun anim(view: View) {
        val spring = springSystem.createSpring()
            .setSpringConfig(sprintConfig)
            .addListener(object : SimpleSpringListener() {
                override fun onSpringUpdate(spring: Spring) {
                    val value = spring.currentValue.toFloat()
                    view.scaleX = value
                    view.scaleY = value
                }
            })
        spring.endValue = 1.0
    }

    override fun onResume() {
        super.onResume()
        mCaptureManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        mCaptureManager.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mCaptureManager.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mCaptureManager.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        mCaptureManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun resume() {
        mCaptureManager.resume()
    }

    @SuppressLint("CheckResult")
    fun handleScanResult(data: String) {
        if (forAddress) {
            val result = Intent().apply {
                putExtra(ARGS_ADDRESS_RESULT, data)
            }
            activity?.setResult(RESULT_CODE, result)
            activity?.finish()
            return
        }

        if (!isMixinUrl(data)) {
            MainActivity.showScan(context!!, data)
            mCaptureManager.closeAndFinish()
            return
        }

        if (data.startsWith("https://mixin.one/pay", true)) {
            pb?.visibility = VISIBLE
            val uri = Uri.parse(data)
            val userId = uri.getQueryParameter("recipient")
            val assetId = uri.getQueryParameter("asset")
            val amount = uri.getQueryParameter("amount")
            val trace = uri.getQueryParameter("trace")
            val memo = uri.getQueryParameter("memo")
            val transferRequest = TransferRequest(assetId, userId, amount, null, trace, memo)
            captureViewModel.pay(transferRequest).autoDisposable(scopeProvider).subscribe({ r ->
                pb?.visibility = GONE
                if (r.isSuccess) {
                    val paymentResponse = r.data!!
                    captureViewModel.saveAsset(paymentResponse.asset)
                    captureViewModel.saveUser(paymentResponse.recipient)
                    if (paymentResponse.status == PaymentStatus.paid.name) {
                        toast(R.string.pay_paid)
                    } else {
                        MainActivity.showPay(context!!, paymentResponse.recipient, amount, paymentResponse.asset, trace, memo)
                    }
                    mCaptureManager.closeAndFinish()
                } else {
                    mCaptureManager.resume()
                    ErrorHandler.handleMixinError(r.errorCode)
                }
            }, {
                pb?.visibility = GONE
                mCaptureManager.resume()
                ErrorHandler.handleError(it)
            })
            return
        } else if (data.startsWith("mixin://transfer/", true)) {
            val segments = Uri.parse(data).pathSegments
            val userId = segments[0]
            doAsync {
                val user = captureViewModel.queryUser(userId)
                // TODO get user from server
                UI {
                    user.let {
                        MainActivity.showTransfer(requireContext(), it!!)
                        mCaptureManager.closeAndFinish()
                    }
                }
            }
        } else {
            val code = if (data.startsWith("https://mixin.one/codes/", true)) {
                val segments = Uri.parse(data).pathSegments
                if (segments.size >= 2) {
                    segments[1]
                } else {
                    return
                }
            } else {
                data
            }
            if (!code.isUUID()) {
                mCaptureManager.resume()
                return
            }
            pb.visibility = View.VISIBLE
            captureViewModel.searchCode(code).observeOn(AndroidSchedulers.mainThread())
                .autoDisposable(scopeProvider).subscribe({ result ->
                    when {
                        result.first == "user" -> {
                            pb?.visibility = View.GONE
                            val account = Session.getAccount()
                            if (account != null && account.userId == (result.second as User).userId) {
                                toast("It's your QR Code, please try another.")
                                mCaptureManager.resume()
                                return@subscribe
                            }
                            MainActivity.showUser(context!!, result.second as User)
                            mCaptureManager.closeAndFinish()
                        }
                        result.first == "authorization" -> {
                            MainActivity.showGroup(context!!, data)
                            pb?.visibility = View.GONE
                            mCaptureManager.onPause()
                        }
                        result.first == "conversation" -> context?.let {
                            MainActivity.showGroup(it, data)
                            mCaptureManager.closeAndFinish()
                        }
                        else -> warning()
                    }
                }, {
                    warning()
                    ErrorHandler.handleError(it)
                })
        }
    }

    private fun warning() {
        if (isAdded) {
            pb?.visibility = View.GONE
            alert(getString(R.string.can_not_recognize)) {
                yesButton { dialog ->
                    mCaptureManager.resume()
                    dialog.dismiss()
                }
                onCancelled { mCaptureManager.resume() }
            }.show()
        }
    }

    private fun handleCapture(sourceData: SourceData) {
        val imageBytes = sourceData.data.xYuv2Simple(sourceData.dataWidth, sourceData.dataHeight)
        val rawBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val bitmap = rawBitmap.rotate(sourceData.dataWidth, sourceData.dataHeight,
            sourceData.rotation, zxing_barcode_scanner.isFacingBack)
        val outFile = context!!.getImageCachePath().createImageTemp()
        doAsync {
            val out = FileOutputStream(outFile)
            out.write(bitmap.toBytes())
            out.closeSilently()
        }

        callback?.setBitmap(bitmap)
        activity?.supportFragmentManager?.inTransaction {
            add(R.id.container, EditFragment.newInstance(outFile.absolutePath), EditFragment.TAG)
                .addToBackStack(null)
        }
    }

    private val captureCallback = object : CaptureManagerCallback {
        override fun onScanResult(result: BarcodeResult) {
            handleScanResult(result.text)
        }

        override fun onPreview(sourceData: SourceData) {
            if (mode == Mode.CAPTURE) {
                handleCapture(sourceData)
                mCaptureManager.pause()
            } else if (mode == Mode.RECORD) {
            }
        }
    }

    interface Callback {
        fun setBitmap(bitmap: Bitmap)
    }
}