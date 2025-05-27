package one.mixin.android.ui.setting.ui.page

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.response.Plan as ApiPlan
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.setting.ui.components.HeaderSection
import one.mixin.android.ui.setting.ui.components.MemberSection
import one.mixin.android.ui.setting.ui.components.PlanSelector
import one.mixin.android.ui.setting.ui.components.ProfileSection
import one.mixin.android.ui.viewmodel.MemberViewModel
import one.mixin.android.vo.Plan
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.api.request.MemberOrderRequest
import timber.log.Timber

@Composable
fun MixinMemberUpgradePage(
    onClose: () -> Unit,
    onUrlGenerated: (String) -> Unit
) {
    val viewModel: MemberViewModel = hiltViewModel()
    var selectedPlan by remember { mutableStateOf(Plan.ADVANCE) }
    var selectedPlanData by remember { mutableStateOf<ApiPlan?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingPlans by remember { mutableStateOf(true) }
    var plansData by remember { mutableStateOf<List<ApiPlan>>(emptyList()) }
    var transactionAssetId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoadingPlans = true
        try {
            val response = viewModel.getPlans()
            if (response.isSuccess && response.data != null) {
                plansData = response.data!!.plans
                transactionAssetId = response.data!!.transaction.assetId

                selectedPlanData = mapLocalPlanToApiPlan(selectedPlan, plansData)
                Timber.d("Plans loaded: ${plansData.size}, selected: ${selectedPlanData?.name}")
            }
        } finally {
            isLoadingPlans = false
        }
    }

    LaunchedEffect(selectedPlan, plansData) {
        if (plansData.isNotEmpty()) {
            selectedPlanData = mapLocalPlanToApiPlan(selectedPlan, plansData)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(id = R.string.mixin_one),
                fontSize = 18.sp,
                fontWeight = FontWeight.W700,
                color = MixinAppTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_circle_close),
                    tint = Color.Unspecified,
                    contentDescription = stringResource(id = R.string.close)
                )
            }
        }

        PlanSelector(
            selectedPlan = selectedPlan,
            onPlanSelected = { plan ->
                selectedPlan = plan
            }
        )
        Spacer(modifier = Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {

            HeaderSection(plan = selectedPlan)
            Spacer(modifier = Modifier.height(10.dp))
            ProfileSection(plan = selectedPlan)
            Spacer(modifier = Modifier.height(10.dp))
            MemberSection(plan = selectedPlan)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 30.dp)) {
            Button(
                onClick = {
                    if (selectedPlanData == null) return@Button

                    isLoading = true
                    viewModel.viewModelScope.launch {
                        try {
                            val planId = selectedPlanData?.plan ?: return@launch
                            val assetId = transactionAssetId ?: "4d8c508b-91c5-375b-92b0-ee702ed2dac5"

                            val orderRequest = MemberOrderRequest(
                                plan = planId,
                                asset = assetId
                            )

                            val orderResponse = viewModel.createMemberOrder(orderRequest)
                            if (orderResponse.isSuccess && orderResponse.data != null) {
                                onUrlGenerated(orderResponse.data!!.paymentUrl ?: "")
                            }
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading && !isLoadingPlans && selectedPlanData != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF3478F6))
            ) {
                if (isLoading || isLoadingPlans) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 4.dp)
                    )
                } else {
                    val priceText = selectedPlanData?.let { "${it.amountPayment} USDT" } ?: stringResource(id = R.string.Upgrade)
                    Text(
                        text = priceText,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun mapLocalPlanToApiPlan(localPlan: Plan, apiPlans: List<ApiPlan>): ApiPlan? {
    return when (localPlan) {
        Plan.ADVANCE -> apiPlans.find { it.name == "basic" }
        Plan.ELITE -> apiPlans.find { it.name == "standard" }
        Plan.PROSPERITY -> apiPlans.find { it.name == "premium" }
        else -> apiPlans.find { it.name == "basic" }
    }
}

@Preview
@Composable
private fun MixinMemberUpgradePagePreview() {
    MixinAppTheme {
        MixinMemberUpgradePage({}, {})
    }
}
