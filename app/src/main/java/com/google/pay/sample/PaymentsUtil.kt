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
}