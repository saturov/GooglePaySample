package com.google.pay.sample

import android.app.Activity
import android.os.Bundle
import com.google.android.gms.wallet.PaymentsClient
import kotlinx.android.synthetic.main.activity_main.*

class CheckoutActivity : Activity() {

    //a client for interacting with the Google Pay API.
    private var paymentsClient: PaymentsClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
    }

    private fun initViews() {
        text_item_name.text = bikeItem.name
        image_item_image.setImageResource(bikeItem.imageResourceId)
        text_item_price.text = PaymentsUtil.microsToString(bikeItem.priceMicros)
    }
}