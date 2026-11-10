package com.expeditee.plant.identifier.model

import com.dqh.lmtech.ads.billing.AppPurchase
import com.expeditee.plant.identifier.R

enum class Premium(val type: Int, val id: String) {
    Weekly(AppPurchase.TYPE_IAP.SUBSCRIPTION, "weekly"),
    Yearly(AppPurchase.TYPE_IAP.SUBSCRIPTION, "yearly"),

    WEEKLY_ONBOARDING(AppPurchase.TYPE_IAP.SUBSCRIPTION, "weekly_onboarding"),
    YEARLY_ONBOARDING(AppPurchase.TYPE_IAP.SUBSCRIPTION, "yearly_onboarding"),
}
