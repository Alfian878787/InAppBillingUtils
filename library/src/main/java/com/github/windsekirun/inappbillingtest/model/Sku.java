package com.github.windsekirun.inappbillingtest.model;

import java.io.Serializable;

/**
 * Sku
 * Created by WindSekirun on 2017-01-05.
 */
public class Sku implements Serializable {
    /**
     * The product ID for the product.
     */
    protected String productId = "";

    /**
     * Value must be inapp for an in-app product or subs for subscriptions.
     */
    protected String type = "";

    /**
     * Formatted price of the item, including its currency sign. The price does not include tax.
     */
    protected String price = "";

    /**
     * Price in micro-units, where 1,000,000 micro-units equal one unit of the currency.
     * For example, if price is 7.99, price_amount_micros is 7990000.
     * This value represents the localized, rounded price for a particular currency.
     */
    protected String priceAmountMicros = "";

    /**
     * ISO 4217 currency code for price.
     * For example, if price is specified in British pounds sterling, price_currency_code is GBP.
     */
    protected String priceCurrencyCode = "";

    /**
     * Title of the product.
     */
    protected String title = "";

    /**
     * Description of the product.
     */
    protected String description = "";
    
     public String toString() {
        return "productId: " + productId + " type: " + type + " price: " + price + " priceAmountMicros: " + priceAmountMicros +
                " priceCurrencyCode: " + priceCurrencyCode + " title: " + title + " description: " + description;
    }

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
