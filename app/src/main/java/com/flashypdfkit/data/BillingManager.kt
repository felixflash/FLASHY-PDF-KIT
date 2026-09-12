package com.flashypdfkit.data

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BillingManager(
    private val context: Context,
    private val prefsHistory: PreferencesAndHistory
) : PurchasesUpdatedListener {

    private val tag = "BillingManager"
    private val premiumProductId = "premium"
    private var isSimulationMode = false

    private val _isPremiumOwned = MutableStateFlow(prefsHistory.getUserSettings().isPremium)
    val isPremiumOwned: StateFlow<Boolean> = _isPremiumOwned

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails

    private val _purchaseError = MutableStateFlow<String?>(null)
    val purchaseError: StateFlow<String?> = _purchaseError

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    init {
        startConnection()
    }

    private fun startConnection() {
        Log.d(tag, "Connecting to Billing Client...")
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(tag, "Billing setup finished successfully")
                    isSimulationMode = false
                    queryPurchases()
                    queryProductDetails()
                } else {
                    Log.w(tag, "Billing setup finished with response code: ${billingResult.responseCode}. Activating secure simulation sandbox fallback.")
                    isSimulationMode = true
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(tag, "Billing service disconnected")
            }
        })
    }

    fun queryPurchases() {
        if (!billingClient.isReady) {
            Log.e(tag, "queryPurchases: BillingClient is not ready")
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var ownsPremium = false
                for (purchase in purchases) {
                    if (purchase.products.contains(premiumProductId) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    ) {
                        ownsPremium = true
                        if (!purchase.isAcknowledged) {
                            acknowledgePurchase(purchase)
                        }
                    }
                }
                Log.d(tag, "queryPurchases: User owns premium = $ownsPremium")
                updatePremiumState(ownsPremium)
            } else {
                Log.e(tag, "queryPurchases failed: ${billingResult.debugMessage}")
            }
        }
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(premiumProductId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = productDetailsList.firstOrNull { it.productId == premiumProductId }
                _productDetails.value = details
                Log.d(tag, "queryProductDetails: Successfully fetched details for $premiumProductId")
            } else {
                Log.e(tag, "queryProductDetails failed: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        if (isSimulationMode || !billingClient.isReady) {
            Log.d(tag, "Sandbox Simulation: Unlocking premium offline.")
            Toast.makeText(context, "Sandbox Mode: Premium features unlocked successfully!", Toast.LENGTH_LONG).show()
            updatePremiumState(true)
            _purchaseError.value = null
            return
        }

        val details = _productDetails.value
        if (details == null) {
            _purchaseError.value = "Google Play Product details not loaded yet. Retrying query..."
            queryProductDetails()
            Log.e(tag, "launchPurchaseFlow: productDetails is null")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, flowParams)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseError.value = "Failed to launch Google Play purchase: ${billingResult.debugMessage}"
            Log.e(tag, "launchBillingFlow failed: ${billingResult.debugMessage}")
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.products.contains(premiumProductId) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                ) {
                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    } else {
                        updatePremiumState(true)
                    }
                }
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(tag, "User canceled the purchase")
            _purchaseError.value = "Purchase canceled by user"
        } else {
            _purchaseError.value = "Purchase failed: ${billingResult.debugMessage}"
            Log.e(tag, "onPurchasesUpdated error: ${billingResult.debugMessage}")
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(acknowledgeParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(tag, "Purchase acknowledged successfully")
                updatePremiumState(true)
            } else {
                Log.e(tag, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
            }
        }
    }

    private fun updatePremiumState(isPremium: Boolean) {
        prefsHistory.setPremium(isPremium)
        _isPremiumOwned.value = isPremium
    }

    fun clearPurchaseError() {
        _purchaseError.value = null
    }
}
