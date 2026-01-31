package com.paliapp.ecommerce.data.model

data class CartItem(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val qty: Int = 0,
    val imageUrl: String = ""
)
