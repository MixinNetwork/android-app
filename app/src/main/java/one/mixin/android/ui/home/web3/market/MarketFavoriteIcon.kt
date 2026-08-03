package one.mixin.android.ui.home.web3.market

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import one.mixin.android.R
import one.mixin.android.extension.dp as viewDp

@Composable
fun MarketFavoriteIcon(
    isFavored: Boolean,
    @DrawableRes unselectedIconRes: Int,
    @DrawableRes selectedIconRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    unselectedTint: Color = Color.Unspecified,
    animationTrigger: Int = 0,
) {
    val animationState =
        remember {
            MarketFavoriteAnimationState(
                initialAnimationTrigger = animationTrigger,
            )
        }
    var isPlaying by
        remember(animationTrigger) {
            mutableStateOf(animationState.shouldPlay(animationTrigger))
        }
    if (!isPlaying) {
        Icon(
            painter = painterResource(if (isFavored) selectedIconRes else unselectedIconRes),
            contentDescription = contentDescription,
            tint = if (isFavored) Color.Unspecified else unselectedTint,
            modifier = modifier.padding(1.5.dp),
        )
        return
    }
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.market_watchlist))
    val progress by
        animateLottieCompositionAsState(
            composition = composition,
            isPlaying = isPlaying,
            restartOnPlay = true,
        )
    LaunchedEffect(progress, isPlaying) {
        if (isPlaying && progress >= 1f) {
            isPlaying = false
        }
    }
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier =
            modifier.semantics {
                this.contentDescription = contentDescription
            },
    )
}

internal class MarketFavoriteAnimationState(
    initialAnimationTrigger: Int,
) {
    private var previousAnimationTrigger = initialAnimationTrigger

    fun shouldPlay(animationTrigger: Int): Boolean {
        val shouldPlay = animationTrigger != previousAnimationTrigger
        previousAnimationTrigger = animationTrigger
        return shouldPlay
    }
}

fun ImageView.setMarketFavoriteIcon(
    isFavored: Boolean,
    animate: Boolean = false,
    @DrawableRes unselectedIconRes: Int = R.drawable.ic_title_favorites,
    @DrawableRes selectedIconRes: Int = R.drawable.ic_title_favorites_checked,
    resizeToTouchTarget: Boolean = true,
) {
    val animationPadding = if (resizeToTouchTarget) 8.viewDp else 4.viewDp
    val iconPadding = if (resizeToTouchTarget) 8.viewDp else 4.viewDp
    if (resizeToTouchTarget) {
        layoutParams =
            layoutParams.apply {
                width = 40.viewDp
                height = 40.viewDp
            }
    }
    scaleType = ImageView.ScaleType.FIT_CENTER
    if (!isFavored || !animate) {
        setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
        setImageResource(if (isFavored) selectedIconRes else unselectedIconRes)
        return
    }
    setPadding(animationPadding, animationPadding, animationPadding, animationPadding)
    val composition =
        LottieCompositionFactory.fromRawResSync(context, R.raw.market_watchlist).value
            ?: run {
                setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
                setImageResource(selectedIconRes)
                return
            }
    val drawable =
        LottieDrawable().apply {
            setComposition(composition)
            repeatCount = 0
            progress = 0f
            addAnimatorListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (this@setMarketFavoriteIcon.drawable === this@apply) {
                            this@setMarketFavoriteIcon.setPadding(
                                iconPadding,
                                iconPadding,
                                iconPadding,
                                iconPadding,
                            )
                            this@setMarketFavoriteIcon.setImageResource(selectedIconRes)
                        }
                    }
                },
            )
        }
    setImageDrawable(drawable)
    drawable.playAnimation()
}
