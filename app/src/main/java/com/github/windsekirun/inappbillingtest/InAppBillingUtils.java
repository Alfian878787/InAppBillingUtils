package com.github.windsekirun.inappbillingtest;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import com.android.vending.billing.IInAppBillingService;
import com.github.windsekirun.inappbillingtest.model.Sku;
import com.github.windsekirun.inappbillingtest.model.Transaction;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * InAppBillingUtils
 * Created by WindSekirun on 2017-01-05.
 */
public class InAppBillingUtils {
    private Activity activity;
    private OnInAppBillingCallback callback;
    private String developerPayload = "";
    private String signatureBase64;

    public static final int PURCHASE_SUCCESS = 0;
    public static final int PURCHASE_FAILED_UNKNOWN = -1;
    public static final int PURCHASE_FAILED_INVALID = -2;

    public InAppBillingUtils(Activity activity, String licenseKey, OnInAppBillingCallback callback) {
        this.activity = activity;
        this.callback = callback;
        this.signatureBase64 = licenseKey;
    }

    IInAppBillingService mService;

    ServiceConnection mServiceConn = new ServiceConnection() {
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
     * Purchase specific item
     * @param productId to purchase
     * @param type inapp or sub
     * @param developerPayload for secure working.
     * @throws RemoteException
     * @throws IntentSender.SendIntentException
     */
    public void purchase(String productId, String type, String developerPayload) throws RemoteException, IntentSender.SendIntentException {
        Bundle buyIntentBundle = mService.getBuyIntent(3, activity.getPackageName(), productId, type, developerPayload);
        this.developerPayload = developerPayload;
        PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
        if (pendingIntent != null) {
            activity.startIntentSenderForResult(pendingIntent.getIntentSender(), 1001, new Intent(), 0, 0, 0);
        } else {
            callback.purchaseFailed(PURCHASE_FAILED_UNKNOWN);
        }
    }

    /**
     * Purchase Result callback, just pass Activity's onActivityResult.
     * @param requestCode requestcode
     * @param resultCode resultCode
     * @param data intent
     * @throws JSONException
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) throws JSONException {
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK && data != null) {
            String jsonStr = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
            Transaction transaction = new Transaction();

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
        } else {
            callback.purchaseFailed(PURCHASE_FAILED_UNKNOWN);
        }
    }

    /**
     * get Available 'inapp' package of own package name.
     *
     * @param items Strings of item's id
     * @return ArrayList of {@link Sku sku}
     * @throws RemoteException processing failed
     * @throws JSONException JSION
     */
    public ArrayList<Sku> getAvailableInappPackage(ArrayList<String> items) throws RemoteException, JSONException {
        ArrayList<Sku> skuArrayList = new ArrayList<>();
        Bundle querySkus = new Bundle();
        querySkus.putStringArrayList("ITEM_ID_LIST", items);
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

        return skuArrayList;
    }

    /**
     * Checking transaction is valid.
     * @param transaction
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
     * @throws JSONException
     */
    private String getJString(JSONObject object, String jsonName) throws JSONException {
        return (object.has(jsonName) ? object.getString(jsonName) : "");
    }


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
