package com.google.pay.sample

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.*
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException
import org.json.JSONObject

class CheckoutActivity : Activity() {

    //a client for interacting with the Google Pay API.
    private var paymentsClient: PaymentsClient? = null

    private val bikeItem = ItemInfo("Simple Bike", 300 * 1000000, R.drawable.bike)
    private val shippingCost = (90 * 1000000).toLong()

    /**
     * Arbitrarily-picked constant integer you define to track a request for payment data activity.
     *
     * @value #LOAD_PAYMENT_DATA_REQUEST_CODE
     */
    private val LOAD_PAYMENT_DATA_REQUEST_CODE = 991

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        initPaymentsClient()
        possiblyShowGooglePayButton()
        googlepay_button.setOnClickListener { requestPayment() }
    }

    /**
     * Handle a resolved activity from the Google Pay payment sheet.
     *
     * @param requestCode Request code originally supplied to AutoResolveHelper in requestPayment().
     * @param resultCode Result code returned by the Google Pay API.
     * @param data Intent from the Google Pay API containing payment or error data.
     * @see [Getting a result
     * from an Activity](https://developer.android.com/training/basics/intents/result)
     */
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            // value passed in AutoResolveHelper
            LOAD_PAYMENT_DATA_REQUEST_CODE -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        val paymentData = PaymentData.getFromIntent(data)
                        handlePaymentSuccess(paymentData!!)
                    }
                    Activity.RESULT_CANCELED -> {
                    }
                    AutoResolveHelper.RESULT_ERROR -> {
                        val status = AutoResolveHelper.getStatusFromIntent(data)
                        handleError(status!!.statusCode)
                    }
                }// Nothing to here normally - the user simply cancelled without selecting a
                // payment method.
                // Do nothing.

                // Re-enables the Google Pay payment button.
                googlepay_button.isClickable = true
            }
        }
    }

    private fun initViews() {
        text_item_name.text = bikeItem.name
        image_item_image.setImageResource(bikeItem.imageResourceId)
        text_item_price.text = PaymentsUtil.microsToString(bikeItem.priceMicros)
    }

    private fun initPaymentsClient() {
        paymentsClient = PaymentsUtil.createPaymentsClient(this)
    }

    /**
     * Determine the viewer's ability to pay with a payment method supported by your app and display a
     * Google Pay payment button.
     *
     * @see [](https://developers.google.com/android/reference/com/google/android/gms/wallet/PaymentsClient.html.isReadyToPay
    ) */
    private fun possiblyShowGooglePayButton() {
        val isReadyToPayJson = PaymentsUtil.getIsReadyToPayRequest()
        if (!isReadyToPayJson.isPresent) {
            return
        }
        val request = IsReadyToPayRequest.fromJson(isReadyToPayJson.get().toString()) ?: return

        // The call to isReadyToPay is asynchronous and returns a Task. We need to provide an
        // OnCompleteListener to be triggered when the result of the call is known.
        val task = paymentsClient?.isReadyToPay(request)
        task?.addOnCompleteListener { task1 ->
            try {
                val result = task1.getResult(ApiException::class.java)!!
                setGooglePayAvailable(result)
            } catch (exception: ApiException) {
                // Process error
                Log.w("isReadyToPay failed", exception)
            }
        }
    }

    /**
     * If isReadyToPay returned `true`, show the button and hide the "checking" text. Otherwise,
     * notify the user that Google Pay is not available. Please adjust to fit in with your current
     * user flow. You are not required to explicitly let the user know if isReadyToPay returns `false`.
     *
     * @param available isReadyToPay API response.
     */
    private fun setGooglePayAvailable(available: Boolean) {
        if (available) {
            googlepay_status.visibility = View.GONE
            googlepay_button.visibility = View.VISIBLE
        } else {
            googlepay_status.setText(R.string.googlepay_status_unavailable)
        }
    }

    // This method is called when the Pay with Google button is clicked.
    fun requestPayment() {
        // Disables the button to prevent multiple clicks.
        googlepay_button.isClickable = false

        // The price provided to the API should include taxes and shipping.
        // This price is not displayed to the user.
        val price = PaymentsUtil.microsToString(bikeItem.priceMicros + shippingCost)

        // TransactionInfo transaction = PaymentsUtil.createTransaction(price);
        val paymentDataRequestJson = PaymentsUtil.getPaymentDataRequest(price)
        if (!paymentDataRequestJson.isPresent) {
            return
        }
        val request = PaymentDataRequest.fromJson(paymentDataRequestJson.get().toString())

        // Since loadPaymentData may show the UI asking the user to select a payment method, we use
        // AutoResolveHelper to wait for the user interacting with it. Once completed,
        // onActivityResult will be called with the result.
        if (request != null) {
            paymentsClient?.loadPaymentData(request)?.let {
                AutoResolveHelper.resolveTask<PaymentData>(
                    it, this, LOAD_PAYMENT_DATA_REQUEST_CODE
                )
            }
        }
    }

    /**
     * PaymentData response object contains the payment information, as well as any additional
     * requested information, such as billing and shipping address.
     *
     * @param paymentData A response object returned by Google after a payer approves payment.
     * @see [Payment
     * Data](https://developers.google.com/pay/api/android/reference/object.PaymentData)
     */
    private fun handlePaymentSuccess(paymentData: PaymentData) {
        val paymentInformation = paymentData.toJson() ?: return

        // Token will be null if PaymentDataRequest was not constructed using fromJson(String).
        val paymentMethodData: JSONObject

        try {
            paymentMethodData = JSONObject(paymentInformation).getJSONObject("paymentMethodData")
            // If the gateway is set to "example", no payment information is returned - instead, the
            // token will only consist of "examplePaymentMethodToken".
            if (paymentMethodData
                    .getJSONObject("tokenizationData")
                    .getString("type") == "PAYMENT_GATEWAY" && paymentMethodData
                    .getJSONObject("tokenizationData")
                    .getString("token") == "examplePaymentMethodToken"
            ) {
                val alertDialog = AlertDialog.Builder(this)
                    .setTitle("Warning")
                    .setMessage(
                        "Gateway name set to \"example\" - please modify " + "Constants.java and replace it with your own gateway."
                    )
                    .setPositiveButton("OK", null)
                    .create()
                alertDialog.show()
            }

            val billingName = paymentMethodData.getJSONObject("info").getJSONObject("billingAddress").getString("name")
            Log.d("BillingName", billingName)
            Toast.makeText(this, getString(R.string.payments_show_name, billingName), Toast.LENGTH_LONG)
                .show()

            // Logging token string.
            Log.d("GooglePaymentToken", paymentMethodData.getJSONObject("tokenizationData").getString("token"))
        } catch (e: JSONException) {
            Log.e("handlePaymentSuccess", "Error: " + e.toString())
            return
        }

    }

    /**
     * At this stage, the user has already seen a popup informing them an error occurred. Normally,
     * only logging is required.
     *
     * @param statusCode will hold the value of any constant from CommonStatusCode or one of the
     * WalletConstants.ERROR_CODE_* constants.
     * @see [
     * Wallet Constants Library](https://developers.google.com/android/reference/com/google/android/gms/wallet/WalletConstants.constant-summary)
     */
    private fun handleError(statusCode: Int) {
        Log.w("loadPaymentData failed", String.format("Error code: %d", statusCode))
    }
}