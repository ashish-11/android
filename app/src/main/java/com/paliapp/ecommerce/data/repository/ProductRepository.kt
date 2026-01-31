package com.paliapp.ecommerce.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.paliapp.ecommerce.data.model.Product

class ProductRepository {

    private val db = FirebaseFirestore.getInstance()

    fun getActiveProducts(onResult: (List<Product>) -> Unit) {
        db.collection("products")
            .whereEqualTo("active", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val products = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                }
                onResult(products)
            }
    }

    fun getAllProducts(onResult: (List<Product>) -> Unit) {
        db.collection("products")
            .get()
            .addOnSuccessListener { snapshot ->
                val products = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                }
                onResult(products)
            }
    }

    fun addProduct(product: Product, onResult: (Boolean) -> Unit) {
        db.collection("products")
            .add(product)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }


    fun updateProduct(product: Product, onResult: (Boolean) -> Unit) {
        db.collection("products")
            .document(product.id)
            .set(product)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun deleteProduct(productId: String, onResult: (Boolean) -> Unit) {
        db.collection("products")
            .document(productId)
            .delete()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
}
