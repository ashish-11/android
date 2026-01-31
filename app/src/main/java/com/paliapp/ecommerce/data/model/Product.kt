package com.paliapp.ecommerce.data.model

data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val unit: String = "kg", // kg, bag, litre, pkt etc.
    val stock: Int = 0,
    val imageUrl: String = "",
    val active: Boolean = true
)
