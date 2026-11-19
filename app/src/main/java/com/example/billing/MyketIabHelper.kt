package com.example.billing

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import com.android.vending.billing.IInAppBillingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class IabResult(
    val response: Int,
    val message: String
) {
    val isSuccess: Boolean get() = response == MyketIabHelper.BILLING_RESPONSE_RESULT_OK
    val isFailure: Boolean get() = !isSuccess
    override fun toString(): String = "IabResult: $message (response: ${MyketIabHelper.getResponseDesc(response)})"
}

data class MyketPurchase(
    val itemType: String,
    val originalJson: String,
    val signature: String,
    val orderId: String,
    val packageName: String,
    val sku: String,
    val purchaseTime: Long,
    val purchaseState: Int,
    val developerPayload: String,
    val token: String
) {
    companion object {
        fun fromJson(itemType: String, json: String, signature: String): MyketPurchase {
            val obj = JSONObject(json)
            return MyketPurchase(
                itemType = itemType,
                originalJson = json,
                signature = signature,
                orderId = obj.optString("orderId"),
                packageName = obj.optString("packageName"),
                sku = obj.optString("productId"),
                purchaseTime = obj.optLong("purchaseTime"),
                purchaseState = obj.optInt("purchaseState"),
                developerPayload = obj.optString("developerPayload"),
                token = obj.optString("token", obj.optString("purchaseToken"))
            )
        }
    }
}

data class MyketSkuDetails(
    val itemType: String,
    val sku: String,
    val type: String,
    val price: String,
    val title: String,
    val description: String
) {
    companion object {
        fun fromJson(itemType: String, json: String): MyketSkuDetails {
            val obj = JSONObject(json)
            return MyketSkuDetails(
                itemType = itemType,
                sku = obj.optString("productId"),
                type = obj.optString("type"),
                price = obj.optString("price"),
                title = obj.optString("title"),
                description = obj.optString("description")
            )
        }
    }
}

class MyketInventory {
    private val skuMap = mutableMapOf<String, MyketSkuDetails>()
    private val purchaseMap = mutableMapOf<String, MyketPurchase>()

    fun addSkuDetails(d: MyketSkuDetails) {
        skuMap[d.sku] = d
    }

    fun addPurchase(p: MyketPurchase) {
        purchaseMap[p.sku] = p
    }

    fun hasPurchase(sku: String): Boolean = purchaseMap.containsKey(sku)
    fun getPurchase(sku: String): MyketPurchase? = purchaseMap[sku]
    fun getSkuDetails(sku: String): MyketSkuDetails? = skuMap[sku]
    fun getAllOwnedSkus(): List<String> = purchaseMap.keys.toList()
}

data class MyketBillingState(
    val isSetupDone: Boolean = false,
    val isBillingSupported: Boolean = false,
    val isMyketInstalled: Boolean = false,
    val isPublicKeyConfigured: Boolean = MyketSecurity.isPublicKeyConfigured(),
    val isBusy: Boolean = false,
    val ownedSkus: Set<String> = emptySet(),
    val skuPrices: Map<String, String> = emptyMap(),
    val lastStatusCode: Int = MyketIabHelper.BILLING_RESPONSE_RESULT_OK,
    val lastErrorMessage: String? = null
)

/**
 * Kotlin implementation of Myket's IabHelper (In-App Billing v3 via IInAppBillingService.aidl).
 */
class MyketIabHelper(
    context: Context,
    private val base64PublicKey: String
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    private var service: IInAppBillingService? = null
    private var serviceConn: ServiceConnection? = null
    private var disposed = false
    private var setupDone = false
    private var subscriptionsSupported = false

    private var pendingPurchaseSku: String? = null
    private var pendingPurchaseItemType: String = ITEM_TYPE_INAPP
    private var pendingPurchaseListener: ((IabResult, MyketPurchase?) -> Unit)? = null

    private val _billingState = MutableStateFlow(
        MyketBillingState(isPublicKeyConfigured = MyketSecurity.isPublicKeyConfigured())
    )
    val billingState: StateFlow<MyketBillingState> = _billingState.asStateFlow()

    interface OnIabSetupFinishedListener {
        fun onIabSetupFinished(result: IabResult)
    }

    fun startSetup(listener: OnIabSetupFinishedListener?) {
        if (disposed || setupDone) return

        val serviceIntent = Intent(MYKET_BIND_ACTION).apply {
            setPackage(MYKET_PACKAGE_NAME)
        }

        val resolveList = appContext.packageManager.queryIntentServices(serviceIntent, 0)
        if (resolveList.isNullOrEmpty()) {
            _billingState.value = _billingState.value.copy(
                isSetupDone = true,
                isMyketInstalled = false,
                isBillingSupported = false,
                lastStatusCode = BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE,
                lastErrorMessage = "Myket billing service unavailable on device"
            )
            listener?.onIabSetupFinished(
                IabResult(
                    BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE,
                    "Billing service unavailable on device (Myket not installed)."
                )
            )
            return
        }

        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                if (disposed) return
                service = IInAppBillingService.Stub.asInterface(binder)
                val pkg = appContext.packageName
                try {
                    val response = service?.isBillingSupported(API_VERSION, pkg, ITEM_TYPE_INAPP)
                        ?: BILLING_RESPONSE_RESULT_ERROR
                    if (response != BILLING_RESPONSE_RESULT_OK) {
                        subscriptionsSupported = false
                        _billingState.value = _billingState.value.copy(
                            isSetupDone = true,
                            isMyketInstalled = true,
                            isBillingSupported = false,
                            lastStatusCode = response,
                            lastErrorMessage = "Error checking Myket billing v3 support"
                        )
                        listener?.onIabSetupFinished(
                            IabResult(response, "Error checking for billing v3 support.")
                        )
                        return
                    }

                    val subsResponse = service?.isBillingSupported(API_VERSION, pkg, ITEM_TYPE_SUBS)
                        ?: BILLING_RESPONSE_RESULT_ERROR
                    subscriptionsSupported = subsResponse == BILLING_RESPONSE_RESULT_OK
                    setupDone = true
                    _billingState.value = _billingState.value.copy(
                        isSetupDone = true,
                        isMyketInstalled = true,
                        isBillingSupported = true,
                        lastStatusCode = BILLING_RESPONSE_RESULT_OK,
                        lastErrorMessage = null
                    )
                    listener?.onIabSetupFinished(
                        IabResult(BILLING_RESPONSE_RESULT_OK, "Setup successful.")
                    )
                } catch (e: RemoteException) {
                    Log.e(TAG, "RemoteException while setting up Myket IAB", e)
                    _billingState.value = _billingState.value.copy(
                        isSetupDone = true,
                        isMyketInstalled = true,
                        isBillingSupported = false,
                        lastStatusCode = IABHELPER_REMOTE_EXCEPTION,
                        lastErrorMessage = e.localizedMessage
                    )
                    listener?.onIabSetupFinished(
                        IabResult(IABHELPER_REMOTE_EXCEPTION, "RemoteException while setting up billing.")
                    )
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
                setupDone = false
                _billingState.value = _billingState.value.copy(
                    isSetupDone = false,
                    isBillingSupported = false
                )
            }
        }
        serviceConn = conn

        val bound = try {
            appContext.bindService(serviceIntent, conn, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind Myket service", e)
            false
        }

        if (!bound) {
            serviceConn = null
            _billingState.value = _billingState.value.copy(
                isSetupDone = true,
                isMyketInstalled = false,
                isBillingSupported = false,
                lastStatusCode = BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE,
                lastErrorMessage = "Could not bind to Myket billing service"
            )
            listener?.onIabSetupFinished(
                IabResult(BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE, "Could not bind to Myket service.")
            )
        }
    }

    fun queryInventoryAsync(
        querySkus: List<String>,
        onComplete: (IabResult, MyketInventory?) -> Unit
    ) {
        if (disposed || !setupDone || service == null) {
            onComplete(
                IabResult(IABHELPER_NOT_INITIALIZED, "Myket billing helper is not connected."),
                null
            )
            return
        }

        _billingState.value = _billingState.value.copy(isBusy = true)
        executor.execute {
            val inv = MyketInventory()
            var result = IabResult(BILLING_RESPONSE_RESULT_OK, "Inventory refresh successful.")
            try {
                val r1 = queryPurchases(inv, ITEM_TYPE_INAPP)
                if (r1 != BILLING_RESPONSE_RESULT_OK) {
                    result = IabResult(r1, "Error refreshing in-app purchases.")
                } else {
                    querySkuDetails(inv, ITEM_TYPE_INAPP, querySkus)
                    if (subscriptionsSupported) {
                        queryPurchases(inv, ITEM_TYPE_SUBS)
                        querySkuDetails(inv, ITEM_TYPE_SUBS, querySkus)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Inventory query exception", e)
                result = IabResult(IABHELPER_UNKNOWN_ERROR, e.localizedMessage ?: "Inventory error")
            }

            mainHandler.post {
                val prices = mutableMapOf<String, String>()
                querySkus.forEach { sku ->
                    inv.getSkuDetails(sku)?.let { details ->
                        if (details.price.isNotBlank()) prices[sku] = details.price
                    }
                }
                _billingState.value = _billingState.value.copy(
                    isBusy = false,
                    ownedSkus = inv.getAllOwnedSkus().toSet(),
                    skuPrices = prices
                )
                onComplete(result, if (result.isSuccess) inv else null)
            }
        }
    }

    fun preparePurchaseIntentSender(
        sku: String,
        itemType: String = ITEM_TYPE_INAPP,
        developerPayload: String = "",
        onIntentReady: (IntentSender?) -> Unit,
        onFinished: (IabResult, MyketPurchase?) -> Unit
    ) {
        val currentService = service
        if (disposed || !setupDone || currentService == null) {
            val res = IabResult(BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE, "Myket service not connected.")
            onIntentReady(null)
            onFinished(res, null)
            return
        }

        _billingState.value = _billingState.value.copy(isBusy = true)
        executor.execute {
            try {
                val buyIntentBundle = currentService.getBuyIntent(
                    API_VERSION,
                    appContext.packageName,
                    sku,
                    itemType,
                    developerPayload
                )
                val response = getResponseCodeFromBundle(buyIntentBundle)
                if (response != BILLING_RESPONSE_RESULT_OK) {
                    mainHandler.post {
                        _billingState.value = _billingState.value.copy(
                            isBusy = false,
                            lastStatusCode = response,
                            lastErrorMessage = getResponseDesc(response)
                        )
                        onIntentReady(null)
                        onFinished(IabResult(response, "Unable to buy item (${getResponseDesc(response)})"), null)
                    }
                    return@execute
                }

                @Suppress("DEPRECATION")
                val pendingIntent = buyIntentBundle.getParcelable<PendingIntent>(RESPONSE_BUY_INTENT)
                if (pendingIntent == null) {
                    mainHandler.post {
                        _billingState.value = _billingState.value.copy(isBusy = false)
                        onIntentReady(null)
                        onFinished(IabResult(IABHELPER_SEND_INTENT_FAILED, "Null PendingIntent from Myket."), null)
                    }
                    return@execute
                }

                mainHandler.post {
                    pendingPurchaseSku = sku
                    pendingPurchaseItemType = itemType
                    pendingPurchaseListener = onFinished
                    onIntentReady(pendingIntent.intentSender)
                }
            } catch (e: RemoteException) {
                Log.e(TAG, "RemoteException on getBuyIntent for $sku", e)
                mainHandler.post {
                    _billingState.value = _billingState.value.copy(
                        isBusy = false,
                        lastStatusCode = IABHELPER_REMOTE_EXCEPTION,
                        lastErrorMessage = e.localizedMessage
                    )
                    onIntentReady(null)
                    onFinished(IabResult(IABHELPER_REMOTE_EXCEPTION, "RemoteException launching purchase."), null)
                }
            }
        }
    }

    fun handleActivityResult(resultCode: Int, data: Intent?) {
        val listener = pendingPurchaseListener
        val itemType = pendingPurchaseItemType
        pendingPurchaseListener = null
        pendingPurchaseSku = null
        _billingState.value = _billingState.value.copy(isBusy = false)

        if (listener == null) return

        if (data == null) {
            listener(IabResult(IABHELPER_BAD_RESPONSE, "Null data in Myket IAB result"), null)
            return
        }

        val responseCode = getResponseCodeFromIntent(data)
        val purchaseData = data.getStringExtra(RESPONSE_INAPP_PURCHASE_DATA)
        val dataSignature = data.getStringExtra(RESPONSE_INAPP_SIGNATURE)

        if (resultCode == android.app.Activity.RESULT_OK && responseCode == BILLING_RESPONSE_RESULT_OK) {
            if (purchaseData == null || dataSignature == null) {
                listener(IabResult(IABHELPER_UNKNOWN_ERROR, "IAB returned null purchaseData or dataSignature"), null)
                return
            }
            try {
                val purchase = MyketPurchase.fromJson(itemType, purchaseData, dataSignature)
                if (base64PublicKey.isNotEmpty() &&
                    !MyketSecurity.verifyPurchase(base64PublicKey, purchaseData, dataSignature)
                ) {
                    listener(
                        IabResult(IABHELPER_VERIFICATION_FAILED, "Signature verification failed for ${purchase.sku}"),
                        purchase
                    )
                    return
                }
                _billingState.value = _billingState.value.copy(
                    ownedSkus = _billingState.value.ownedSkus + purchase.sku,
                    lastStatusCode = BILLING_RESPONSE_RESULT_OK,
                    lastErrorMessage = null
                )
                listener(IabResult(BILLING_RESPONSE_RESULT_OK, "Success"), purchase)
            } catch (e: Exception) {
                listener(IabResult(IABHELPER_BAD_RESPONSE, "Failed to parse purchase data."), null)
            }
        } else if (resultCode == android.app.Activity.RESULT_OK) {
            listener(IabResult(responseCode, "Problem purchasing item: ${getResponseDesc(responseCode)}"), null)
        } else if (resultCode == android.app.Activity.RESULT_CANCELED) {
            listener(IabResult(IABHELPER_USER_CANCELLED, "User canceled."), null)
        } else {
            listener(IabResult(IABHELPER_UNKNOWN_PURCHASE_RESPONSE, "Unknown purchase response."), null)
        }
    }

    private fun queryPurchases(inv: MyketInventory, itemType: String): Int {
        val currentService = service ?: return IABHELPER_NOT_INITIALIZED
        var continueToken: String? = null
        var verificationFailed = false
        do {
            val ownedItems = currentService.getPurchases(
                API_VERSION,
                appContext.packageName,
                itemType,
                continueToken
            )
            val response = getResponseCodeFromBundle(ownedItems)
            if (response != BILLING_RESPONSE_RESULT_OK) return response

            val ownedSkus = ownedItems.getStringArrayList(RESPONSE_INAPP_ITEM_LIST) ?: arrayListOf()
            val purchaseDataList = ownedItems.getStringArrayList(RESPONSE_INAPP_PURCHASE_DATA_LIST) ?: arrayListOf()
            val signatureList = ownedItems.getStringArrayList(RESPONSE_INAPP_SIGNATURE_LIST) ?: arrayListOf()

            for (i in purchaseDataList.indices) {
                val purchaseData = purchaseDataList[i]
                val signature = signatureList.getOrNull(i) ?: ""
                val valid = if (base64PublicKey.isNotEmpty()) {
                    MyketSecurity.verifyPurchase(base64PublicKey, purchaseData, signature)
                } else {
                    false
                }
                if (valid) {
                    val purchase = MyketPurchase.fromJson(itemType, purchaseData, signature)
                    inv.addPurchase(purchase)
                } else {
                    Log.w(TAG, "Purchase verification failed for ${ownedSkus.getOrNull(i)}")
                    verificationFailed = true
                }
            }
            continueToken = ownedItems.getString(INAPP_CONTINUATION_TOKEN)
        } while (!continueToken.isNullOrEmpty())

        return if (verificationFailed) IABHELPER_VERIFICATION_FAILED else BILLING_RESPONSE_RESULT_OK
    }

    private fun querySkuDetails(inv: MyketInventory, itemType: String, skus: List<String>): Int {
        val currentService = service ?: return IABHELPER_NOT_INITIALIZED
        if (skus.isEmpty()) return BILLING_RESPONSE_RESULT_OK

        val queryBundle = Bundle().apply {
            putStringArrayList(GET_SKU_DETAILS_ITEM_LIST, ArrayList(skus))
        }
        val skuDetails = currentService.getSkuDetails(API_VERSION, appContext.packageName, itemType, queryBundle)
        if (!skuDetails.containsKey(RESPONSE_GET_SKU_DETAILS_LIST)) {
            return getResponseCodeFromBundle(skuDetails)
        }
        val responseList = skuDetails.getStringArrayList(RESPONSE_GET_SKU_DETAILS_LIST) ?: return BILLING_RESPONSE_RESULT_OK
        for (json in responseList) {
            inv.addSkuDetails(MyketSkuDetails.fromJson(itemType, json))
        }
        return BILLING_RESPONSE_RESULT_OK
    }

    fun dispose() {
        disposed = true
        setupDone = false
        serviceConn?.let { conn ->
            try {
                appContext.unbindService(conn)
            } catch (_: Exception) {
            }
        }
        serviceConn = null
        service = null
        pendingPurchaseListener = null
        executor.shutdownNow()
    }

    private fun getResponseCodeFromBundle(b: Bundle?): Int {
        if (b == null) return IABHELPER_BAD_RESPONSE
        val o = b.get(RESPONSE_CODE) ?: return BILLING_RESPONSE_RESULT_OK
        return when (o) {
            is Int -> o
            is Long -> o.toInt()
            else -> IABHELPER_BAD_RESPONSE
        }
    }

    private fun getResponseCodeFromIntent(i: Intent): Int {
        val o = i.extras?.get(RESPONSE_CODE) ?: return BILLING_RESPONSE_RESULT_OK
        return when (o) {
            is Int -> o
            is Long -> o.toInt()
            else -> IABHELPER_BAD_RESPONSE
        }
    }

    companion object {
        private const val TAG = "MyketIabHelper"
        const val API_VERSION = 3

        // Myket Market package & bind action from official docs
        const val MYKET_PACKAGE_NAME = "ir.mservices.market"
        const val MYKET_BIND_ACTION = "ir.mservices.market.InAppBillingService.BIND"

        // Product SKUs for Meow Pro
        const val SKU_PRO_MONTHLY = "meow_pro_monthly"
        const val SKU_PRO_LIFETIME = "meow_pro_lifetime"

        const val ITEM_TYPE_INAPP = "inapp"
        const val ITEM_TYPE_SUBS = "subs"

        const val BILLING_RESPONSE_RESULT_OK = 0
        const val BILLING_RESPONSE_RESULT_USER_CANCELED = 1
        const val BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3
        const val BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE = 4
        const val BILLING_RESPONSE_RESULT_DEVELOPER_ERROR = 5
        const val BILLING_RESPONSE_RESULT_ERROR = 6
        const val BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7
        const val BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED = 8

        const val IABHELPER_REMOTE_EXCEPTION = -1001
        const val IABHELPER_BAD_RESPONSE = -1002
        const val IABHELPER_VERIFICATION_FAILED = -1003
        const val IABHELPER_SEND_INTENT_FAILED = -1004
        const val IABHELPER_USER_CANCELLED = -1005
        const val IABHELPER_UNKNOWN_PURCHASE_RESPONSE = -1006
        const val IABHELPER_NOT_INITIALIZED = -1007
        const val IABHELPER_UNKNOWN_ERROR = -1008

        private const val RESPONSE_CODE = "RESPONSE_CODE"
        private const val RESPONSE_GET_SKU_DETAILS_LIST = "DETAILS_LIST"
        private const val RESPONSE_BUY_INTENT = "BUY_INTENT"
        private const val RESPONSE_INAPP_PURCHASE_DATA = "INAPP_PURCHASE_DATA"
        private const val RESPONSE_INAPP_SIGNATURE = "INAPP_DATA_SIGNATURE"
        private const val RESPONSE_INAPP_ITEM_LIST = "INAPP_PURCHASE_ITEM_LIST"
        private const val RESPONSE_INAPP_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST"
        private const val RESPONSE_INAPP_SIGNATURE_LIST = "INAPP_DATA_SIGNATURE_LIST"
        private const val INAPP_CONTINUATION_TOKEN = "INAPP_CONTINUATION_TOKEN"
        private const val GET_SKU_DETAILS_ITEM_LIST = "ITEM_ID_LIST"

        fun getResponseDesc(code: Int): String = when (code) {
            BILLING_RESPONSE_RESULT_OK -> "OK"
            BILLING_RESPONSE_RESULT_USER_CANCELED -> "User Canceled"
            BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE -> "Billing Unavailable"
            BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE -> "Item Unavailable"
            BILLING_RESPONSE_RESULT_DEVELOPER_ERROR -> "Developer Error"
            BILLING_RESPONSE_RESULT_ERROR -> "Error"
            BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED -> "Item Already Owned"
            BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED -> "Item Not Owned"
            IABHELPER_REMOTE_EXCEPTION -> "Remote Exception"
            IABHELPER_BAD_RESPONSE -> "Bad Response"
            IABHELPER_VERIFICATION_FAILED -> "Signature Verification Failed"
            IABHELPER_USER_CANCELLED -> "User Cancelled"
            else -> "Code $code"
        }
    }
}
