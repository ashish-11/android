package com.paliapp.ecommerce.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.paliapp.ecommerce.data.model.CartItem
import com.paliapp.ecommerce.data.model.Order
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class OrderRepository {
    private val db = FirebaseFirestore.getInstance()

    fun getAllOrders(): Flow<List<Order>> = callbackFlow {
        val subscription = db.collection("orders")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val orders = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Order::class.java)?.copy(id = doc.id)
                    }
                    trySend(orders)
                }
            }
        awaitClose { subscription.remove() }
    }

    fun getCustomerOrders(userId: String): Flow<List<Order>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = db.collection("orders")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val orders = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Order::class.java)?.copy(id = doc.id)
                    }
                    trySend(orders)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun placeOrder(
        userId: String,
        userName: String,
        userEmail: String,
        userMobile: String,
        items: List<CartItem>,
        address: String
    ): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                // 1. Validate stock and prepare updates
                val productUpdates = items.map { item ->
                    val productRef = db.collection("products").document(item.id)
                    val productSnap = transaction.get(productRef)
                    
                    if (!productSnap.exists()) {
                        throw FirebaseFirestoreException("Product ${item.name} not found", FirebaseFirestoreException.Code.ABORTED)
                    }
                    
                    val currentStock = productSnap.getLong("stock") ?: 0
                    if (currentStock < item.qty) {
                        throw FirebaseFirestoreException("Insufficient stock for ${item.name}", FirebaseFirestoreException.Code.ABORTED)
                    }
                    
                    productRef to (currentStock - item.qty)
                }

                // 2. Perform updates
                productUpdates.forEach { (ref, newStock) ->
                    transaction.update(ref, "stock", newStock)
                }

                // 3. Create Order
                val totalAmount = items.sumOf { it.price * it.qty }
                val orderRef = db.collection("orders").document()
                val order = Order(
                    id = orderRef.id,
                    userId = userId,
                    userName = userName,
                    userEmail = userEmail,
                    userMobile = userMobile,
                    items = items,
                    totalAmount = totalAmount,
                    address = address,
                    status = "PLACED",
                    paymentStatus = "PENDING",
                    email = userEmail,
                    timestamp = System.currentTimeMillis()
                )
                transaction.set(orderRef, order)

                // 4. Clear Cart (Batch deletion in transaction)
                items.forEach { item ->
                    val itemRef = db.collection("carts").document(userId).collection("items").document(item.id)
                    transaction.delete(itemRef)
                }
                
                // 5. Update user address
                transaction.update(db.collection("users").document(userId), "address", address)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: String): Result<Unit> {
        return try {
            db.collection("orders").document(orderId).update("status", status).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePaymentStatus(orderId: String, status: String): Result<Unit> {
        return try {
            db.collection("orders").document(orderId).update("paymentStatus", status).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDeliveryDate(orderId: String, date: String): Result<Unit> {
        return try {
            db.collection("orders").document(orderId).update("deliveryDate", date).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setPaymentMethod(orderId: String, method: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "paymentMethod" to method,
                "paymentStatus" to "AWAITING_APPROVAL"
            )
            db.collection("orders").document(orderId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteOrder(orderId: String): Result<Unit> {
        return try {
            db.collection("orders").document(orderId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun archiveAndDeleteOrders(orders: List<Order>): Result<Unit> {
        return try {
            val batch = db.batch()
            orders.forEach { order ->
                if (order.id.isBlank()) {
                    Log.e("OrderRepository", "Skipping archive for order with blank ID")
                    return@forEach
                }

                val calendar = Calendar.getInstance().apply {
                    timeInMillis = if (order.timestamp > 0) order.timestamp else System.currentTimeMillis()
                }
                val year = calendar.get(Calendar.YEAR).toString()
                val month = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                val week = calendar.get(Calendar.WEEK_OF_YEAR).toString().padStart(2, '0')
                val day = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')

                val archiveRef = db.collection("archived_orders")
                    .document(year)
                    .collection(month)
                    .document(week)
                    .collection(day)
                    .document(order.id)

                val orderRef = db.collection("orders").document(order.id)
                batch.set(archiveRef, order)
                batch.delete(orderRef)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error archiving orders: ${e.message}", e)
            Result.failure(e)
        }
    }
}
