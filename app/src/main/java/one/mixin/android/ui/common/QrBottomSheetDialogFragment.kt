package one.mixin.android.ui.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.arch.lifecycle.Observer
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.core.os.bundleOf
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_qr_bottom_sheet.view.*
import kotlinx.android.synthetic.main.view_badge_avatar.view.*
import kotlinx.android.synthetic.main.view_qr_bottom.view.*
import kotlinx.android.synthetic.main.view_title.view.*
import one.mixin.android.Constants.ARGS_USER_ID
import one.mixin.android.Constants.MIXIN_TRANSFER_PREFIX
import one.mixin.android.Constants.MY_QR
import one.mixin.android.R
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.generateQRCode
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.getQRCodePath
import one.mixin.android.extension.isQRCodeFileExists
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.save
import one.mixin.android.extension.saveQRCode
import one.mixin.android.util.Session
import one.mixin.android.vo.User
import one.mixin.android.widget.BadgeCircleImageView.Companion.END_BOTTOM
import one.mixin.android.widget.BottomSheet
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.uiThread
import java.io.FileNotFoundException

class QrBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "QrBottomSheetDialogFragment"
        const val ARGS_TYPE = "args_type"

        const val TYPE_MY_QR = 0
        const val TYPE_RECEIVE_QR = 1

        fun newInstance(userId: String, type: Int) = QrBottomSheetDialogFragment().apply {
            arguments = bundleOf(
                ARGS_USER_ID to userId,
                ARGS_TYPE to type
            )
        }
    }

    private val userId: String by lazy { arguments!!.getString(ARGS_USER_ID) }
    private val type: Int by lazy { arguments!!.getInt(ARGS_TYPE) }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        contentView = View.inflate(context, R.layout.fragment_qr_bottom_sheet, null)
        (dialog as BottomSheet).setCustomView(contentView)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contentView.title.left_ib.setOnClickListener { dialog?.dismiss() }
        contentView.right_animator.setOnClickListener { showBottom() }
        if (type == TYPE_MY_QR) {
            contentView.title.title_tv.text = getString(R.string.contact_my_qr_title)
            contentView.tip_tv.text = getString(R.string.contact_my_qr_tip)
        } else if (type == TYPE_RECEIVE_QR) {
            contentView.title.title_tv.text = getString(R.string.contact_receive_money)
            contentView.tip_tv.text = getString(R.string.contact_receive_tip)
        }
        dialog?.dismiss()
        bottomViewModel.findUserById(userId).observe(this, Observer { user ->
            if (user == null) {
                bottomViewModel.refreshUser(userId)
            } else {
                val info = if (user.fullName != null && user.fullName.isNotEmpty()) user.fullName[0] else ' '
                contentView.badge_view.bg.setInfo(info, user.avatarUrl, user.identityNumber)
                if (type == TYPE_RECEIVE_QR) {
                    contentView.badge_view.badge.setImageResource(R.drawable.ic_contacts_receive_blue)
                    contentView.badge_view.pos = END_BOTTOM
                }

                val name = getName(user)
                if (context!!.isQRCodeFileExists(name)) {
                    contentView.qr.setImageBitmap(BitmapFactory.decodeFile(context!!.getQRCodePath(name).absolutePath))
                } else {
                    contentView.qr.post {
                        Observable.create<Bitmap> { e ->
                            val account = Session.getAccount() ?: return@create
                            val code = when (type) {
                                TYPE_MY_QR -> account.code_url
                                TYPE_RECEIVE_QR -> "$MIXIN_TRANSFER_PREFIX${user.userId}"
                                else -> ""
                            }
                            val b = code.generateQRCode(contentView.qr.width,
                                if (type == TYPE_MY_QR) context?.getColor(R.color.colorBlue) else null)
                            if (b != null) {
                                b.saveQRCode(context!!, name)
                                e.onNext(b)
                            }
                        }.subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .autoDisposable(scopeProvider)
                            .subscribe({ r ->
                                contentView.qr.setImageBitmap(r)
                            }, { _ ->
                            })
                    }
                }
            }
        })
    }

    private fun getName(user: User): String {
        return when (type) {
            TYPE_MY_QR -> MY_QR
            TYPE_RECEIVE_QR -> "$TYPE_RECEIVE_QR-${user.userId}"
            else -> ""
        }
    }

    private fun showBottom() {
        context?.let { ctx ->
            val builder = BottomSheet.Builder(ctx)
            val view = View.inflate(ctx, R.layout.view_qr_bottom, null)
            builder.setCustomView(view)
            val bottomSheet = builder.create()
            view.save.setOnClickListener {
                RxPermissions(activity!!)
                    .request(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .autoDisposable(scopeProvider)
                    .subscribe({ granted ->
                        if (granted) {
                            doAsync {
                                val outFile = ctx.getImagePath().createImageTemp()
                                val b = Bitmap.createBitmap(contentView.qr_fl.width, contentView.qr_fl.height, Bitmap.Config.ARGB_8888)
                                val c = Canvas(b)
                                contentView.qr_fl.draw(c)
                                b.save(outFile)
                                try {
                                    MediaStore.Images.Media.insertImage(ctx.contentResolver,
                                        outFile.absolutePath, outFile.name, null)
                                } catch (e: FileNotFoundException) {
                                    e.printStackTrace()
                                }
                                ctx.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(outFile)))

                                uiThread { toast(R.string.save_success) }
                            }
                        } else {
                            ctx.openPermissionSetting()
                        }
                    }, {
                        toast(R.string.save_failure)
                    })
                bottomSheet.dismiss()
            }
            view.cancel.setOnClickListener { bottomSheet.dismiss() }
            bottomSheet.show()
        }
    }
}