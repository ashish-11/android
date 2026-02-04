package com.paliapp.ecommerce.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.paliapp.ecommerce.data.model.Product
import kotlinx.coroutines.tasks.await

class ProductRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun getActiveProducts(): List<Product> {
        return try {
            db.collection("products")
                .whereEqualTo("active", true)
                .get()
                .await()
                .documents.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllProducts(): List<Product> {
        return try {
            db.collection("products")
                .get()
                .await()
                .documents.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addProduct(product: Product): Result<Unit> {
        return try {
            db.collection("products").add(product).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun updateProduct(product: Product): Result<Unit> {
        val productId = product.id.trim()
        if (productId.isBlank()) {
            return Result.failure(Exception("Product ID is blank"))
        }
        return try {
            db.collection("products")
                .document(productId)
                .set(product)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        val id = productId.trim()
        if (id.isBlank()) {
            return Result.failure(Exception("Product ID is blank"))
        }
        return try {
            db.collection("products")
                .document(id)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetProductCategory(categoryId: String): Result<Unit> {
        return try {
            val querySnapshot = db.collection("products")
                .whereEqualTo("categoryId", categoryId)
                .get()
                .await()
            
            val batch = db.batch()
            for (document in querySnapshot.documents) {
                batch.update(document.reference, "categoryId", "")
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
