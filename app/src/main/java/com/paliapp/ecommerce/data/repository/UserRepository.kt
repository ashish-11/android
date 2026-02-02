package com.paliapp.ecommerce.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getUid(): String? = auth.currentUser?.uid

    fun getUserAddress(): Flow<String> = callbackFlow {
        val uid = getUid()
        if (uid == null) {
            trySend("")
            close()
            return@callbackFlow
        }

        val subscription = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Log the error instead of closing the flow with an exception
                    // This prevents crashes during logout when permission is revoked
                    Log.w("UserRepository", "Error listening to user address: ${error.message}")
                    trySend("")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.getString("address") ?: "")
                } else {
                    trySend("")
                }
            }

        awaitClose { subscription.remove() }
    }

    suspend fun getUserDetails(uid: String): Map<String, Any>? {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            if (doc.exists()) doc.data else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUserAddress(uid: String, address: String): Result<Unit> {
        return try {
            db.collection("users").document(uid).update("address", address).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
