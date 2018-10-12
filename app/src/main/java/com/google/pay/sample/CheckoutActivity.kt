package com.google.pay.sample

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.*
import kotlinx.android.synthetic.main.activity_main.*

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
        googlepay_button.setOnClickListener { view -> requestPayment(view) }
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
    fun requestPayment(view: View) {
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
}