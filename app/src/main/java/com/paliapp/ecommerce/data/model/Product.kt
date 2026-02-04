package com.paliapp.ecommerce.data.model

import com.google.firebase.firestore.PropertyName

data class Product(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("price") @set:PropertyName("price") var price: Double = 0.0,
    @get:PropertyName("unit") @set:PropertyName("unit") var unit: String = "kg",
    @get:PropertyName("stock") @set:PropertyName("stock") var stock: Int = 0,
    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl") var imageUrl: String = "",
    @get:PropertyName("imageUrls") @set:PropertyName("imageUrls") var imageUrls: List<String> = emptyList(),
    @get:PropertyName("categoryId") @set:PropertyName("categoryId") var categoryId: String = "",
    @get:PropertyName("active") @set:PropertyName("active") var active: Boolean = true,
    @get:PropertyName("isReturnable") @set:PropertyName("isReturnable") var isReturnable: Boolean = false,
    @get:PropertyName("returnWindowDays") @set:PropertyName("returnWindowDays") var returnWindowDays: Int = 0
)
