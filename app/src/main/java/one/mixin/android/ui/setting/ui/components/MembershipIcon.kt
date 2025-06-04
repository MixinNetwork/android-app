package one.mixin.android.ui.setting.ui.components

import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import one.mixin.android.R
import one.mixin.android.vo.Plan

@Composable
fun MembershipIcon(plan: Plan, modifier: Modifier = Modifier) {
    when (plan) {
        Plan.ADVANCE -> AdvanceIcon(modifier)
        Plan.ELITE -> EliteIcon(modifier)
        Plan.PROSPERITY -> ProsperityAnimation(modifier)
        else -> {}
    }
}

@Composable
fun MembershipIcon(plan: String, modifier: Modifier = Modifier) {
    when (plan) {
        "basic" -> AdvanceIcon(modifier)
        "standard" -> EliteIcon(modifier)
        else -> ProsperityAnimation(modifier)
    }
}

@Composable
private fun AdvanceIcon(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(id = R.drawable.ic_membership_advance),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = modifier
    )
}

@Composable
private fun EliteIcon(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(id = R.drawable.ic_membership_elite),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = modifier
    )
}

@Composable
private fun ProsperityAnimation(modifier: Modifier = Modifier) {
    val composition = rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(R.raw.prosperity)
    )
    LottieAnimation(
        composition = composition.value,
        iterations = LottieConstants.IterateForever,
        modifier = modifier
    )
}