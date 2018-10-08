package one.mixin.android.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import android.view.View.VISIBLE
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_camera.*
import one.mixin.android.R
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.widget.media.CameraView
import java.io.FileOutputStream

class CameraActivity : AppCompatActivity(), CameraView.CameraViewListener {

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        RxPermissions(this)
            .request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .subscribe { granted ->
                if (granted) {
                    if (camera_view.isMultiCamera) {
                        swap_camera_button.visibility = VISIBLE
                        swap_camera_button.setImageResource(if (camera_view.isRearCamera)
                            R.drawable.quick_camera_front
                        else R.drawable.quick_camera_rear)
                        swap_camera_button.setOnClickListener {
                            camera_view.flipCamera()
                            swap_camera_button.setImageResource(if (camera_view.isRearCamera)
                                R.drawable.quick_camera_front
                            else R.drawable.quick_camera_rear)
                        }
                    }
                    shutter_button.setOnClickListener {
                        camera_view.takePicture(Rect(0, 0, camera_view.measuredWidth, camera_view.measuredHeight))
                    }
                    camera_view.addListener(this)
                } else {
                    openPermissionSetting()
                }
            }
    }

    override fun onResume() {
        super.onResume()
        camera_view.onResume()
    }

    override fun onPause() {
        super.onPause()
        camera_view.onPause()
    }

    override fun onCameraFail() {
    }

    override fun onCameraStart() {
    }

    override fun onCameraStop() {
    }

    override fun onImageCapture(imageBytes: ByteArray) {
        val outFile = this.getImagePath().createImageTemp()
        val out = FileOutputStream(outFile)
        out.write(imageBytes)
        out.close()
        val intent = Intent()
        intent.data = Uri.fromFile(outFile)
        setResult(Activity.RESULT_OK, intent)
        camera_view.onPause()
        finish()
    }

    companion object {
        private const val REQUEST_CODE = 0x01
        fun show(context: Activity) {
            val intent = Intent(context, CameraActivity::class.java)
            context.startActivity(intent)
        }

        fun show(fragment: Fragment) {
            fragment.startActivityForResult(Intent(fragment.context, CameraActivity::class.java), REQUEST_CODE)
        }
    }
}
