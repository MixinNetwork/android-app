package one.mixin.android.ui.setting.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.Plan

@Composable
fun MemberSection(plan: Plan) {
    val features = when (plan) {
        Plan.ADVANCE -> listOf(
            stringResource(id = R.string.member_desc_mixin_safe, 5),
            stringResource(id = R.string.member_desc_mixin_star, 5),
            stringResource(id = R.string.member_desc_safe_members, 5, 10),
            stringResource(id = R.string.member_desc_recovery_paid, 2)
        )

        Plan.ELITE -> listOf(
            stringResource(id = R.string.member_desc_mixin_safe, 5),
            stringResource(id = R.string.member_desc_mixin_star, 5),
            stringResource(id = R.string.member_desc_safe_members, 5, 10),
            stringResource(id = R.string.member_desc_recovery_free)
        )

        Plan.PROSPERITY -> listOf(
            stringResource(id = R.string.member_desc_mixin_safe, 100),
            stringResource(id = R.string.member_desc_mixin_star, 2),
            stringResource(id = R.string.member_desc_safe_members, 10, 100),
            stringResource(id = R.string.member_desc_recovery_free)
        )

        else -> listOf()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardBackground(
                backgroundColor = MixinAppTheme.colors.background,
                borderColor = MixinAppTheme.colors.borderColor
            )
            .padding(vertical = 24.dp, horizontal = 20.dp),
    ) {
        features.forEachIndexed { index, feature ->
            val iconRes = if (index < icons.size) icons[index] else icons[0]
            val title = when (index) {
                0 -> stringResource(id = R.string.member_title_mixin_safe)
                1 -> stringResource(id = R.string.member_title_mixin_star)
                2 -> stringResource(id = R.string.safe_members)
                3 -> if (plan == Plan.ADVANCE)
                    stringResource(id = R.string.member_title_recovery_paid)
                else
                    stringResource(id = R.string.free_recovery_service)

                else -> ""
            }
            MemberRow(title = title, text = feature, iconRes = iconRes)

            if (index < features.size - 1) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private val icons = listOf(
    R.drawable.ic_interests_safe,
    R.drawable.ic_interests_star,
    R.drawable.ic_interests_members,
    R.drawable.ic_interests_paid
)
