package com.paliapp.ecommerce.data.model

import com.google.firebase.firestore.PropertyName

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val mobile: String = "",
    val address: String = "",
    val role: String = "CUSTOMER", // CUSTOMER, ADMIN
    @get:PropertyName("isApproved")
    @set:PropertyName("isApproved")
    var isApproved: Boolean = false
)
