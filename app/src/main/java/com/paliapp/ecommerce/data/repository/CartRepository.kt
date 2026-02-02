package com.paliapp.ecommerce.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paliapp.ecommerce.data.model.CartItem
import com.paliapp.ecommerce.data.model.Product
import kotlinx.coroutines.tasks.await

class CartRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUid(): String? {
        return auth.currentUser?.uid
    }

    suspend fun getCartItems(): List<CartItem> {
        val uid = getUid() ?: return emptyList()
        return try {
            db.collection("carts").document(uid).collection("items")
                .get()
                .await()
                .toObjects(CartItem::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addToCart(product: Product, quantity: Int): Result<Unit> {
        val uid = getUid() ?: return Result.failure(Exception("User not logged in"))
        val itemRef = db.collection("carts").document(uid).collection("items").document(product.id)

        return try {
            db.runTransaction {
                val snapshot = it.get(itemRef)
                val currentQty = snapshot.getLong("qty") ?: 0
                val newQty = currentQty + quantity

                if (newQty > product.stock) {
                    throw Exception("Only ${product.stock} units available in stock")
                }

                if (snapshot.exists()) {
                    it.update(itemRef, "qty", newQty)
                } else {
                    val cartItem = CartItem(
                        id = product.id,
                        name = product.name,
                        price = product.price,
                        qty = quantity,
                        imageUrl = product.imageUrl
                    )
                    it.set(itemRef, cartItem)
                }
                null
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromCart(itemId: String): Result<Unit> {
        val uid = getUid() ?: return Result.failure(Exception("User not logged in"))
        return try {
            db.collection("carts").document(uid).collection("items").document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearCart(): Result<Unit> {
        val uid = getUid() ?: return Result.failure(Exception("User not logged in"))
        return try {
            val cartItems = getCartItems()
            val batch = db.batch()
            cartItems.forEach {
                val itemRef = db.collection("carts").document(uid).collection("items").document(it.id)
                batch.delete(itemRef)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}