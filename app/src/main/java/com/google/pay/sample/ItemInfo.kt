package com.google.pay.sample

/**
 * Used for storing the (hard coded) info about the item we're selling.
 *
 * Micros are used for prices to avoid rounding errors when converting between currencies.
 */
data class ItemInfo(
    val name: String,
    var priceMicros: Long = 0,
    val imageResourceId: Int
)