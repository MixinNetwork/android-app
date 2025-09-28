package one.mixin.android.ui.setting.ui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.response.MembershipOrder
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.setting.member.MixinMemberNotificationBottomSheetDialogFragment
import one.mixin.android.ui.setting.ui.components.MembershipIcon
import one.mixin.android.ui.viewmodel.MemberViewModel

@Composable
fun MixinMemberNotificationPage(
    onClose: () -> Unit,
) {
    val viewModel: MemberViewModel = hiltViewModel()
    val order = viewModel.getOrdersFlow("f54a28b3-934a-4128-b6da-4b7323204b8c").collectAsState(null)?.value ?: return

    MixinAppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.bg_membership),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                Spacer(modifier = Modifier.height(50.dp))
                MembershipIcon(order.after, modifier = Modifier.size(70.dp))
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(if (order.after == order.before) R.string.Renew_Success else R.string.Upgrade_Success),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.W600,
                    color = MixinAppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    text = stringResource(
                        R.string.membership_congratulation_description,
                        order.createdAt.substringBefore("T"),
                        order.stars
                    ),
                    textAlign = TextAlign.Center,
                    color = MixinAppTheme.colors.textMinor,
                )
                Spacer(modifier = Modifier.height(40.dp))
                Button(
                    onClick = { onClose.invoke() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MixinAppTheme.colors.accent,
                        disabledBackgroundColor = Color.Gray
                    ),
                    elevation = ButtonDefaults.elevation(
                        pressedElevation = 0.dp,
                        defaultElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        focusedElevation = 0.dp,
                    )
                ) {
                    Text(
                        text = stringResource(R.string.Got_it),
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.View_Plan),
                    modifier = Modifier
                        .padding(5.dp)
                        .clickable {
                            onClose.invoke()
                        },
                    color = MixinAppTheme.colors.accent
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}