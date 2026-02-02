package com.paliapp.ecommerce.data.model

import com.google.firebase.firestore.PropertyName

data class User(
    @get:PropertyName("uid") @set:PropertyName("uid") var uid: String = "",
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("email") @set:PropertyName("email") var email: String = "",
    @get:PropertyName("mobile") @set:PropertyName("mobile") var mobile: String = "",
    @get:PropertyName("address") @set:PropertyName("address") var address: String = "",
    @get:PropertyName("role") @set:PropertyName("role") var role: String = "CUSTOMER", // CUSTOMER, ADMIN
    @get:PropertyName("isApproved")
    @set:PropertyName("isApproved")
    var isApproved: Boolean = false
)
