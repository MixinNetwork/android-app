package one.mixin.android.ui.setting.ui.page

import one.mixin.android.BuildConfig
import one.mixin.android.api.response.MemberOrderPlan
import one.mixin.android.vo.Plan

fun isPlanAvailableInGooglePlay(
    plan: Plan,
    availablePlans: List<MemberOrderPlan>,
    availablePlayStorePlans: Set<String>
): Boolean {
    if (!BuildConfig.IS_GOOGLE_PLAY) return true

    val memberPlan = mapLocalPlanToMemberOrderPlan(plan, availablePlans) ?: return false
    return memberPlan.playStoreSubscriptionId?.let {
        availablePlayStorePlans.contains(it)
    } ?: false
}

fun mapLocalPlanToMemberOrderPlan(
    localPlan: Plan,
    memberOrderPlans: List<MemberOrderPlan>
): MemberOrderPlan? {
    return when (localPlan) {
        Plan.ADVANCE -> memberOrderPlans.find { it.name == "basic" }
        Plan.ELITE -> memberOrderPlans.find { it.name == "standard" }
        Plan.PROSPERITY -> memberOrderPlans.find { it.name == "premium" }
        else -> memberOrderPlans.find { it.name == "basic" }
    }
}

fun getPlanFromOrderAfter(after: String?): Plan? {
    return when (after) {
        "basic" -> Plan.ADVANCE
        "standard" -> Plan.ELITE
        "premium" -> Plan.PROSPERITY
        else -> null
    }
}
