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
import android.util.Base64;
import android.util.Log;

import com.android.vending.billing.IInAppBillingService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.UUID;

/**
 * InAppBillingUtils
 * Created by WindSekirun on 2017-01-05.
 */
@SuppressWarnings("unused")
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
     * Purchase specific 'inapp' item, developerPayload generated automatically
     *
     * @param productId to puirchase
     * @throws RemoteException
     * @throws IntentSender.SendIntentException
     */
    public void purchase(String productId) throws RemoteException, IntentSender.SendIntentException {
        purchase(productId, "inapp");
    }

    /**
     * Purchase specific item, developerPayload generated automatically.
     *
     * @param productId to purchase
     * @param type      inapp or sub
     * @throws RemoteException
     * @throws IntentSender.SendIntentException
     */
    public void purchase(String productId, String type) throws RemoteException, IntentSender.SendIntentException {
        String developerPayload = generateDeveloperPayload(productId, type);
        purchase(productId, type, developerPayload);
    }

    /**
     * Purchase specific item
     *
     * @param productId        to purchase
     * @param type             inapp or sub
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
     *
     * @param requestCode requestcode
     * @param resultCode  resultCode
     * @param data        intent
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
     * @throws JSONException   JSION
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
     * generate developerpayload using nonce(UUID)
     *
     * @param productId productId of product
     * @param type      inapp or sub
     * @return random generated developerPayload
     */
    @SuppressWarnings("StringBufferReplaceableByString")
    private String generateDeveloperPayload(String productId, String type) {
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
     *
     * @param transaction transcation object
     * @return true - valid, false - invalid
     */
    public boolean isValidTransaction(Transaction transaction) {
        return verifyPurchaseSignature(transaction.getProductId(), transaction.getPurchaseInfo(), transaction.getDataSignature());
    }

    /**
     * getting value of JSONObject
     *
     * @param object   JSONObject object
     * @param jsonName extract key-value
     * @return value, if key is available, it will be return value. otherwise, it will return "" (empty)
     * @throws JSONException
     */
    private String getJString(JSONObject object, String jsonName) throws JSONException {
        return (object.has(jsonName) ? object.getString(jsonName) : "");
    }

    /**
     * verify signature using BASE64
     *
     * @param productId     productId
     * @param purchaseData  purchaseData (full-jsonStr)
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

    /**
     * Security-related methods. For a secure implementation, all of this code
     * should be implemented on a server that communicates with the
     * application on the device. For the sake of simplicity and clarity of this
     * example, this code is included here and is executed on the device. If you
     * must verify the purchases on the phone, you should obfuscate this code to
     * make it harder for an attacker to replace the code with stubs that treat all
     * purchases as verified.
     */
    public static class Security {
        private static final String TAG = "IABUtil/Security";

        private static final String KEY_FACTORY_ALGORITHM = "RSA";
        private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";

        /**
         * Verifies that the data was signed with the given signature, and returns
         * the verified purchase. The data is in JSON format and signed
         * with a private key. The data also contains the purchaseState
         * and product ID of the purchase.
         *
         * @param productId       the product Id used for debug validation.
         * @param base64PublicKey the base64-encoded public key to use for verifying.
         * @param signedData      the signed JSON string (signed, not encrypted)
         * @param signature       the signature for the data, signed with the private key
         */
        public static boolean verifyPurchase(String productId, String base64PublicKey, String signedData, String signature) {
            if (TextUtils.isEmpty(signedData) || TextUtils.isEmpty(base64PublicKey) ||
                    TextUtils.isEmpty(signature)) {
                if (productId.equals("android.test.purchased") || productId.equals("android.test.canceled") ||
                        productId.equals("android.test.refunded") || productId.equals("android.test.item_unavailable")) {
                    return true;
                }

                Log.e(TAG, "Purchase verification failed: missing data.");
                return false;
            }

            PublicKey key = Security.generatePublicKey(base64PublicKey);
            return Security.verify(key, signedData, signature);
        }

        /**
         * Generates a PublicKey instance from a string containing the
         * Base64-encoded public key.
         *
         * @param encodedPublicKey Base64-encoded public key
         * @throws IllegalArgumentException if encodedPublicKey is invalid
         */
        public static PublicKey generatePublicKey(String encodedPublicKey) {
            try {
                byte[] decodedKey = Base64.decode(encodedPublicKey, Base64.DEFAULT);
                KeyFactory keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
                return keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (InvalidKeySpecException e) {
                Log.e(TAG, "Invalid key specification.");
                throw new IllegalArgumentException(e);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Base64 decoding failed.");
                throw e;
            }
        }

        /**
         * Verifies that the signature from the server matches the computed
         * signature on the data.  Returns true if the data is correctly signed.
         *
         * @param publicKey  public key associated with the developer account
         * @param signedData signed data from server
         * @param signature  server signature
         * @return true if the data and signature match
         */
        public static boolean verify(PublicKey publicKey, String signedData, String signature) {
            Signature sig;
            try {
                sig = Signature.getInstance(SIGNATURE_ALGORITHM);
                sig.initVerify(publicKey);
                sig.update(signedData.getBytes());
                if (!sig.verify(Base64.decode(signature, Base64.DEFAULT))) {
                    Log.e(TAG, "Signature verification failed.");
                    return false;
                }
                return true;
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "NoSuchAlgorithmException.");
            } catch (InvalidKeyException e) {
                Log.e(TAG, "Invalid key specification.");
            } catch (SignatureException e) {
                Log.e(TAG, "Signature exception.");
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Base64 decoding failed.");
            }
            return false;
        }
    }

    @SuppressWarnings("unused")
    public class Sku implements Serializable {
        /**
         * The product ID for the product.
         */
        private String productId = "";

        /**
         * Value must be “inapp” for an in-app product or "subs" for subscriptions.
         */
        private String type = "";

        /**
         * Formatted price of the item, including its currency sign. The price does not include tax.
         */
        private String price = "";

        /**
         * Price in micro-units, where 1,000,000 micro-units equal one unit of the currency.
         * For example, if price is "€7.99", price_amount_micros is "7990000".
         * This value represents the localized, rounded price for a particular currency.
         */
        private String priceAmountMicros = "";

        /**
         * ISO 4217 currency code for price.
         * For example, if price is specified in British pounds sterling, price_currency_code is "GBP".
         */
        private String priceCurrencyCode = "";

        /**
         * Title of the product.
         */
        private String title = "";

        /**
         * Description of the product.
         */
        private String description = "";

        /**
         * Signature of Purchase, BASE64 encoded
         */
        private String dataSignature = "";

        public String getPriceAmountMicros() {
            return priceAmountMicros;
        }

        public String getProductId() {
            return productId;
        }

        public String getDescription() {
            return description;
        }

        public String getPrice() {
            return price;
        }

        public String getPriceCurrencyCode() {
            return priceCurrencyCode;
        }

        public String getTitle() {
            return title;
        }

        public String getType() {
            return type;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setPrice(String price) {
            this.price = price;
        }

        public void setPriceAmountMicros(String priceAmountMicros) {
            this.priceAmountMicros = priceAmountMicros;
        }

        public void setPriceCurrencyCode(String priceCurrencyCode) {
            this.priceCurrencyCode = priceCurrencyCode;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    @SuppressWarnings("unused")
    public class Transaction implements Serializable {
        /**
         * Indicates whether the subscription renews automatically.
         * If true, the subscription is active, and will automatically renew on the next billing date.
         * If false, indicates that the user has canceled the subscription.
         * The user has access to subscription content until the next billing date and will lose access at
         * that time unless they re-enable automatic renewal (or manually renew, as described in Manual Renewal).
         * If you offer a grace period, this value remains set to true for all subscriptions,
         * as long as the grace period has not lapsed.
         * The next billing date is extended dynamically every day until the end of the grace period or until the user fixes their payment method.
         */
        private String autoRenewing;

        /**
         * A unique order identifier for the transaction.
         * This identifier corresponds to the Google payments order ID.
         * If the order is a test purchase made through the In-app Billing Sandbox, orderId is blank.
         */
        private String orderId;

        /**
         * The application package from which the purchase originated.
         */
        private String packageName;

        /**
         * The item's product identifier.
         * Every item has a product ID, which you must specify in the application's product list on the Google Play developer console.
         */
        private String productId;

        /**
         * The time the product was purchased, in milliseconds since the epoch (Jan 1, 1970).
         */
        private long purchaseTime;

        /**
         * The purchase state of the order. Possible values are 0 (purchased), 1 (canceled), or 2 (refunded).
         */
        private int purchaseState;

        /**
         * A developer-specified string that contains supplemental information about an order. You can specify a value for this field when you make a getBuyIntent request.
         */
        private String developerPayload;

        /**
         * A token that uniquely identifies a purchase for a given item and user pair.
         */
        private String purchaseToken;

        /**
         * Full-string of Transaction
         */
        private String purchaseInfo;

        private String dataSignature;

        public String getDataSignature() {
            return dataSignature;
        }

        public void setDataSignature(String dataSignature) {
            this.dataSignature = dataSignature;
        }

        public String getAutoRenewing() {
            return autoRenewing;
        }

        public String getDeveloperPayload() {
            return developerPayload;
        }

        public String getOrderId() {
            return orderId;
        }

        public String getPackageName() {
            return packageName;
        }

        public String getProductId() {
            return productId;
        }

        public String getPurchaseInfo() {
            return purchaseInfo;
        }

        public int getPurchaseState() {
            return purchaseState;
        }

        public long getPurchaseTime() {
            return purchaseTime;
        }

        public String getPurchaseToken() {
            return purchaseToken;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public void setAutoRenewing(String autoRenewing) {
            this.autoRenewing = autoRenewing;
        }

        public void setDeveloperPayload(String developerPayload) {
            this.developerPayload = developerPayload;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public void setPurchaseInfo(String purchaseInfo) {
            this.purchaseInfo = purchaseInfo;
        }

        public void setPurchaseState(int purchaseState) {
            this.purchaseState = purchaseState;
        }

        public void setPurchaseTime(long purchaseTime) {
            this.purchaseTime = purchaseTime;
        }

        public void setPurchaseToken(String purchaseToken) {
            this.purchaseToken = purchaseToken;
        }
    }
}