package com.google.pay.sample

import java.math.BigDecimal
import java.math.RoundingMode

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
}