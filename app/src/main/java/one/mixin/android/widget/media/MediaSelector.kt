package one.mixin.android.widget.media

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.util.Pair
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.OvershootInterpolator
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.loader.app.LoaderManager
import one.mixin.android.R

@SuppressLint("InflateParams")
class MediaSelector(
    context: Context,
    loaderManager: LoaderManager,
    private var listener: MediaSelectorListener?,
    onDismissListener: OnDismissListener
) : PopupWindow(context) {

    private var currentAnchor: View? = null
    private val recentPhotos: RecentPhotoRecyclerView
    private val menuCamera: View
    private val menuGallery: View
    private val menuVideo: View
    private val menuTransfer: View
    private val menuMusic: View
    private val menuLocation: View
    private val menuDocument: View
    private val menuDown: View

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val layout = inflater.inflate(R.layout.view_media_selector, null, true) as ConstraintLayout
        recentPhotos = layout.findViewById(R.id.recent_photos)
        recentPhotos.setListener(RecentPhotoSelectedListener())
        menuCamera = layout.findViewById(R.id.menu_camera)
        menuGallery = layout.findViewById(R.id.menu_gallery)
        menuVideo = layout.findViewById(R.id.menu_video)
        menuTransfer = layout.findViewById(R.id.menu_transfer)
        menuMusic = layout.findViewById(R.id.menu_music)
        menuLocation = layout.findViewById(R.id.menu_location)
        menuDocument = layout.findViewById(R.id.menu_document)
        menuDown = layout.findViewById(R.id.menu_down)
        contentView = layout
        width = LinearLayout.LayoutParams.MATCH_PARENT
        height = LinearLayout.LayoutParams.WRAP_CONTENT
        animationStyle = 0
        inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
        setBackgroundDrawable(ColorDrawable())
        isFocusable = true
        isTouchable = true
        setOnDismissListener(onDismissListener)
        loaderManager.initLoader<Cursor>(1, null, recentPhotos)
        menuDown.setOnClickListener {
            dismiss()
        }
        menuCamera.setOnClickListener { listener?.onClick(TYPE_CAMERA); dismiss() }
        menuGallery.setOnClickListener { listener?.onClick(TYPE_GALLERY); dismiss() }
        menuVideo.setOnClickListener { listener?.onClick(TYPE_VIDEO); dismiss() }
        menuTransfer.setOnClickListener { listener?.onClick(TYPE_TRANSFER); dismiss() }
        menuMusic.setOnClickListener { listener?.onClick(TYPE_MUSIC); dismiss() }
        menuLocation.setOnClickListener { listener?.onClick(TYPE_LOCATION); dismiss() }
        menuDocument.setOnClickListener { listener?.onClick(TYPE_DOCUMENT); dismiss() }
    }

    fun show(anchor: View) {
        this.currentAnchor = anchor
        showAtLocation(anchor, Gravity.BOTTOM, 0, 0)
        contentView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                contentView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                animateWindowInCircular(anchor, contentView)
            }
        })
        animateButtonIn(menuCamera, ANIMATION_DURATION / 2)
        animateButtonIn(menuGallery, ANIMATION_DURATION / 2)
        animateButtonIn(menuVideo, ANIMATION_DURATION / 3)
        animateButtonIn(menuTransfer, ANIMATION_DURATION / 3)
        animateButtonIn(menuMusic, ANIMATION_DURATION / 4)
        animateButtonIn(menuLocation, ANIMATION_DURATION / 4)
        animateButtonIn(menuDocument, 0)
        animateButtonIn(menuDown, 0)
    }

    private fun animateButtonIn(button: View, delay: Int) {
        val animation = AnimationSet(true)
        val scale = ScaleAnimation(
            0.0f, 1.0f, 0.0f, 1.0f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.0f
        )

        animation.addAnimation(scale)
        animation.interpolator = OvershootInterpolator(1f)
        animation.duration = ANIMATION_DURATION.toLong()
        animation.startOffset = delay.toLong()
        button.startAnimation(animation)
    }

    override fun dismiss() {
        animateWindowOutCircular(currentAnchor, contentView)
    }

    @Suppress("unused")
    fun setListener(listener: MediaSelectorListener?) {
        this.listener = listener
    }

    private fun animateWindowInCircular(anchor: View?, contentView: View) {
        val coordinates = getClickOrigin(anchor, contentView)
        val animator = ViewAnimationUtils.createCircularReveal(
            contentView,
            coordinates.first,
            coordinates.second,
            0f,
            Math.max(contentView.width, contentView.height).toFloat()
        )
        animator.duration = ANIMATION_DURATION.toLong()
        animator.start()
    }

    private fun animateWindowOutCircular(anchor: View?, contentView: View) {
        val coordinates = getClickOrigin(anchor, contentView)
        val animator = ViewAnimationUtils.createCircularReveal(
            getContentView(),
            coordinates.first,
            coordinates.second,
            Math.max(getContentView().width, getContentView().height).toFloat(),
            0f
        )

        animator.duration = ANIMATION_DURATION.toLong()
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                super@MediaSelector.dismiss()
            }

            override fun onAnimationCancel(animation: Animator) {}

            override fun onAnimationRepeat(animation: Animator) {}
        })

        animator.start()
    }

    private fun animateWindowOutTranslate(contentView: View) {
        val animation = TranslateAnimation(0f, 0f, 0f, (contentView.top + contentView.height).toFloat())
        animation.duration = ANIMATION_DURATION.toLong()
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                super@MediaSelector.dismiss()
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })

        getContentView().startAnimation(animation)
    }

    private fun getClickOrigin(anchor: View?, contentView: View): Pair<Int, Int> {
        if (anchor == null) return Pair(0, 0)

        val anchorCoordinates = IntArray(2)
        anchor.getLocationOnScreen(anchorCoordinates)
        anchorCoordinates[0] += anchor.width / 2
        anchorCoordinates[1] += anchor.height / 2

        val contentCoordinates = IntArray(2)
        contentView.getLocationOnScreen(contentCoordinates)

        val x = anchorCoordinates[0] - contentCoordinates[0]
        val y = anchorCoordinates[1] - contentCoordinates[1]

        return Pair(x, y)
    }

    private inner class RecentPhotoSelectedListener : RecentPhotoRecyclerView.OnItemClickedListener {
        override fun onItemClicked(uri: Uri) {
            animateWindowOutTranslate(contentView)

            if (listener != null) listener!!.onQuickAttachment(uri)
        }
    }

    companion object {
        val ANIMATION_DURATION = 300
        val TYPE_CAMERA = 0
        val TYPE_GALLERY = 1
        val TYPE_VIDEO = 2
        val TYPE_TRANSFER = 3
        val TYPE_MUSIC = 4
        val TYPE_LOCATION = 5
        val TYPE_DOCUMENT = 6
    }
}
