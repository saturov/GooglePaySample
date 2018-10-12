package com.google.pay.sample

import android.app.Activity
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

object PaymentsUtil {

    private val MICROS = BigDecimal(1000000.0)

    /**
     * Converts micros to a string format accepted by [PaymentsUtil.getPaymentDataRequest].
     *
     * @param micros value of the price.
     */
    fun microsToString(micros: Long): String {
        return BigDecimal(micros).divide(MICROS).setScale(2, RoundingMode.HALF_EVEN).toString()
    }

    /**
     * Create a Google Pay API base request object with properties used in all requests.
     *
     * @return Google Pay API base request object.
     * @throws JSONException
     */
    @Throws(JSONException::class)
    private fun getBaseRequest() =
        JSONObject().put("apiVersion", 2).put("apiVersionMinor", 0)

    /**
     * Creates an instance of [PaymentsClient] for use in an [Activity] using the
     * environment and theme set in [Constants].
     *
     * @param activity is the caller's activity.
     */
    fun createPaymentsClient(activity: Activity): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder().setEnvironment(PAYMENTS_ENVIRONMENT).build()
        return Wallet.getPaymentsClient(activity, walletOptions)
    }

    /**
     * Describe your app's support for the CARD payment method.
     *
     * The provided properties are applicable to both an IsReadyToPayRequest and a
     * PaymentDataRequest.
     *
     * @return A CARD PaymentMethod object describing accepted cards.
     * @throws JSONException
     * @see [PaymentMethod](https://developers.google.com/pay/api/android/reference/object.PaymentMethod)
     */
    @Throws(JSONException::class)
    private fun getBaseCardPaymentMethod(): JSONObject {
        val cardPaymentMethod = JSONObject()
        cardPaymentMethod.put("type", "CARD")

        val parameters = JSONObject()
        parameters.put("allowedAuthMethods", getAllowedCardAuthMethods())
        parameters.put("allowedCardNetworks", getAllowedCardNetworks())
        // Optionally, you can add billing address/phone number associated with a CARD payment method.
        parameters.put("billingAddressRequired", true)

        val billingAddressParameters = JSONObject()
        billingAddressParameters.put("format", "FULL")

        parameters.put("billingAddressParameters", billingAddressParameters)

        cardPaymentMethod.put("parameters", parameters)

        return cardPaymentMethod
    }

    /**
     * Card authentication methods supported by your app and your gateway.
     *
     * @return Allowed card authentication methods.
     * @see [CardParameters](https://developers.google.com/pay/api/android/reference/object.CardParameters)
     */
    private fun getAllowedCardAuthMethods(): JSONArray {
        return JSONArray(SUPPORTED_METHODS)
    }

    /**
     * Card networks supported by your app and your gateway.
     *
     * @return Allowed card networks
     * @see [CardParameters](https://developers.google.com/pay/api/android/reference/object.CardParameters)
     */
    private fun getAllowedCardNetworks() = JSONArray(SUPPORTED_NETWORKS)

    /**
     * An object describing accepted forms of payment by your app, used to determine a viewer's
     * readiness to pay.
     *
     * @return API version and payment methods supported by the app.
     * @see [IsReadyToPayRequest](https://developers.google.com/pay/api/android/reference/object.IsReadyToPayRequest)
     */
    fun getIsReadyToPayRequest(): Optional<JSONObject> {
        return try {
            val isReadyToPayRequest = getBaseRequest()
            isReadyToPayRequest.put(
                "allowedPaymentMethods", JSONArray().put(getBaseCardPaymentMethod())
            )

            Optional.of(isReadyToPayRequest)
        } catch (e: JSONException) {
            Optional.empty()
        }

    }

    /**
     * An object describing information requested in a Google Pay payment sheet
     *
     * @return Payment data expected by your app.
     * @see [PaymentDataRequest](https://developers.google.com/pay/api/android/reference/object.PaymentDataRequest)
     */
    fun getPaymentDataRequest(price: String): Optional<JSONObject> {
        try {
            val paymentDataRequest = PaymentsUtil.getBaseRequest()
            paymentDataRequest.put(
                "allowedPaymentMethods", JSONArray().put(PaymentsUtil.getCardPaymentMethod())
            )
            paymentDataRequest.put("transactionInfo", PaymentsUtil.getTransactionInfo(price))
            paymentDataRequest.put("merchantInfo", PaymentsUtil.getMerchantInfo())

            /* An optional shipping address requirement is a top-level property of the PaymentDataRequest
      JSON object. */
            paymentDataRequest.put("shippingAddressRequired", true)

            val shippingAddressParameters = JSONObject()
            shippingAddressParameters.put("phoneNumberRequired", false)

            val allowedCountryCodes = JSONArray(SHIPPING_SUPPORTED_COUNTRIES)

            shippingAddressParameters.put("allowedCountryCodes", allowedCountryCodes)
            paymentDataRequest.put("shippingAddressParameters", shippingAddressParameters)
            return Optional.of(paymentDataRequest)
        } catch (e: JSONException) {
            return Optional.empty()
        }
    }

    /**
     * Information about the merchant requesting payment information
     *
     * @return Information about the merchant.
     * @throws JSONException
     * @see [MerchantInfo](https://developers.google.com/pay/api/android/reference/object.MerchantInfo)
     */
    @Throws(JSONException::class)
    private fun getMerchantInfo(): JSONObject {
        return JSONObject().put("merchantName", "Example Merchant")
    }

    /**
     * Provide Google Pay API with a payment amount, currency, and amount status.
     *
     * @return information about the requested payment.
     * @throws JSONException
     * @see [TransactionInfo](https://developers.google.com/pay/api/android/reference/object.TransactionInfo)
     */
    @Throws(JSONException::class)
    private fun getTransactionInfo(price: String): JSONObject {
        val transactionInfo = JSONObject()
        transactionInfo.put("totalPrice", price)
        transactionInfo.put("totalPriceStatus", "FINAL")
        transactionInfo.put("currencyCode", CURRENCY_CODE)

        return transactionInfo
    }

    /**
     * Describe the expected returned payment data for the CARD payment method
     *
     * @return A CARD PaymentMethod describing accepted cards and optional fields.
     * @throws JSONException
     * @see [PaymentMethod](https://developers.google.com/pay/api/android/reference/object.PaymentMethod)
     */
    @Throws(JSONException::class)
    private fun getCardPaymentMethod(): JSONObject {
        val cardPaymentMethod = getBaseCardPaymentMethod()
        cardPaymentMethod.put("tokenizationSpecification", getGatewayTokenizationSpecification())

        return cardPaymentMethod
    }

    /**
     * Gateway Integration: Identify your gateway and your app's gateway merchant identifier.
     *
     *
     * The Google Pay API response will return an encrypted payment method capable of being charged
     * by a supported gateway after payer authorization.
     *
     *
     * TODO: Check with your gateway on the parameters to pass and modify them in Constants.java.
     *
     * @return Payment data tokenization for the CARD payment method.
     * @throws JSONException
     * @see [PaymentMethodTokenizationSpecification](https://developers.google.com/pay/api/android/reference/object.PaymentMethodTokenizationSpecification)
     */
    @Throws(JSONException::class, RuntimeException::class)
    private fun getGatewayTokenizationSpecification(): JSONObject {
        if (PAYMENT_GATEWAY_TOKENIZATION_PARAMETERS.isEmpty()) {
            throw RuntimeException(
                "Please edit the Constants.java file to add gateway name and other parameters your " + "processor requires"
            )
        }
        val tokenizationSpecification = JSONObject()

        tokenizationSpecification.put("type", "PAYMENT_GATEWAY")
        val parameters = JSONObject(PAYMENT_GATEWAY_TOKENIZATION_PARAMETERS)
        tokenizationSpecification.put("parameters", parameters)

        return tokenizationSpecification
    }
}