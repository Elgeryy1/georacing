package com.georacing.georacing.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object BillingManager {
    private var billingClient: BillingClient? = null
    
    private val _purchases = MutableStateFlow<List<Purchase>>(emptyList())
    val purchases = _purchases.asStateFlow()

    private val _billingFlowResult = MutableStateFlow<Int?>(null) // BillingResponseCode
    val billingFlowResult = _billingFlowResult.asStateFlow()

    private val _simulationEvent = MutableStateFlow<Boolean>(false)
    val simulationEvent = _simulationEvent.asStateFlow()

    fun consumeSimulation() {
        _simulationEvent.value = false
    }

    fun initialize(context: Context) {
        if (billingClient != null) return

        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    _purchases.value = purchases
                    // Here we would acknowledge purchases normally
                    acknowledgePurchases(purchases)
                }
                _billingFlowResult.value = billingResult.responseCode
            }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()

        startConnection()
    }

    private fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Client is ready
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to Google Play
                // For now, we rely on reconnection logic in usage
            }
        })
    }

    private fun acknowledgePurchases(purchases: List<Purchase>) {
        val scope = CoroutineScope(Dispatchers.IO)
        purchases.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    scope.launch {
                        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                             // Handle acknowledgment result
                             if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                 // Purchase acknowledged
                             }
                        }
                    }
                }
            }
        }
    }

    fun launchBillingFlow(activity: Activity, skuId: String, price: Double) {
        if (billingClient?.isReady != true) {
            startConnection()
            activity.runOnUiThread {
                android.widget.Toast.makeText(activity, "Conectando... Inténtalo de nuevo.", android.widget.Toast.LENGTH_SHORT).show()
            }
            return
        }

        val skuList = listOf(QueryProductDetailsParams.Product.newBuilder()
            .setProductId("android.test.purchased") 
            .setProductType(BillingClient.ProductType.INAPP)
            .build())

        val params = QueryProductDetailsParams.newBuilder().setProductList(skuList).build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                        )
                    )
                    .build()
                
                billingClient?.launchBillingFlow(activity, billingFlowParams)
            } else {
                // Fallback for testing without Play Console setup
                activity.runOnUiThread {
                    android.widget.Toast.makeText(
                        activity, 
                        "Modo Test: Simulando Pago (Google Play no req.)", 
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                // Simulate success
                _simulationEvent.value = true
            }
        }
    }
    
    fun cleanFlow() {
        _billingFlowResult.value = null
    }
}
