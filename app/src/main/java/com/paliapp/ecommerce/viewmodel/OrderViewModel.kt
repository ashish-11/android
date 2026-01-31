package com.paliapp.ecommerce.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.paliapp.ecommerce.data.model.Order

class OrderViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _orders = mutableStateOf<List<Order>>(emptyList())
    val orders: State<List<Order>> = _orders

    private val _customerOrders = mutableStateOf<List<Order>>(emptyList())
    val customerOrders: State<List<Order>> = _customerOrders

    val upiQrUrl = mutableStateOf<String?>(null)

    init {
        loadAllOrders()
        loadUpiQr()
    }

    fun loadAllOrders() {
        db.collection("orders")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("OrderViewModel", "Error loading orders", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    _orders.value = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Order::class.java)?.apply { id = doc.id }
                    }
                }
            }
    }

    fun loadCustomerOrders(userId: String) {
        if (userId.isEmpty()) return
        db.collection("orders")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("OrderViewModel", "Error loading customer orders", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    _customerOrders.value = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Order::class.java)?.apply { id = doc.id }
                    }
                } else {
                    _customerOrders.value = emptyList()
                }
            }
    }
    
    fun updateOrderStatus(orderId: String, newStatus: String) {
        if (orderId.isEmpty()) return
        db.collection("orders").document(orderId).update("status", newStatus)
    }

    fun updatePaymentStatus(orderId: String, newStatus: String) {
        if (orderId.isEmpty()) return
        db.collection("orders").document(orderId).update("paymentStatus", newStatus)
    }

    fun setPaymentMethod(orderId: String, method: String) {
        if (orderId.isEmpty()) return
        
        val updates = hashMapOf<String, Any>(
            "paymentMethod" to method,
            "paymentStatus" to "AWAITING_APPROVAL"
        )
        
        db.collection("orders").document(orderId).update(updates)
            .addOnSuccessListener {
                Log.d("OrderViewModel", "Order updated to AWAITING_APPROVAL")
            }
            .addOnFailureListener {
                Log.e("OrderViewModel", "Failed to update order payment method", it)
            }
    }

    fun updateDeliveryDate(orderId: String, date: String) {
        if (orderId.isEmpty()) return
        db.collection("orders").document(orderId).update("deliveryDate", date)
    }

    fun deleteOrder(orderId: String) {
        if (orderId.isEmpty()) return
        db.collection("orders").document(orderId).delete()
    }

    fun refreshFromServer() {
        db.clearPersistence().addOnCompleteListener {
            loadAllOrders()
            loadUpiQr()
        }
    }

    private fun loadUpiQr() {
        db.collection("settings").document("payment").addSnapshotListener { doc, _ ->
            upiQrUrl.value = doc?.getString("upiQrUrl")
        }
    }

    fun updateUpiQr(url: String) {
        db.collection("settings").document("payment").set(mapOf("upiQrUrl" to url))
    }
}
