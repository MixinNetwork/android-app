package one.mixin.android.extension

import android.app.Activity
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Point
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Browser
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.annotation.IdRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import one.mixin.android.BuildConfig
import one.mixin.android.R
import one.mixin.android.receiver.ShareBroadcastReceiver
import one.mixin.android.util.Attachment
import one.mixin.android.util.video.MediaController
import one.mixin.android.util.video.VideoEditedInfo
import one.mixin.android.widget.gallery.Gallery
import one.mixin.android.widget.gallery.MimeType
import one.mixin.android.widget.gallery.engine.impl.GlideEngine
import org.jetbrains.anko.displayMetrics
import timber.log.Timber
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

private val uiHandler = Handler(Looper.getMainLooper())

fun Context.mainThread(runnable: () -> Unit) {
    uiHandler.post(runnable)
}

fun Context.mainThreadDelayed(runnable: () -> Unit, delayMillis: Long) {
    uiHandler.postDelayed(runnable, delayMillis)
}

fun Context.runOnUIThread(runnable: Runnable, delay: Long = 0L) {
    if (delay == 0L) {
        uiHandler.post(runnable)
    } else {
        uiHandler.postDelayed(runnable, delay)
    }
}

fun Context.cancelRunOnUIThread(runnable: Runnable) {
    uiHandler.removeCallbacks(runnable)
}

fun Context.async(runnable: () -> Unit) {
    Thread(runnable).start()
}

fun Context.async(runnable: () -> Unit, executor: ExecutorService): Future<out Any?> =
    executor.submit(runnable)

fun Context.statusBarHeight(): Int {
    val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
        return resources.getDimensionPixelSize(resourceId)
    }
    return dpToPx(24f)
}

fun Context.navigationBarHeight(): Int {
    val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    if (resourceId > 0) {
        return resources.getDimensionPixelSize(resourceId)
    }
    return dpToPx(24f)
}

fun Context.hasNavBar(): Boolean {
    val id = resources.getIdentifier("config_showNavigationBar", "bool", "android")
    return id > 0 && resources.getBoolean(id)
}

@Suppress("DEPRECATION")
fun Context.vibrate(pattern: LongArray) {
    if (Build.VERSION.SDK_INT >= 26) {
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(VibrationEffect.createWaveform(pattern, -1))
    } else {
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(pattern, -1)
    }
}

fun Context.dpToPx(dp: Float): Int {
    return if (dp == 0f) {
        0
    } else {
        Math.ceil((this.resources.displayMetrics.density * dp).toDouble()).toInt()
    }
}

fun Context.spToPX(sp: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, this.resources.displayMetrics).toInt()

fun Context.getPixelsInCM(cm: Float, isX: Boolean): Float =
    cm / 2.54f * if (isX) displayMetrics.xdpi else displayMetrics.ydpi

fun Context.isTablet(): Boolean = resources.getBoolean(R.bool.isTablet)

fun Context.appCompatActionBarHeight(): Int {
    val tv = TypedValue()
    theme.resolveAttribute(R.attr.actionBarSize, tv, true)
    return resources.getDimensionPixelSize(tv.resourceId)
}

fun Context.networkConnected(): Boolean {
    val cm = getSystemService<ConnectivityManager>() ?: return false
    val network: NetworkInfo
    try {
        network = cm.activeNetworkInfo
    } catch (t: Throwable) {
        return false
    }
    return network != null && network.isConnected
}

fun Context.displaySize(): Point {
    val displaySize = Point()
    val manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val display = manager.defaultDisplay
    display.getSize(displaySize)
    return displaySize
}

fun Context.realSize(): Point {
    val displaySize = Point()
    val manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val display = manager.defaultDisplay
    display.getRealSize(displaySize)
    return displaySize
}

fun Context.displayHeight(): Int {
    return displaySize().y
}

fun Context.displayWitdh(): Int {
    return displaySize().x
}

fun Context.screenHeight(): Int {
    return screenSize().y
}

fun Context.screenWidth(): Int {
    return screenSize().x
}

fun Context.screenSize(): Point {
    val size = Point()
    val manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    manager.defaultDisplay.getRealSize(size)
    return size
}

fun Context.displayRatio(): Float {
    val size = displaySize()
    return size.y.toFloat() / size.x
}

fun Context.getUriForFile(file: File): Uri {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val authority = String.format("%s.provider", this.packageName)
        FileProvider.getUriForFile(this, authority, file)
    } else {
        Uri.fromFile(file)
    }
}

fun Context.hasNavigationBar(bottom: Int = 0): Boolean {
    // TRICK  Maybe not correct
    if (bottom > displaySize().y) {
        return true
    }

    if (Build.MANUFACTURER == "smartisan" && Build.MODEL == "OS105") {
        return true
    }
    val hasMenuKey = ViewConfiguration.get(this).hasPermanentMenuKey()
    val hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)
    return !hasMenuKey && !hasBackKey
}

private var maxItemWidth: Int? = null

fun Context.maxItemWidth(): Int {
    if (maxItemWidth == null) {
        maxItemWidth = displaySize().x - dpToPx(66f)
    }
    return maxItemWidth!!
}

// fragment
fun FragmentActivity.replaceFragment(fragment: Fragment, frameId: Int) {
    supportFragmentManager.inTransaction { replace(frameId, fragment) }
}

fun FragmentActivity.replaceFragment(fragment: Fragment, frameId: Int, tag: String) {
    supportFragmentManager.inTransaction { replace(frameId, fragment, tag) }
}

fun FragmentActivity.addFragment(from: Fragment, to: Fragment, tag: String) {
    val fm = supportFragmentManager
    fm?.let {
        val ft = it.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, 0, 0, R.anim.slide_out_right)
        if (to.isAdded) {
            ft.show(to)
        } else {
            ft.add(R.id.container, to, tag)
        }
        ft.addToBackStack(null)
        ft.commitAllowingStateLoss()
    }
}

inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> Unit) {
    val fragmentTransaction = beginTransaction()
    fragmentTransaction.func()
    fragmentTransaction.commitAllowingStateLoss()
}

fun Fragment.bottomShowFragment(fragment: Fragment, @IdRes id: Int, tag: String) {
    val fm = fragmentManager
    fm?.let {
        val ft = it.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_bottom, 0, 0, R.anim.slide_out_bottom)
        if (fragment.isAdded) {
            ft.show(fragment)
        } else {
            ft.add(id, fragment, tag).addToBackStack(null)
        }
        ft.commitAllowingStateLoss()
    }
}

const val REQUEST_IMAGE = 0x01
const val REQUEST_GALLERY = 0x02
const val REQUEST_CAMERA = 0x03
const val REQUEST_FILE = 0x04
const val REQUEST_AUDIO = 0x05
fun Fragment.openImage(output: Uri) {
    val cameraIntents = ArrayList<Intent>()
    val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    val packageManager = this.activity!!.packageManager
    val listCam = packageManager.queryIntentActivities(captureIntent, 0)
    for (res in listCam) {
        val packageName = res.activityInfo.packageName
        val intent = Intent(captureIntent)
        intent.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
        intent.`package` = packageName
        intent.putExtra(MediaStore.EXTRA_OUTPUT, output)
        cameraIntents.add(intent)
    }

    val galleryIntent = Intent()
    galleryIntent.type = "image/*"
    galleryIntent.action = Intent.ACTION_PICK

    val chooserIntent = Intent.createChooser(galleryIntent, "Select Picture")
    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toTypedArray())
    try {
        this.startActivityForResult(chooserIntent, REQUEST_IMAGE)
    } catch (e: ActivityNotFoundException) {
    }
}

fun Fragment.openCamera(output: Uri) {
    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
        intent.putExtra(MediaStore.EXTRA_OUTPUT, output)
    } else {
        val file = File(output.path)
        val photoUri = FileProvider.getUriForFile(context!!.applicationContext,
            BuildConfig.APPLICATION_ID + ".provider", file)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    if (intent.resolveActivity(context!!.packageManager) != null) {
        startActivityForResult(intent, REQUEST_CAMERA)
    } else {
        context?.toast(R.string.error_no_camera)
    }
}

fun Fragment.selectMediaType(type: String, extraMimeType: Array<String>?, requestCode: Int) {
    val intent = Intent()
    intent.type = type
    intent.putExtra(Intent.EXTRA_MIME_TYPES, extraMimeType)
    intent.action = Intent.ACTION_OPEN_DOCUMENT
    try {
        startActivityForResult(intent, requestCode)
        return
    } catch (e: ActivityNotFoundException) {
    }

    intent.action = Intent.ACTION_GET_CONTENT
    try {
        startActivityForResult(intent, requestCode)
    } catch (e: ActivityNotFoundException) {
    }
}

fun Context.openPermissionSetting() {
    val intent = Intent()
    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    val uri = Uri.fromParts("package", packageName, null)
    intent.data = uri
    startActivity(intent)
    toast(R.string.error_permission)
}

fun Fragment.selectDocument() {
    selectMediaType("*/*", arrayOf("*/*"), REQUEST_FILE)
}

fun Fragment.selectAudio(requestCode: Int) {
    selectMediaType("audio/*", null, REQUEST_AUDIO)
}

fun Context.getAttachment(uri: Uri): Attachment? {
    var cursor: Cursor? = null

    try {
        cursor = contentResolver.query(uri, null, null, null, null)

        if (cursor != null && cursor.moveToFirst()) {
            val fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            val fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE))
            val mimeType = contentResolver.getType(uri)

            return Attachment(uri, fileName, mimeType, fileSize)
        }
    } finally {
        if (cursor != null) cursor.close()
    }
    return null
}

private val maxVideoSize by lazy {
    480f
}

fun Context.getVideoModel(uri: Uri): VideoEditedInfo? {
    try {
        val path = uri.getFilePath() ?: return null
        val m = MediaMetadataRetriever().apply {
            setDataSource(path)
        }
        val fileName = File(path).name
        val rotation = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
        val image = m.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST)
        val mediaWith = image.width
        val mediaHeight = image.height
        val duration = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
        val thumbnail = image.zoomOut()?.fastBlur(1f, 10)?.bitmap2String()
        val scale = if (mediaWith > mediaHeight) maxVideoSize / mediaWith else maxVideoSize / mediaHeight
        val resultWidth = (Math.round((mediaWith * scale / 2).toDouble()) * 2).toInt()
        val resultHeight = (Math.round((mediaHeight * scale / 2).toDouble()) * 2).toInt()
        return if (scale < 1) {
            val bitrate = MediaController.getBitrate(path, scale)
            VideoEditedInfo(path, duration, rotation, mediaWith, mediaHeight, resultWidth, resultHeight, thumbnail,
                fileName, bitrate)
        } else {
            VideoEditedInfo(path, duration, rotation, mediaWith, mediaHeight, mediaWith, mediaHeight, thumbnail,
                fileName, 0, false)
        }
    } catch (e: Exception) {
        Timber.e(e)
    }
    return null
}

fun Fragment.openGallery() {
    Gallery.from(this)
        .choose(MimeType.ofMedia())
        .imageEngine(GlideEngine())
        .forResult(REQUEST_GALLERY)
}

fun Fragment.openGalleryFromSticker() {
    Gallery.from(this)
        .choose(MimeType.ofSticker())
        .showSingleMediaType(true)
        .preview(false)
        .imageEngine(GlideEngine())
        .forResult(REQUEST_GALLERY)
}

fun Context.openUrl(url: String) {
    var uri = url.toUri()
    if (uri.scheme.isNullOrBlank()) {
        uri = Uri.parse("http://$url")
    }
    try {
        val actionIntent = Intent(this, ShareBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, actionIntent, 0)
        val customTabsIntent = CustomTabsIntent.Builder()
            .setToolbarColor(ContextCompat.getColor(this, android.R.color.white))
            .setShowTitle(true)
            .setActionButton(BitmapFactory.decodeResource(this.resources, R.drawable.ic_share_black_24dp),
                this.getString(R.string.share_file), pendingIntent)
            .build()
        customTabsIntent.launchUrl(this, uri)
        return
    } catch (e: Exception) {
        Log.e("OpenUrl", "OpenUrl", e)
    }
    try {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, packageName)
        startActivity(intent)
    } catch (e: Exception) {
        Log.e("OpenUrl", "OpenUrl", e)
    }
}

fun Context.getClipboardManager(): ClipboardManager = this.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

inline fun <T : Any, R> notNullElse(input: T?, normalAction: (T) -> R, default: R): R {
    return if (input == null) {
        default
    } else {
        input.let(normalAction)
    }
}

inline fun <T : Any, R> notNullElse(input: T?, normalAction: (T) -> R, elseAction: () -> R): R {
    return if (input != null) {
        input.let(normalAction)
    } else {
        elseAction()
    }
}

inline fun <T : Any> notNullElse(input: T?, normalAction: (T) -> Unit, elseAction: () -> Unit) {
    return if (input != null) {
        input.let(normalAction)
    } else {
        elseAction()
    }
}

inline fun <T : Number, R> notEmptyOrElse(input: T?, normalAction: (T) -> R, elseAction: () -> R): R {
    return if (input != null && input != 0) {
        normalAction(input)
    } else {
        elseAction()
    }
}

inline fun supportsOreo(code: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        code()
    }
}

inline fun supportsNougat(code: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        code()
    }
}

inline fun belowOreo(code: () -> Unit) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        code()
    }
}

inline fun <T : Fragment> T.withArgs(argsBuilder: Bundle.() -> Unit): T =
    this.apply { arguments = Bundle().apply(argsBuilder) }

fun Context.isGooglePlayServicesAvailable(): Boolean {
    val googleApiAvailability = GoogleApiAvailability.getInstance()
    val status = googleApiAvailability.isGooglePlayServicesAvailable(this)
    return status == ConnectionResult.SUCCESS
}