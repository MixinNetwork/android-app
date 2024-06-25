package one.mixin.android.extension

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.BUBBLE_PREFERENCE_NONE
import android.app.PendingIntent
import android.app.UiModeManager
import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.ContextWrapper
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.VibrationEffect.EFFECT_CLICK
import android.os.VibrationEffect.EFFECT_DOUBLE_CLICK
import android.os.VibrationEffect.EFFECT_HEAVY_CLICK
import android.os.VibrationEffect.EFFECT_TICK
import android.os.Vibrator
import android.os.VibratorManager
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.Browser
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import androidx.core.database.getStringOrNull
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailabilityLight
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.firebase.installations.FirebaseInstallations
import kotlinx.coroutines.CoroutineScope
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.receiver.ShareBroadcastReceiver
import one.mixin.android.ui.call.CallActivity
import one.mixin.android.util.Attachment
import one.mixin.android.util.RomUtil
import one.mixin.android.util.XiaomiUtilities
import one.mixin.android.util.blurhash.BlurHashEncoder
import one.mixin.android.util.video.MediaController
import one.mixin.android.util.video.VideoEditedInfo
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.absolutePath
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.gallery.Gallery
import one.mixin.android.widget.gallery.MimeType
import one.mixin.android.widget.gallery.engine.impl.GlideEngine
import timber.log.Timber
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import kotlin.math.roundToInt

private val uiHandler = Handler(Looper.getMainLooper())

fun mainThread(runnable: () -> Unit) {
    uiHandler.post(runnable)
}

fun mainThreadDelayed(
    runnable: () -> Unit,
    delayMillis: Long,
) {
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

inline val Context.layoutInflater: android.view.LayoutInflater
    get() = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as android.view.LayoutInflater

// Please consider whether the soft keyboard is displayed.
fun Activity.visibleDisplayHeight(): Int {
    val outRect = Rect()
    return try {
        window.decorView.getWindowVisibleDisplayFrame(outRect)
        outRect.height()
    } catch (e: ClassCastException) {
        Timber.e(e)
        displayHeight()
    }
}

fun Activity.hideKeyboard() {
    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
}

fun Context.runOnUiThread(
    f: Context.() -> Unit,
    delay: Long = 0L,
) {
    if (delay == 0L) {
        uiHandler.post { f() }
    } else {
        uiHandler.postDelayed({ f() }, delay)
    }
}

fun Throwable.getStackTraceInfo(): String {
    val trace: String
    try {
        val writer = StringWriter()
        val pw = PrintWriter(writer)
        this.printStackTrace(pw)
        trace = writer.toString()
        pw.close()
    } catch (e: Exception) {
        return ""
    }
    return trace
}

fun Context.runOnUiThread(f: Context.() -> Unit) {
    runOnUiThread(f, 0L)
}

fun runOnUiThread(
    runnable: Runnable,
    delay: Long = 0L,
) {
    if (delay == 0L) {
        uiHandler.post(runnable)
    } else {
        uiHandler.postDelayed(runnable, delay)
    }
}

fun cancelRunOnUiThread(runnable: Runnable) {
    uiHandler.removeCallbacks(runnable)
}

fun async(runnable: () -> Unit) {
    Thread(runnable).start()
}

fun async(
    runnable: () -> Unit,
    executor: ExecutorService,
): Future<out Any?> =
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

@SuppressLint("PrivateApi")
fun Context.hasNavigationBar(): Boolean {
    var hasNavigationBar = false
    val rs = this.resources
    val id = rs.getIdentifier("config_showNavigationBar", "bool", "android")
    if (id == 0) {
        hasNavigationBar = rs.getBoolean(id)
    }
    try {
        val systemPropertiesClass = Class.forName("android.os.SystemProperties")
        val m = systemPropertiesClass.getMethod("get", String::class.java)
        val navBarOverride = m.invoke(systemPropertiesClass, "qemu.hw.mainkeys") as String
        if ("1" == navBarOverride) {
            hasNavigationBar = false
        } else if ("0" == navBarOverride) {
            hasNavigationBar = true
        }
    } catch (_: Exception) {
    }
    return hasNavigationBar
}

fun Context.isActivityNotDestroyed(): Boolean {
    if (this is Activity) {
        if (this.isDestroyed || this.isFinishing) {
            return false
        }
    }
    return true
}

fun Context.vibrate(
    effect: VibrationEffect?,
    pattern: LongArray = longArrayOf(0, 20L),
) {
    val vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    if (effect != null && Build.VERSION.SDK_INT >= 26) {
        vibrator.vibrate(effect)
    } else if (Build.VERSION.SDK_INT >= 26) {
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(pattern, -1)
    }
}

fun Context.tickVibrate() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrate(VibrationEffect.createPredefined(EFFECT_TICK))
    } else {
        vibrate(effect = null, pattern = longArrayOf(0, 10L))
    }
}

fun Context.clickVibrate() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrate(VibrationEffect.createPredefined(EFFECT_CLICK))
    } else {
        vibrate(effect = null, pattern = longArrayOf(0, 20L))
    }
}

fun Context.heavyClickVibrate() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrate(VibrationEffect.createPredefined(EFFECT_HEAVY_CLICK))
    } else {
        vibrate(effect = null, pattern = longArrayOf(0, 30L))
    }
}

fun Context.doubleClickVibrate() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrate(VibrationEffect.createPredefined(EFFECT_DOUBLE_CLICK))
    } else {
        vibrate(effect = null, pattern = longArrayOf(0, 30L))
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

fun Context.pxToDp(px: Float): Float {
    return if (px == 0f) {
        0f
    } else {
        val scale = resources.displayMetrics.density
        px / scale
    }
}

fun Context.spToPx(sp: Float): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics).toInt()
}

var Context.displayMetrics: DisplayMetrics
    get() = resources.displayMetrics

    @Deprecated("Property does not have a setter")
    private set(
    @Suppress("UNUSED_PARAMETER") value
    ) = error("Property does not have a setter")

@ColorInt
fun Context.colorAttr(
    @AttrRes attribute: Int,
): Int = theme.color(attribute)

fun Resources.Theme.attr(
    @AttrRes attribute: Int,
): TypedValue {
    val typedValue = TypedValue()
    if (!resolveAttribute(attribute, typedValue, true)) {
        throw IllegalArgumentException("Failed to resolve attribute: $attribute")
    }

    return typedValue
}

@ColorInt
fun Resources.Theme.color(
    @AttrRes attribute: Int,
): Int {
    val attr = attr(attribute)
    if (attr.type < TypedValue.TYPE_FIRST_COLOR_INT || attr.type > TypedValue.TYPE_LAST_COLOR_INT) {
        throw IllegalArgumentException("Attribute value type is not color: $attribute")
    }

    return attr.data
}

fun Context.getPixelsInCM(
    cm: Float,
    isX: Boolean,
): Float =
    cm / 2.54f * if (isX) displayMetrics.xdpi else displayMetrics.ydpi

fun Context.isTablet(): Boolean = resources.getBoolean(R.bool.isTablet)

fun Context.appCompatActionBarHeight(): Int {
    val tv = TypedValue()
    theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)
    return resources.getDimensionPixelSize(tv.resourceId)
}

@Suppress("DEPRECATION")
fun Context.networkConnected(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = connectivityManager.activeNetworkInfo ?: return false
    return activeNetwork.isConnectedOrConnecting
}

fun Context.realSize(): Point {
    val size = Point()
    val manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    manager.defaultDisplay.getRealSize(size)
    return size
}

fun Context.displayHeight() = realSize().y - statusBarHeight() - navigationBarHeight()

fun Context.isWideScreen(): Boolean {
    val ratio = displayRatio()
    return ratio < 1.33f
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
    val authority = String.format("%s.provider", this.packageName)
    return FileProvider.getUriForFile(this, authority, file)
}

fun Context.maxItemWidth(): Int {
    return realSize().x * 4 / 5
}

// fragment
fun FragmentActivity.replaceFragment(
    fragment: Fragment,
    frameId: Int,
) {
    supportFragmentManager.inTransaction { replace(frameId, fragment) }
}

fun FragmentActivity.replaceFragment(
    fragment: Fragment,
    frameId: Int,
    tag: String,
) {
    supportFragmentManager.inTransaction { replace(frameId, fragment, tag) }
}

fun FragmentActivity.addFragment(
    @Suppress("UNUSED_PARAMETER") from: Fragment,
    to: Fragment,
    tag: String,
    id: Int = R.id.container,
) {
    val ft =
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, 0, 0, R.anim.slide_out_right)
    if (to.isAdded) {
        ft.show(to)
    } else {
        ft.add(id, to, tag)
    }
    ft.addToBackStack(null)
    ft.commitAllowingStateLoss()
}

fun Fragment.navTo(
    fragment: Fragment,
    tag: String,
) {
    activity?.addFragment(this, fragment, tag)
}

inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> Unit) {
    val fragmentTransaction = beginTransaction()
    fragmentTransaction.func()
    fragmentTransaction.commitAllowingStateLoss()
}

const val REQUEST_IMAGE = 0x01
const val REQUEST_GALLERY = 0x02
const val REQUEST_CAMERA = 0x03
const val REQUEST_FILE = 0x04
const val REQUEST_AUDIO = 0x05
const val REQUEST_LOCATION = 0x06

@Suppress("unused")
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

@SuppressLint("ObsoleteSdkInt")
fun Fragment.openCamera(output: Uri) {
    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
        intent.putExtra(MediaStore.EXTRA_OUTPUT, output)
    } else {
        val path = output.path ?: return
        val file = File(path)
        val photoUri =
            FileProvider.getUriForFile(
                requireContext().applicationContext,
                BuildConfig.APPLICATION_ID + ".provider",
                file,
            )
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    if (intent.resolveActivity(requireContext().packageManager) != null) {
        startActivityForResult(intent, REQUEST_CAMERA)
    } else {
        toast(R.string.No_camera)
    }
}

fun String.isFileUri(): Boolean {
    return try {
        val uri = Uri.parse(this)
        uri.scheme == ContentResolver.SCHEME_FILE
    } catch (e: Exception) {
        false
    }
}

fun Context.openMedia(messageItem: MessageItem) {
    val intent = Intent()
    intent.action = Intent.ACTION_VIEW
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        messageItem.absolutePath()?.let {
            val uri = Uri.parse(it)
            if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                intent.setDataAndType(uri, messageItem.mediaMimeType)
                startActivity(intent)
            } else {
                val path =
                    if (uri.scheme == ContentResolver.SCHEME_FILE) {
                        uri.path
                    } else {
                        messageItem.absolutePath()
                    }
                if (path == null) {
                    toast(R.string.File_does_not_exist)
                    return@let
                }
                val file = File(path)
                if (!file.exists()) {
                    toast(R.string.File_does_not_exist)
                } else {
                    intent.setDataAndType(getUriForFile(file), messageItem.mediaMimeType)
                    startActivity(intent)
                }
            }
        }
    } catch (e: ActivityNotFoundException) {
        toast(R.string.error_unable_to_open_media)
    } catch (e: SecurityException) {
        toast(R.string.File_does_not_exist)
    }
}

fun Context.openMedia(messageItem: ChatHistoryMessageItem) {
    val intent = Intent()
    intent.action = Intent.ACTION_VIEW
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        messageItem.absolutePath()?.let {
            val uri = Uri.parse(it)
            if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                intent.setDataAndType(uri, messageItem.mediaMimeType)
                startActivity(intent)
            } else {
                val path =
                    if (uri.scheme == ContentResolver.SCHEME_FILE) {
                        uri.path
                    } else {
                        messageItem.absolutePath()
                    }
                if (path == null) {
                    toast(R.string.File_does_not_exist)
                    return@let
                }
                val file = File(path)
                if (!file.exists()) {
                    toast(R.string.File_does_not_exist)
                } else {
                    intent.setDataAndType(getUriForFile(file), messageItem.mediaMimeType)
                    startActivity(intent)
                }
            }
        }
    } catch (e: ActivityNotFoundException) {
        toast(R.string.error_unable_to_open_media)
    } catch (e: SecurityException) {
        toast(R.string.File_does_not_exist)
    }
}

fun Fragment.selectMediaType(
    type: String,
    extraMimeType: Array<String>?,
    requestCode: Int,
) {
    val intent = Intent()
    intent.type = type
    intent.putExtra(Intent.EXTRA_MIME_TYPES, extraMimeType)
    intent.action = Intent.ACTION_OPEN_DOCUMENT
    try {
        startActivityForResult(intent, requestCode)
        return
    } catch (_: ActivityNotFoundException) {
    }

    intent.action = Intent.ACTION_GET_CONTENT
    try {
        startActivityForResult(intent, requestCode)
    } catch (_: ActivityNotFoundException) {
    } catch (_: SecurityException) {
    }
}

fun Context.openPermissionSetting(enableToast: Boolean = true) {
    val intent = Intent()
    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    val uri = Uri.fromParts("package", packageName, null)
    intent.data = uri
    startActivity(intent)
    if (enableToast) {
        toast(R.string.error_permission)
    }
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
        openPermissionSetting(false)
    }
}

fun Fragment.selectDocument() {
    selectMediaType("*/*", arrayOf("*/*"), REQUEST_FILE)
}

fun Fragment.selectAudio() {
    selectMediaType("audio/*", null, REQUEST_AUDIO)
}

fun Context.getAttachment(
    local: Uri,
    mimeType: String? = null,
): Attachment? {
    var cursor: Cursor? = null
    try {
        val uri =
            if (local.authority == null) {
                val path = local.path ?: return null
                getUriForFile(File(path))
            } else {
                local
            }
        cursor = contentResolver.query(uri, null, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            val fileName = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) ?: "File"
            val copyPath = uri.copyFileUrlWithAuthority(this, fileName)
            val resultUri =
                if (copyPath == null) {
                    return null
                } else {
                    getUriForFile(File(copyPath))
                }
            val fileSize = File(copyPath).length()
            return Attachment(resultUri, fileName, mimeType ?: contentResolver.getType(uri) ?: "", fileSize)
        }
    } catch (e: Exception) {
        toast(R.string.File_does_not_exist)
    } finally {
        cursor?.close()
    }
    return null
}

private val maxVideoSize by lazy {
    1280.0f
}

fun getVideoModel(uri: Uri): VideoEditedInfo? {
    try {
        @Suppress("DEPRECATION")
        val path = uri.getFilePath() ?: return null
        val m =
            MediaMetadataRetriever().apply {
                setDataSource(path)
            }
        val fileName = File(path).name
        val rotation = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION) ?: "0"
        val image = m.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST) ?: return null
        val mediaWith = image.width
        val mediaHeight = image.height
        val duration = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
        val thumbnail = BlurHashEncoder.encode(image)
        val scale = if (mediaWith > mediaHeight) maxVideoSize / mediaWith else maxVideoSize / mediaHeight
        val resultWidth = ((mediaWith * scale / 2).toDouble().roundToInt() * 2)
        val resultHeight = ((mediaHeight * scale / 2).toDouble().roundToInt() * 2)
        return if (scale < 1) {
            val bitrate = MediaController.getBitrate(path, scale)
            VideoEditedInfo(
                path,
                duration,
                rotation,
                mediaWith,
                mediaHeight,
                resultWidth,
                resultHeight,
                thumbnail,
                fileName,
                bitrate,
            )
        } else {
            VideoEditedInfo(
                path,
                duration,
                rotation,
                mediaWith,
                mediaHeight,
                mediaWith,
                mediaHeight,
                thumbnail,
                fileName,
                0,
                false,
            )
        }
    } catch (e: Exception) {
        Timber.e(e)
    }
    return null
}

fun Fragment.openImageGallery(preview: Boolean = false) {
    Gallery.from(this)
        .choose(MimeType.ofImage())
        .imageEngine(GlideEngine())
        .preview(preview)
        .forResult(REQUEST_GALLERY)
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
        val pendingIntent = PendingIntent.getBroadcast(this, 0, actionIntent, PendingIntent.FLAG_IMMUTABLE)
        val customTabsIntent =
            CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(ContextCompat.getColor(this, android.R.color.white))
                        .build(),
                )
                .setShowTitle(true)
                .setActionButton(
                    BitmapFactory.decodeResource(this.resources, R.drawable.ic_share),
                    this.getString(R.string.Share),
                    pendingIntent,
                )
                .build()
        customTabsIntent.launchUrl(this, uri)
        return
    } catch (e: Exception) {
        Timber.e(e, "OpenUrl")
    }
    try {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, packageName)
        startActivity(intent)
    } catch (e: Exception) {
        Timber.e(e, "OpenUrl")
    }
}

fun Context.openExternalUrl(url: String) {
    try {
        var uri = Uri.parse(url)
        if (uri.scheme == null) {
            uri = uri.buildUpon().scheme("https").build()
        }
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    } catch (e: Exception) {
        Timber.e(e, "OpenUrl")
    }
}

fun Context.getClipboardManager(): ClipboardManager = requireNotNull(getSystemService())

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

inline fun <T, R> T?.notNullWithElse(
    normalAction: (T) -> R,
    default: R,
): R {
    return if (this == null) {
        default
    } else {
        normalAction(this)
    }
}

inline fun <T, R> T?.notNullWithElse(
    normalAction: (T) -> R,
    elseAction: () -> R,
): R {
    return if (this != null) {
        normalAction(this)
    } else {
        elseAction()
    }
}

inline fun CharSequence?.notEmptyWithElse(
    normalAction: (CharSequence) -> Unit,
    elseAction: () -> Unit,
) {
    return if (!this.isNullOrEmpty()) {
        normalAction(this)
    } else {
        elseAction()
    }
}

inline fun <T, R> Collection<T>?.notEmptyWithElse(
    normalAction: (Collection<T>) -> R,
    default: R,
): R {
    return if (!this.isNullOrEmpty()) {
        normalAction(this)
    } else {
        default
    }
}

inline fun <T, R> Collection<T>?.notEmptyWithElse(
    normalAction: (Collection<T>) -> R,
    elseAction: () -> R,
): R {
    return if (!this.isNullOrEmpty()) {
        normalAction(this)
    } else {
        elseAction()
    }
}

inline fun <T : Number, R> T?.notEmptyWithElse(
    normalAction: (T) -> R,
    elseAction: () -> R,
): R {
    return if (this != null && this != 0) {
        normalAction(this)
    } else {
        elseAction()
    }
}

fun supportsS(
    code: () -> Unit,
    elseAction: (() -> Unit)? = null,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        code()
    } else {
        elseAction?.invoke()
    }
}

fun supportsR(
    code: () -> Unit,
    elseAction: (() -> Unit)? = null,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        code()
    } else {
        elseAction?.invoke()
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

fun supportsOreo(
    code: () -> Unit,
    elseAction: (() -> Unit),
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        code()
    } else {
        elseAction.invoke()
    }
}

@SuppressLint("ObsoleteSdkInt")
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
    GoogleApiAvailabilityLight.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS

fun Context.isFirebaseDecodeAvailable() =
    isGooglePlayServicesAvailable() && Locale.getDefault() != Locale.CHINA

fun Fragment.getTipsByAsset(asset: TokenItem) =
    when (asset.assetId) {
        Constants.ChainId.BITCOIN_CHAIN_ID -> getString(R.string.deposit_tip_btc)
        Constants.ChainId.ETHEREUM_CHAIN_ID -> getString(R.string.deposit_tip_eth)
        Constants.ChainId.EOS_CHAIN_ID -> getString(R.string.deposit_tip_eos)
        Constants.ChainId.TRON_CHAIN_ID -> getString(R.string.deposit_tip_trx)
        else -> getString(R.string.deposit_tip_common, asset.symbol)
    }

fun Context.showConfirmDialog(
    message: String,
    action: () -> Unit,
) {
    alertDialogBuilder()
        .setMessage(message)
        .setNegativeButton(R.string.Cancel) { dialog, _ ->
            dialog.dismiss()
        }.setPositiveButton(R.string.OK) { dialog, _ ->
            action.invoke()
            dialog.dismiss()
        }.create().apply {
            setOnShowListener {
                getButton(DialogInterface.BUTTON_POSITIVE).textColorResource = R.color.colorRed
            }
        }.show()
}

fun Context.showConfirmDialog(
    message: String,
    cancelable: Boolean,
    action: () -> Unit,
) {
    alertDialogBuilder()
        .setMessage(message).setCancelable(cancelable)
        .setNegativeButton(R.string.Cancel) { dialog, _ ->
            dialog.dismiss()
        }.setPositiveButton(R.string.Confirm) { dialog, _ ->
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
        val currentId =
            defaultSharedPreferences.getInt(
                Constants.Theme.THEME_CURRENT_ID,
                Constants.Theme.THEME_AUTO_ID,
            )
        return if (currentId == Constants.Theme.THEME_AUTO_ID) {
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        } else {
            currentId == Constants.Theme.THEME_NIGHT_ID
        }
    } else {
        defaultSharedPreferences.getInt(
            Constants.Theme.THEME_CURRENT_ID,
            Constants.Theme.THEME_DEFAULT_ID,
        ) == Constants.Theme.THEME_NIGHT_ID
    }
}

fun Context.isLandscape() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

fun Context.isAutoRotate() = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1

fun toastShort(textResource: Int) = toast(textResource, ToastDuration.Short)

fun Context.getCurrentThemeId() =
    defaultSharedPreferences.getInt(
        Constants.Theme.THEME_CURRENT_ID,
        defaultThemeId,
    )

val defaultThemeId =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        Constants.Theme.THEME_DEFAULT_ID
    } else {
        Constants.Theme.THEME_AUTO_ID
    }

fun Context.checkInlinePermissions(): Boolean {
    if (RomUtil.isMiui && !XiaomiUtilities.isCustomPermissionGranted(XiaomiUtilities.OP_BACKGROUND_START_ACTIVITY)) {
        return false
    }
    if (Settings.canDrawOverlays(this)) {
        return true
    }
    return false
}

fun Context.checkInlinePermissions(showAlert: () -> Unit): Boolean {
    if (RomUtil.isMiui && !XiaomiUtilities.isCustomPermissionGranted(XiaomiUtilities.OP_BACKGROUND_START_ACTIVITY)) {
        var intent = XiaomiUtilities.getPermissionManagerIntent()
        if (intent != null) {
            try {
                startActivity(intent)
            } catch (x: Exception) {
                try {
                    intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data =
                        Uri.parse("package:" + MixinApplication.appContext.packageName)
                    startActivity(intent)
                } catch (xx: Exception) {
                    Timber.e(xx)
                }
            }
        }
        toast(R.string.need_background_permission)
        return false
    }
    if (Settings.canDrawOverlays(this)) {
        return true
    } else {
        showAlert()
    }
    return false
}

fun Context.isPlayStoreInstalled(): Boolean {
    return try {
        packageManager.getPackageInfoCompat(GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE, 0)
        true
    } catch (e: Exception) {
        false
    }
}

fun PackageManager.getPackageInfoCompat(
    packageName: String,
    flags: Int = 0,
): PackageInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        getPackageInfo(packageName, flags)
    }

fun Context.openMarket(
    fragmentManager: FragmentManager,
    scope: CoroutineScope,
) {
    if (isPlayStoreInstalled()) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("market://details?id=${BuildConfig.APPLICATION_ID}")
            intent.setPackage(GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE)
            startActivity(intent)
        } catch (e: Exception) {
            getString(R.string.website).openAsUrlOrWeb(this, null, fragmentManager, scope)
        }
    } else {
        getString(R.string.website).openAsUrlOrWeb(this, null, fragmentManager, scope)
    }
}

@Suppress("DEPRECATION") // Deprecated for third party Services.
fun <T> Context.isServiceRunning(service: Class<T>) =
    (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
        .getRunningServices(Integer.MAX_VALUE)
        .any { it.service.className == service.name }

fun Activity.showPipPermissionNotification(
    targetActivity: Class<*>,
    title: String,
) {
    val pendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, targetActivity),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    val builder =
        NotificationCompat.Builder(this, CallActivity.CHANNEL_PIP_PERMISSION)
            .setSmallIcon(R.drawable.ic_msg_default)
            .setContentIntent(pendingIntent)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
    supportsOreo {
        val channel =
            NotificationChannel(
                CallActivity.CHANNEL_PIP_PERMISSION,
                getString(R.string.OTHER),
                NotificationManager.IMPORTANCE_HIGH,
            )
        notificationManager.createNotificationChannel(channel)
    }
    notificationManager.notify(CallActivity.ID_PIP_PERMISSION, builder.build())
}

@SuppressLint("HardwareIds")
fun getStringDeviceId(resolver: ContentResolver): String {
    var deviceId = Settings.Secure.getString(resolver, Settings.Secure.ANDROID_ID)
    if (deviceId == null || deviceId == "9774d56d682e549c") {
        deviceId = FirebaseInstallations.getInstance().id.result
    }
    return UUID.nameUUIDFromBytes(deviceId.toByteArray()).toString()
}

fun Context.getStringDeviceId(): String {
    return getStringDeviceId(contentResolver)
}

fun Context.handleIgnoreBatteryOptimization(newTask: Boolean = false) {
    if (Build.MANUFACTURER.equalsIgnoreCase("google") || Build.MANUFACTURER.equalsIgnoreCase("samsung")) {
        requestIgnoreBatteryOptimization(newTask)
    } else {
        openIgnoreBatteryOptimizationSetting(newTask)
    }
}

fun Context.requestIgnoreBatteryOptimization(newTask: Boolean = false) {
    Intent().apply {
        action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        data = Uri.parse("package:$packageName")
        if (newTask) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(this)
        } catch (e: ActivityNotFoundException) {
            Timber.w("Battery optimization activity not found")
        }
    }
}

fun Context.openIgnoreBatteryOptimizationSetting(newTask: Boolean = false) {
    Intent().apply {
        action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        if (newTask) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(this)
        } catch (e: ActivityNotFoundException) {
            Timber.w("Power setting activity not found")
        }
    }
}

fun Context.getDisplayPath(uri: Uri): String {
    val lastPathSegment = requireNotNull(uri.lastPathSegment)
    val backupVolume = lastPathSegment.replaceFirst(":.*".toRegex(), "")
    val backupName = lastPathSegment.replaceFirst(".*:".toRegex(), "")
    val storageManager: StorageManager =
        requireNotNull(
            ContextCompat.getSystemService(
                this,
                StorageManager::class.java,
            ),
        )
    val storageVolumes = storageManager.storageVolumes
    var storageVolume: StorageVolume? = null
    for (volume in storageVolumes) {
        if (volume.uuid == backupVolume) {
            storageVolume = volume
            break
        }
    }
    return if (storageVolume == null) {
        backupName
    } else {
        String.format("%s/%s", storageVolume.getDescription(this), backupName)
    }
}

inline fun <reified T> Fragment.findListener(): T? {
    var parent: Fragment? = parentFragment
    while (parent != null) {
        if (parent is T) {
            return parent
        }
        parent = parent.parentFragment
    }

    return requireActivity() as? T
}

val Context.notificationManager: NotificationManager
    get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

@RequiresApi(Build.VERSION_CODES.R)
fun NotificationManager.areBubblesAllowedCompat(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        bubblePreference != BUBBLE_PREFERENCE_NONE
    } else {
        @Suppress("DEPRECATION")
        areBubblesAllowed() || notificationChannels.any { it.canBubble() }
    }
}

fun Context.shareFile(file: File) {
    Intent().apply {
        val uri = getUriForFile(file)
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val extraMimeTypes = arrayOf("text/plain", "audio/*", "image/*", "video/*")
        putExtra(Intent.EXTRA_MIME_TYPES, extraMimeTypes)
        setType("application/*")
        val resInfoList = packageManager.queryIntentActivities(this, PackageManager.MATCH_DEFAULT_ONLY)
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(Intent.createChooser(this, getString(R.string.File)))
        } catch (ignored: ActivityNotFoundException) {
            Timber.e(ignored)
        }
    }
}

fun Context.shareMedia(
    isVideo: Boolean,
    url: String,
) {
    var uri: Uri
    val sendIntent =
        Intent().apply {
            action = Intent.ACTION_SEND
            uri = Uri.parse(url)
            if (ContentResolver.SCHEME_FILE == uri.scheme) {
                @Suppress("DEPRECATION")
                val path = uri.getFilePath(this@shareMedia)
                if (path == null) {
                    toast(R.string.File_does_not_exist)
                    return
                }
                uri = getUriForFile(File(path))
                putExtra(Intent.EXTRA_STREAM, uri)
            } else {
                putExtra(Intent.EXTRA_STREAM, uri)
            }
            type = if (isVideo) "video/*" else "image/*"
        }
    val name = getString(if (isVideo) R.string.Video else R.string.Photo)
    val chooser = Intent.createChooser(sendIntent, getString(R.string.share_to, name))
    val resInfoList = packageManager.queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY)
    resInfoList.forEach {
        val packageName = it.activityInfo.packageName
        grantUriPermission(
            packageName,
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
    }
    startActivity(chooser)
}

fun Context.openEmail(email: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
        startActivity(intent)
    } catch (e: Exception) {
        Timber.e(e)
    }
}

fun Context.callPhone(phone: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    } catch (e: Exception) {
        Timber.e(e)
    }
}

fun Context.findFragmentActivityOrNull(): FragmentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) {
            return ctx
        }
        ctx = ctx.baseContext
    }
    return null
}

fun Context.isAuto(): Boolean {
    val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_CAR
}
