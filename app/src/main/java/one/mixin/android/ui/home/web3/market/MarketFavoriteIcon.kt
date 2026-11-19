package one.mixin.android.ui.home.web3.market

import android.widget.ImageView
import androidx.annotation.DrawableRes
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
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import one.mixin.android.R
import one.mixin.android.extension.dp as viewDp

@Composable
fun MarketFavoriteIcon(
    isFavored: Boolean,
    @DrawableRes unselectedIconRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    unselectedTint: Color = Color.Unspecified,
    animationIntent: MarketFavoriteAnimationIntent? = null,
    onAnimationFinished: (Int) -> Unit = {},
) {
    if (animationIntent == null) {
        if (isFavored) {
            Icon(
                painter = painterResource(R.drawable.ic_title_favorites_checked),
                contentDescription = contentDescription,
                tint = Color.Unspecified,
                modifier = modifier,
            )
        } else {
            Icon(
                painter = painterResource(unselectedIconRes),
                contentDescription = contentDescription,
                tint = unselectedTint,
                modifier = modifier,
            )
        }
        return
    }

    val animationState =
        remember {
            MarketFavoriteAnimationState(
                initialFavored = isFavored,
            )
        }
    var completedAnimationIntentId by remember { mutableStateOf<Int?>(null) }
    val decision =
        remember(isFavored, animationIntent, completedAnimationIntentId) {
            animationState.update(isFavored, animationIntent)
        }
    val shouldAnimate =
        decision.mode == MarketFavoriteAnimationMode.ANIMATE_FORWARD &&
            decision.intentId != completedAnimationIntentId
    val displayFavored = decision.targetProgress == 1f

    if (shouldAnimate) {
        AnimatedMarketFavoriteLottie(
            decision = decision,
            contentDescription = contentDescription,
            modifier = modifier,
            onFinished = { intentId ->
                animationState.onAnimationFinished(intentId)
                completedAnimationIntentId = intentId
                intentId?.let(onAnimationFinished)
            },
        )
    } else if (displayFavored) {
        Icon(
            painter = painterResource(R.drawable.ic_title_favorites_checked),
            contentDescription = contentDescription,
            tint = Color.Unspecified,
            modifier = modifier,
        )
    } else {
        Icon(
            painter = painterResource(unselectedIconRes),
            contentDescription = contentDescription,
            tint = unselectedTint,
            modifier = modifier,
        )
    }
}

@Composable
private fun AnimatedMarketFavoriteLottie(
    decision: MarketFavoriteAnimationDecision,
    contentDescription: String,
    modifier: Modifier,
    onFinished: (Int?) -> Unit,
) {
    val compositionResult = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.market_watchlist))
    val composition by compositionResult
    val animatable = rememberLottieAnimatable()
    LaunchedEffect(composition, compositionResult.isFailure, decision.intentId) {
        val currentComposition =
            composition ?: run {
                if (compositionResult.isFailure) {
                    onFinished(decision.intentId)
                }
                return@LaunchedEffect
            }
        animatable.animate(
            composition = currentComposition,
            initialProgress = 0f,
        )
        onFinished(decision.intentId)
    }
    val iconModifier =
        modifier.semantics {
            this.contentDescription = contentDescription
        }
    if (composition != null) {
        LottieAnimation(
            composition = composition,
            progress = { animatable.progress },
            modifier = iconModifier,
        )
    } else {
        Icon(
            painter = painterResource(R.drawable.ic_title_favorites_checked),
            contentDescription = contentDescription,
            tint = Color.Unspecified,
            modifier = iconModifier,
        )
    }
}

data class MarketFavoriteAnimationIntent(
    val id: Int,
    val targetFavored: Boolean,
)

internal fun shouldClearFavoriteAnimationIntent(
    intent: MarketFavoriteAnimationIntent?,
    requestResult: Boolean?,
    isFavored: Boolean,
    completedIntentId: Int?,
): Boolean {
    val currentIntent = intent ?: return false
    if (requestResult == false) return true
    if (requestResult != true || isFavored != currentIntent.targetFavored) return false
    return !currentIntent.targetFavored || completedIntentId == currentIntent.id
}

internal enum class MarketFavoriteAnimationMode {
    SNAP,
    ANIMATE_FORWARD,
}

internal data class MarketFavoriteAnimationDecision(
    val mode: MarketFavoriteAnimationMode,
    val targetProgress: Float,
    val intentId: Int? = null,
)

internal class MarketFavoriteAnimationState(
    initialFavored: Boolean,
) {
    private var authoritativeFavored = initialFavored
    private var latestIntent: MarketFavoriteAnimationIntent? = null
    private var activeAnimationIntentId: Int? = null

    fun update(
        isFavored: Boolean,
        intent: MarketFavoriteAnimationIntent?,
    ): MarketFavoriteAnimationDecision {
        authoritativeFavored = isFavored
        if (intent == null) {
            latestIntent = null
            activeAnimationIntentId = null
            return snapTo(authoritativeFavored)
        }
        if (intent.id != latestIntent?.id) {
            latestIntent = intent
            activeAnimationIntentId = if (intent.targetFavored) intent.id else null
            return if (intent.targetFavored) {
                MarketFavoriteAnimationDecision(
                    mode = MarketFavoriteAnimationMode.ANIMATE_FORWARD,
                    targetProgress = 1f,
                    intentId = intent.id,
                )
            } else {
                snapTo(false)
            }
        }
        return if (activeAnimationIntentId == intent.id) {
            MarketFavoriteAnimationDecision(
                mode = MarketFavoriteAnimationMode.ANIMATE_FORWARD,
                targetProgress = 1f,
                intentId = intent.id,
            )
        } else {
            snapTo(intent.targetFavored)
        }
    }

    fun onAnimationFinished(intentId: Int?) {
        if (activeAnimationIntentId == intentId) {
            activeAnimationIntentId = null
        }
    }

    private fun snapTo(isFavored: Boolean) =
        MarketFavoriteAnimationDecision(
            mode = MarketFavoriteAnimationMode.SNAP,
            targetProgress = if (isFavored) 1f else 0f,
        )
}

fun ImageView.setMarketFavoriteIcon(
    isFavored: Boolean,
    animate: Boolean = false,
    @DrawableRes unselectedIconRes: Int = R.drawable.ic_title_favorites,
) {
    imageTintList = null
    layoutParams =
        layoutParams.apply {
            width = 40.viewDp
            height = 40.viewDp
        }
    scaleType = ImageView.ScaleType.FIT_CENTER
    if (!isFavored) {
        setTag(R.id.market_favorite_icon_request, null)
        setImageResource(unselectedIconRes)
        return
    }
    val request = Any()
    setTag(R.id.market_favorite_icon_request, request)
    setImageResource(R.drawable.ic_title_favorites_checked)
    LottieCompositionFactory
        .fromRawRes(context, R.raw.market_watchlist)
        .addListener { composition ->
            if (getTag(R.id.market_favorite_icon_request) !== request) return@addListener
            val drawable =
                LottieDrawable().apply {
                    setComposition(composition)
                    repeatCount = 0
                    progress = if (animate) 0f else 1f
                }
            setImageDrawable(drawable)
            if (animate) {
                drawable.playAnimation()
            }
        }
}
