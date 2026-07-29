package one.mixin.android.ui.home.web3.market

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
) {
    var previousIsFavored by remember { mutableStateOf(isFavored) }
    var isPlaying by remember(isFavored) { mutableStateOf(isFavored && !previousIsFavored) }
    SideEffect {
        previousIsFavored = isFavored
    }
    if (!isPlaying) {
        Icon(
            painter = painterResource(if (isFavored) selectedIconRes else unselectedIconRes),
            contentDescription = contentDescription,
            tint = Color.Unspecified,
            modifier = modifier.padding(1.dp),
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

fun ImageView.setMarketFavoriteIcon(
    isFavored: Boolean,
    animate: Boolean = false,
    @DrawableRes unselectedIconRes: Int = R.drawable.ic_title_favorites,
    @DrawableRes selectedIconRes: Int = R.drawable.ic_title_favorites_checked,
) {
    layoutParams =
        layoutParams.apply {
            width = 40.viewDp
            height = 40.viewDp
    }
    scaleType = ImageView.ScaleType.FIT_CENTER
    if (!isFavored || !animate) {
        setPadding(1.viewDp, 1.viewDp, 1.viewDp, 1.viewDp)
        setImageResource(if (isFavored) selectedIconRes else unselectedIconRes)
        return
    }
    setPadding(0, 0, 0, 0)
    val composition =
        LottieCompositionFactory.fromRawResSync(context, R.raw.market_watchlist).value
            ?: run {
                setPadding(1.viewDp, 1.viewDp, 1.viewDp, 1.viewDp)
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
                            this@setMarketFavoriteIcon.setPadding(1.viewDp, 1.viewDp, 1.viewDp, 1.viewDp)
                            this@setMarketFavoriteIcon.setImageResource(selectedIconRes)
                        }
                    }
                },
            )
        }
    setImageDrawable(drawable)
    drawable.playAnimation()
}
