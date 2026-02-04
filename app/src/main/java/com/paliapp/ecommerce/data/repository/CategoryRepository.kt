package com.paliapp.ecommerce.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.paliapp.ecommerce.data.model.Category
import kotlinx.coroutines.tasks.await

class CategoryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val categoriesRef = db.collection("categories")

    suspend fun getCategories(): List<Category> {
        return try {
            categoriesRef.get().await().toObjects(Category::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addCategory(category: Category): Result<Unit> {
        return try {
            val docRef = categoriesRef.document()
            val newCategory = category.copy(id = docRef.id)
            docRef.set(newCategory).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCategory(category: Category): Result<Unit> {
        return try {
            categoriesRef.document(category.id).set(category).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return try {
            categoriesRef.document(categoryId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
