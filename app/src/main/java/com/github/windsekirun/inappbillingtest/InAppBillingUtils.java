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
    Activity activity;
    OnInAppBillingCallback callback;
    String developerPayload = "";

    public InAppBillingUtils(Activity activity, OnInAppBillingCallback callback) {
        this.activity = activity;
        this.callback = callback;
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
            callback.purchaseFailed(-1);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) throws JSONException {
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK && data != null) {
            String jsonStr = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
            Transaction transaction = new Transaction();

            JSONObject object = new JSONObject(jsonStr);
            if (object.getString("developerPayload") == developerPayload) {

            } else {
                callback.purchaseFailed(-1);
            }
        } else {
            callback.purchaseFailed(-1);
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
     * getting value of JSONObject
     * @param object JSONObject object
     * @param jsonName extract key-value
     * @return value, if key is available, it will be return value. otherwise, it will return "" (empty)
     * @throws JSONException
     */
    private String getJString(JSONObject object, String jsonName) throws JSONException {
        return (object.has(jsonName) ? object.getString(jsonName) : "");
    }

    public interface OnInAppBillingCallback {
        void purchaseDone(Transaction transaction);
        void purchaseFailed(int responseCode);
    }
}
