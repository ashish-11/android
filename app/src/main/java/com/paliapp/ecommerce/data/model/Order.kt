package com.paliapp.ecommerce.data.model

import com.google.firebase.firestore.PropertyName

data class Order(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("userId") @set:PropertyName("userId") var userId: String = "",
    @get:PropertyName("userName") @set:PropertyName("userName") var userName: String = "",
    @get:PropertyName("userEmail") @set:PropertyName("userEmail") var userEmail: String = "",
    @get:PropertyName("userMobile") @set:PropertyName("userMobile") var userMobile: String = "",
    @get:PropertyName("items") @set:PropertyName("items") var items: List<CartItem> = emptyList(),
    @get:PropertyName("totalAmount") @set:PropertyName("totalAmount") var totalAmount: Double = 0.0,
    @get:PropertyName("status") @set:PropertyName("status") var status: String = "PLACED", // PLACED, DELIVERED, CANCELLED, RETURN_REQUESTED, RETURNED
    @get:PropertyName("paymentStatus") @set:PropertyName("paymentStatus") var paymentStatus: String = "PENDING", // PENDING, PAID, AWAITING_APPROVAL
    @get:PropertyName("paymentMethod") @set:PropertyName("paymentMethod") var paymentMethod: String = "", // COD, UPI
    @get:PropertyName("address") @set:PropertyName("address") var address: String = "",
    @get:PropertyName("deliveryNote") @set:PropertyName("deliveryNote") var deliveryNote: String = "",
    @get:PropertyName("deliveryDate") @set:PropertyName("deliveryDate") var deliveryDate: String = "",
    @get:PropertyName("timestamp") @set:PropertyName("timestamp") var timestamp: Long = System.currentTimeMillis(),
    @get:PropertyName("email") @set:PropertyName("email") var email: String = "",
    
    // Return specific fields
    @get:PropertyName("returnReason") @set:PropertyName("returnReason") var returnReason: String = "",
    @get:PropertyName("returnRequestDate") @set:PropertyName("returnRequestDate") var returnRequestDate: Long = 0,
    @get:PropertyName("returnAdminNote") @set:PropertyName("returnAdminNote") var returnAdminNote: String = ""
)
