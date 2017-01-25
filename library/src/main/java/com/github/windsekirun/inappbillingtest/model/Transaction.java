package com.github.windsekirun.inappbillingtest.model;

import java.io.Serializable;

/**
 * Transaction
 * Created by WindSekirun on 2017-01-05.
 */
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
    protected String autoRenewing;

    /**
     * A unique order identifier for the transaction.
     * This identifier corresponds to the Google payments order ID.
     * If the order is a test purchase made through the In-app Billing Sandbox, orderId is blank.
     */
    protected String orderId;

    /**
     * The application package from which the purchase originated.
     */
    protected String packageName;

    /**
     * The item's product identifier.
     * Every item has a product ID, which you must specify in the application's product list on the Google Play developer console.
     */
    protected String productId;

    /**
     * The time the product was purchased, in milliseconds since the epoch (Jan 1, 1970).
     */
    protected long purchaseTime;

    /**
     * The purchase state of the order. Possible values are 0 (purchased), 1 (canceled), or 2 (refunded).
     */
    protected int purchaseState;

    /**
     * A developer-specified string that contains supplemental information about an order. You can specify a value for this field when you make a getBuyIntent request.
     */
    protected String developerPayload;

    /**
     * A token that uniquely identifies a purchase for a given item and user pair.
     */
    protected String purchaseToken;

    /**
     * Full-string of Transaction
     */
    protected String purchaseInfo;

    /**
     * Data Signature value to execute valid-check processing
     */
    protected String dataSignature;

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
