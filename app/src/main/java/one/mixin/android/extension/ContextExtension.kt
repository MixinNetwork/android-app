package one.mixin.android.extension

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
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
import android.view.Window
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
import java.io.File
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import kotlin.math.roundToInt
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.receiver.ShareBroadcastReceiver
import one.mixin.android.util.Attachment
import one.mixin.android.util.video.MediaController
import one.mixin.android.util.video.VideoEditedInfo
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.MessageItem
import one.mixin.android.widget.gallery.Gallery
import one.mixin.android.widget.gallery.MimeType
import one.mixin.android.widget.gallery.engine.impl.GlideEngine
import org.jetbrains.anko.configuration
import org.jetbrains.anko.displayMetrics
import org.jetbrains.anko.textColorResource
import timber.log.Timber

private val uiHandler = Handler(Looper.getMainLooper())

fun Context.mainThread(runnable: () -> Unit) {
    uiHandler.post(runnable)
}

fun Context.mainThreadDelayed(runnable: () -> Unit, delayMillis: Long) {
    uiHandler.postDelayed(runnable, delayMillis)
}

fun Context.colorFromAttribute(attribute: Int): Int {
    val attributes = obtainStyledAttributes(intArrayOf(attribute))
    val color = attributes.getColor(0, 0)
    attributes.recycle()
    return color
}

fun Context.booleanFromAttribute(attribute: Int): Boolean {
    val attributes = obtainStyledAttributes(intArrayOf(attribute))
    val b = attributes.getBoolean(0, false)
    attributes.recycle()
    return b
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
        val scale = resources.displayMetrics.density
        (dp * scale + 0.5f).toInt()
    }
}

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

fun Context.realSize(): Point {
    val size = Point()
    val manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    manager.defaultDisplay.getRealSize(size)
    return size
}

fun Context.screenHeight(): Int {
    return realSize().y
}

fun Context.screenWidth(): Int {
    return realSize().x
}

fun Context.displayRatio(): Float {
    val size = realSize()
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
    if (bottom > realSize().y) {
        return true
    }

    if (Build.MANUFACTURER == "smartisan") {
        return true
    }
    val hasMenuKey = ViewConfiguration.get(this).hasPermanentMenuKey()
    val hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)
    return !hasMenuKey && !hasBackKey
}

private var maxItemWidth: Int? = null

fun Context.maxItemWidth(): Int {
    if (maxItemWidth == null) {
        maxItemWidth = realSize().x * 4 / 5
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

fun FragmentActivity.addFragment(from: Fragment, to: Fragment, tag: String, id: Int = R.id.container) {
    val fm = supportFragmentManager
    fm?.let {
        val ft = it.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, 0, 0, R.anim.slide_out_right)
        if (to.isAdded) {
            ft.show(to)
        } else {
            ft.add(id, to, tag)
        }
        ft.addToBackStack(null)
        ft.commitAllowingStateLoss()
    }
}

fun Fragment.navTo(fragment: Fragment, tag: String) {
    activity?.addFragment(this, fragment, tag)
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
    val packageManager = this.requireActivity().packageManager
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
        val photoUri = FileProvider.getUriForFile(requireContext().applicationContext,
            BuildConfig.APPLICATION_ID + ".provider", file)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    if (intent.resolveActivity(requireContext().packageManager) != null) {
        startActivityForResult(intent, REQUEST_CAMERA)
    } else {
        context?.toast(R.string.error_no_camera)
    }
}

fun Context.openMedia(messageItem: MessageItem) {
    val intent = Intent()
    intent.action = Intent.ACTION_VIEW
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        messageItem.mediaUrl?.let {
            val uri = Uri.parse(it)
            if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                intent.setDataAndType(uri, messageItem.mediaMimeType)
                startActivity(intent)
            } else {
                val file = File(if (uri.scheme == ContentResolver.SCHEME_FILE) {
                    uri.path
                } else {
                    messageItem.mediaUrl
                })
                if (!file.exists()) {
                    toast(R.string.error_file_exists)
                } else {
                    intent.setDataAndType(getUriForFile(file), messageItem.mediaMimeType)
                    startActivity(intent)
                }
            }
        }
    } catch (e: ActivityNotFoundException) {
        toast(R.string.error_unable_to_open_media)
    } catch (e: SecurityException) {
        toast(R.string.error_file_exists)
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

fun Context.openNotificationSetting() {
    try {
        val intent = Intent()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        } else {
            intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
        }
        intent.putExtra("app_package", packageName)
        intent.putExtra("app_uid", applicationInfo.uid)
        startActivity(intent)
    } catch (e: Exception) {
        Timber.e(e)
    }
}

fun Fragment.selectDocument() {
    selectMediaType("*/*", arrayOf("*/*"), REQUEST_FILE)
}

fun Fragment.selectAudio(requestCode: Int) {
    selectMediaType("audio/*", null, REQUEST_AUDIO)
}

fun Context.getAttachment(local: Uri): Attachment? {
    var cursor: Cursor? = null
    try {
        val uri = if (local.authority == null) {
            getUriForFile(File(local.path))
        } else {
            local
        }
        cursor = contentResolver.query(uri, null, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            val fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            val mimeType = contentResolver.getType(uri) ?: ""

            val copyPath = uri.copyFileUrlWithAuthority(this, fileName)
            val resultUri = if (copyPath == null) {
                return null
            } else {
                getUriForFile(File(copyPath))
            }
            val fileSize = File(copyPath).length()
            return Attachment(resultUri, fileName, mimeType, fileSize)
        }
    } catch (e: SecurityException) {
        toast(R.string.error_file_exists)
    } finally {
        cursor?.close()
    }
    return null
}

private val maxVideoSize by lazy {
    480f
}

fun getVideoModel(uri: Uri): VideoEditedInfo? {
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
        val resultWidth = ((mediaWith * scale / 2).toDouble().roundToInt() * 2)
        val resultHeight = ((mediaHeight * scale / 2).toDouble().roundToInt() * 2)
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

fun Fragment.openGallery(preview: Boolean = false) {
    Gallery.from(this)
        .choose(MimeType.ofMedia())
        .imageEngine(GlideEngine())
        .preview(preview)
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
            .setActionButton(BitmapFactory.decodeResource(this.resources, R.drawable.ic_share),
                this.getString(R.string.share), pendingIntent)
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

fun Window.isNotchScreen(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val insets = decorView.rootWindowInsets
        if (insets != null) {
            val cutout = insets.displayCutout
            if (cutout != null) {
                val rects = cutout.boundingRects
                if (rects.size > 0) {
                    return true
                }
            }
        }
        return false
    } else {
        return false
    }
}

inline fun <T, R> T?.notNullWithElse(normalAction: (T) -> R, default: R): R {
    return if (this == null) {
        default
    } else {
        normalAction(this)
    }
}

inline fun <T, R> T?.notNullWithElse(normalAction: (T) -> R, elseAction: () -> R): R {
    return if (this != null) {
        normalAction(this)
    } else {
        elseAction()
    }
}

inline fun <T> T?.notNullWithElse(normalAction: (T) -> Unit, elseAction: () -> Unit) {
    return if (this != null) {
        normalAction(this)
    } else {
        elseAction()
    }
}

inline fun <T : Number, R> T?.notEmptyWithElse(normalAction: (T) -> R, elseAction: () -> R): R {
    return if (this != null && this != 0) {
        normalAction(this)
    } else {
        elseAction()
    }
}

inline fun supportsQ(code: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        code()
    }
}

inline fun supportsPie(code: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        code()
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

fun Context.isGooglePlayServicesAvailable() =
    GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS &&
        Locale.getDefault() != Locale.CHINA

fun Fragment.getTipsByAsset(asset: AssetItem) =
    when (asset.chainId) {
        Constants.ChainId.BITCOIN_CHAIN_ID -> getString(R.string.bottom_deposit_tip_btc)
        Constants.ChainId.ETHEREUM_CHAIN_ID -> getString(R.string.bottom_deposit_tip_eth)
        Constants.ChainId.EOS_CHAIN_ID -> getString(R.string.bottom_deposit_tip_eos)
        Constants.ChainId.TRON_CHAIN_ID -> getString(R.string.bottom_deposit_tip_trx)
        else -> getString(R.string.bottom_deposit_tip_common, asset.symbol)
    }

fun Context.showConfirmDialog(
    message: String,
    action: () -> Unit
) {
    alertDialogBuilder()
        .setMessage(message)
        .setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }.setPositiveButton(R.string.ok) { dialog, _ ->
            action.invoke()
            dialog.dismiss()
        }.create().apply {
            setOnShowListener {
                getButton(DialogInterface.BUTTON_POSITIVE).textColorResource = R.color.colorRed
            }
        }.show()
}

fun Context.isNightMode(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val currentId = defaultSharedPreferences.getInt(
            Constants.Theme.THEME_CURRENT_ID,
            Constants.Theme.THEME_AUTO_ID
        )
        return if (currentId == Constants.Theme.THEME_AUTO_ID) {
            configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        } else {
            currentId == Constants.Theme.THEME_NIGHT_ID
        }
    } else {
        defaultSharedPreferences.getInt(
            Constants.Theme.THEME_CURRENT_ID,
            Constants.Theme.THEME_DEFAULT_ID
        ) == Constants.Theme.THEME_NIGHT_ID
    }
}

fun Context.isLandscape() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

fun Context.isAutoRotate() = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1

fun Fragment.toast(textResource: Int) = requireActivity().toast(textResource)

fun Fragment.toast(text: CharSequence) = requireActivity().toast(text)
