package com.paliapp.ecommerce.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SettingsRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getSupportWhatsapp(): String {
        return try {
            val doc = db.collection("settings").document("support").get().await()
            doc.getString("whatsapp") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun updateSupportWhatsapp(number: String): Result<Unit> {
        return try {
            db.collection("settings").document("support")
                .set(mapOf("whatsapp" to number))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUpiQrUrlFlow(): Flow<String?> = callbackFlow {
        val subscription = db.collection("settings").document("payment")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.getString("upiQrUrl"))
            }
        awaitClose { subscription.remove() }
    }

    suspend fun updateUpiQrUrl(url: String): Result<Unit> {
        return try {
            db.collection("settings").document("payment")
                .set(mapOf("upiQrUrl" to url))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}