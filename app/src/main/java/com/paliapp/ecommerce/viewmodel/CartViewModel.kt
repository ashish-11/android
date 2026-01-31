package com.paliapp.ecommerce.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paliapp.ecommerce.data.model.CartItem
import com.paliapp.ecommerce.data.model.Order
import com.paliapp.ecommerce.data.model.Product

class CartViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _cartItems = mutableStateOf<List<CartItem>>(emptyList())
    val cartItems: State<List<CartItem>> = _cartItems

    private val _userAddress = mutableStateOf("")
    val userAddress: State<String> = _userAddress

    init {
        loadCartItems()
        loadUserAddress()
    }

    private fun loadUserAddress() {
        val uid = auth.currentUser?.uid ?: return
        if (uid.isEmpty()) return
        
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            _userAddress.value = doc.getString("address") ?: ""
        }
    }

    fun loadCartItems() {
        val uid = auth.currentUser?.uid ?: return
        if (uid.isEmpty()) return

        db.collection("carts").document(uid).collection("items")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _cartItems.value = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(CartItem::class.java)?.copy(id = doc.id)
                    }
                }
            }
    }

    fun addToCart(product: Product, quantity: Int) {
        val uid = auth.currentUser?.uid ?: return
        if (uid.isEmpty() || product.id.isEmpty()) return

        val itemRef = db.collection("carts")
            .document(uid)
            .collection("items")
            .document(product.id)

        itemRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val currentQty = doc.getLong("qty") ?: 0
                itemRef.update("qty", currentQty + quantity)
            } else {
                itemRef.set(
                    CartItem(
                        id = product.id,
                        name = product.name,
                        price = product.price,
                        qty = quantity,
                        imageUrl = product.imageUrl
                    )
                )
            }
        }
    }

    fun removeFromCart(itemId: String) {
        val uid = auth.currentUser?.uid ?: return
        if (uid.isEmpty() || itemId.isEmpty()) return
        db.collection("carts").document(uid).collection("items").document(itemId).delete()
    }

    fun placeOrder(address: String, onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        if (uid.isEmpty() || _cartItems.value.isEmpty()) return

        // Update user's default address in profile
        db.collection("users").document(uid).update("address", address)

        // Fetch user details to include in the order
        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            val userName = userDoc.getString("name") ?: ""
            val userMobile = userDoc.getString("mobile") ?: ""

            val totalAmount = _cartItems.value.sumOf { it.price * it.qty }
            val orderRef = db.collection("orders").document()
            val order = Order(
                id = orderRef.id,
                userId = uid,
                userName = userName,
                userMobile = userMobile,
                items = _cartItems.value,
                totalAmount = totalAmount,
                address = address
            )

            orderRef.set(order).addOnSuccessListener {
                // Clear cart
                val batch = db.batch()
                _cartItems.value.forEach { item ->
                    if (item.id.isNotEmpty()) {
                        val itemRef = db.collection("carts").document(uid).collection("items").document(item.id)
                        batch.delete(itemRef)
                    }
                }
                batch.commit().addOnSuccessListener {
                    onSuccess()
                }
            }
        }
    }
}
