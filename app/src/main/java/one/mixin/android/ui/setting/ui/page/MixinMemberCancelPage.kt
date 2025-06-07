package one.mixin.android.ui.setting.ui.page

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
import one.mixin.android.api.response.MemberOrder
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.setting.ui.components.MembershipIcon
import one.mixin.android.ui.viewmodel.MemberViewModel

@Composable
fun MixinMemberCancelPage(
    order: MemberOrder,
    onClose: () -> Unit,
) {
    val viewModel: MemberViewModel = hiltViewModel()
    var isLoading by remember { mutableStateOf(false) }

    MixinAppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        )
        {
            Spacer(modifier = Modifier.height(50.dp))
            Box(modifier = Modifier.size(70.dp)) {
                MembershipIcon(order.after, modifier = Modifier.size(70.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_membership_cancel),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.BottomEnd),
                    tint = Color.Unspecified
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.not_paid),
                fontSize = 20.sp,
                fontWeight = FontWeight.W600,
                color = MixinAppTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                modifier = Modifier.padding(horizontal = 10.dp),
                text = stringResource(R.string.not_paid_description),
                textAlign = TextAlign.Center,
                color = MixinAppTheme.colors.textMinor,
            )
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = {
                    if (!isLoading) {
                        isLoading = true
                        viewModel.viewModelScope.launch {
                            try {
                                val r = viewModel.cancelOrder(order.orderId)
                                if (r.isSuccess) {
                                    viewModel.insertOrders(r.data!!)
                                }
                                onClose()
                            } catch (e: Exception) {
                                // Handle error if needed
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (isLoading) Color.Gray else Color(0xFFDB454F),
                    disabledBackgroundColor = Color.Gray
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.Cancel_Waiting),
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.Keep_Waiting),
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