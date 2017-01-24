package com.github.windsekirun.inappbillingtest;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import com.android.vending.billing.IInAppBillingService;
import com.github.windsekirun.inappbillingtest.model.Sku;
import com.github.windsekirun.inappbillingtest.model.Transaction;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

/**
 * InAppBillingUtils
 * Created by WindSekirun on 2017-01-05.
 */
public class InAppBillingUtils {
    private Activity activity;
    private OnInAppBillingCallback callback;
    private String developerPayload = "";
    private String signatureBase64;

    @SuppressWarnings("unused") public static final int PURCHASE_SUCCESS = 0;
    private static final int PURCHASE_FAILED_UNKNOWN = -1;
    private static final int PURCHASE_FAILED_INVALID = -2;

    public InAppBillingUtils(Activity activity, String licenseKey, OnInAppBillingCallback callback) {
        this.activity = activity;
        this.callback = callback;
        this.signatureBase64 = licenseKey;
    }

    private IInAppBillingService mService;

    private ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);
        }
    };

    /**
     * Should be initialized in onCreate
     */
    public void initInAppBilling() {
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        activity.bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
    }

    /**
     * Should be initialized in onDestroy
     */
    public void unBindInAppBilling() {
        if (mService != null) {
           activity.unbindService(mServiceConn);
        }
    }

    /**
     * Purchase specific 'inapp' item, developerPayload generated automatically
     * @param productId to purchase
     */
    @SuppressWarnings("unused")
    public void purchase(String productId) {
        purchase(productId, "inapp");
    }

    /**
     * Purchase specific item, developerPayload generated automatically.
     * @param productId to purchase
     * @param type inapp or sub
     */
    public void purchase(String productId, String type) {
        String developerPayload = generateDeveloperPayload(productId, type);
        purchase(productId, type, developerPayload);
    }

    /**
     * Purchase specific item
     * @param productId to purchase
     * @param type inapp or sub
     * @param developerPayload for secure working.
     */
    public void purchase(String productId, String type, String developerPayload) {
        Bundle buyIntentBundle = null;
        try {
            buyIntentBundle = mService.getBuyIntent(3, activity.getPackageName(), productId, type, developerPayload);
        } catch (RemoteException e) {
            callback.purchaseFailed(PURCHASE_FAILED_UNKNOWN);
        }
        this.developerPayload = developerPayload;

        if (buyIntentBundle != null) {
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            if (pendingIntent != null) {
                try {
                    activity.startIntentSenderForResult(pendingIntent.getIntentSender(), 1001, new Intent(), 0, 0, 0);
                } catch (Exception e) {
                    callback.purchaseFailed(PURCHASE_FAILED_UNKNOWN);
                }
            } else {
                callback.purchaseFailed(PURCHASE_FAILED_UNKNOWN);
            }
        }

    }

    /**
     * Purchase Result callback, just pass Activity's onActivityResult.
     * @param requestCode requestcode
     * @param resultCode resultCode
     * @param data intent
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data)  {
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK && data != null) {
            String jsonStr = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
            Transaction transaction = new Transaction();

            try {
                JSONObject object = new JSONObject(jsonStr);
                if (object.getString("developerPayload").equals(developerPayload)) {
                    transaction.setDataSignature(dataSignature);
                    transaction.setPurchaseInfo(jsonStr);
                    transaction.setOrderId(getJString(object, "orderId"));
                    transaction.setPackageName(getJString(object, "packageName"));
                    transaction.setProductId(getJString(object, "productId"));
                    transaction.setPurchaseTime(object.has("purchaseTime") ? object.getLong("purchaseTime") : 0);
                    transaction.setPurchaseState(object.has("purchaseState") ? object.getInt("purchaseState") : 0);
                    transaction.setDeveloperPayload(getJString(object, "developerPayload"));
                    transaction.setPurchaseToken(getJString(object, "purchaseToken"));

                    if (isValidTransaction(transaction)) {
                        callback.purchaseDone(transaction);
                    } else {
                        callback.purchaseFailed(PURCHASE_FAILED_INVALID);
                    }
                } else {
                    callback.purchaseFailed(PURCHASE_FAILED_INVALID);
                }
            } catch (Exception e) {
                callback.purchaseFailed(PURCHASE_FAILED_UNKNOWN);
            }
        } else {
            callback.purchaseFailed(PURCHASE_FAILED_UNKNOWN);
        }
    }

    /**
     * get Available 'inapp' package of own package name.
     *
     * @param items Strings of item's id
     * @return ArrayList of {@link Sku sku}
     */
    public ArrayList<Sku> getAvailableInappPackage(ArrayList<String> items) {
        ArrayList<Sku> skuArrayList = new ArrayList<>();
        Bundle querySkus = new Bundle();
        querySkus.putStringArrayList("ITEM_ID_LIST", items);
        try {
            Bundle skuDetails = mService.getSkuDetails(3, activity.getPackageName(), "inapp", querySkus);

            int response = skuDetails.getInt("RESPONSE_CODE");
            if (response == 0) {
                ArrayList<String> responseList = skuDetails.getStringArrayList("DETAILS_LIST");
                if (responseList != null) {
                    for (String thisResponse : responseList) {
                        Sku sku = new Sku();
                        JSONObject object = new JSONObject(thisResponse);
                        sku.setDescription(getJString(object, "description"));
                        sku.setTitle(getJString(object, "title"));
                        sku.setPrice(getJString(object, "price"));
                        sku.setPriceAmountMicros(getJString(object, "price_amount_micros"));
                        sku.setPriceCurrencyCode(getJString(object, "price_currency_code"));
                        sku.setType(getJString(object, "type"));
                        sku.setProductId(getJString(object, "productId"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return skuArrayList;
    }

    /**
     * generate developerpayload using nonce(UUID)
     * @param productId productId of product
     * @param type inapp or sub
     * @return random generated developerPayload
     */
    @SuppressWarnings("StringBufferReplaceableByString")
    public String generateDeveloperPayload(String productId, String type) {
        StringBuilder builder = new StringBuilder();
        builder.append(productId)
                .append(":")
                .append(type)
                .append(":")
                .append(UUID.randomUUID().toString().replaceAll("-", ""));

        return builder.toString();
    }

    /**
     * Checking transaction is valid.
     * @param transaction transcation object
     * @return true - valid, false - invalid
     */
    public boolean isValidTransaction(Transaction transaction) {
        return verifyPurchaseSignature(transaction.getProductId(), transaction.getPurchaseInfo(), transaction.getDataSignature());
    }

    /**
     * getting value of JSONObject
     * @param object JSONObject object
     * @param jsonName extract key-value
     * @return value, if key is available, it will be return value. otherwise, it will return "" (empty)
     */
    private String getJString(JSONObject object, String jsonName) {
        try {
            return (object.has(jsonName) ? object.getString(jsonName) : "");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * verify signature using BASE64
     * @param productId productId
     * @param purchaseData purchaseData (full-jsonStr)
     * @param dataSignature signature key
     * @return value, true - valid, false - invalid
     */
    private boolean verifyPurchaseSignature(String productId, String purchaseData, String dataSignature) {
        try {
            return TextUtils.isEmpty(signatureBase64) || Security.verifyPurchase(productId, signatureBase64, purchaseData, dataSignature);
        } catch (Exception e) {
            return false;
        }
    }
    public interface OnInAppBillingCallback {
        void purchaseDone(Transaction transaction);
        void purchaseFailed(int responseCode);
    }
}