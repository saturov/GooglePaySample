package com.google.pay.sample

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import kotlinx.android.synthetic.main.activity_main.*

class CheckoutActivity : Activity() {

    //a client for interacting with the Google Pay API.
    private var paymentsClient: PaymentsClient? = null

    private val bikeItem = ItemInfo("Simple Bike", 300 * 1000000, R.drawable.bike)
    private val shippingCost = (90 * 1000000).toLong()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        initPaymentsClient()
        possiblyShowGooglePayButton();
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
}