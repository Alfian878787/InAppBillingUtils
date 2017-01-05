package com.github.windsekirun.inappbillingtest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.android.vending.billing.IInAppBillingService;
import com.github.windsekirun.inappbillingtest.model.Sku;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * InAppBillingUtils
 * Created by WindSekirun on 2017-01-05.
 */
public class InAppBillingUtils {
    Activity activity;
    public InAppBillingUtils(Activity activity) {
        this.activity = activity;
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
}
