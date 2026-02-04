package com.paliapp.ecommerce.data.model

import com.google.firebase.firestore.PropertyName

data class CartItem(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("price") @set:PropertyName("price") var price: Double = 0.0,
    @get:PropertyName("qty") @set:PropertyName("qty") var qty: Int = 0,
    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl") var imageUrl: String = "",
    @get:PropertyName("isReturnable") @set:PropertyName("isReturnable") var isReturnable: Boolean = false,
    @get:PropertyName("returnWindowDays") @set:PropertyName("returnWindowDays") var returnWindowDays: Int = 0,
    @get:PropertyName("status") @set:PropertyName("status") var status: String = "DELIVERED",
    @get:PropertyName("returnQty") @set:PropertyName("returnQty") var returnQty: Int = 0
)
