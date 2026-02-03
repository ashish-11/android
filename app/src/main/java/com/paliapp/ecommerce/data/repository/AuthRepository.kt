package com.paliapp.ecommerce.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.paliapp.ecommerce.data.model.User

class AuthRepository {

    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun login(
        email: String,
        password: String,
        onResult: (Result<User>) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (!uid.isNullOrBlank()) {
                    getUserDetails(uid) { userResult ->
                        onResult(userResult)
                    }
                } else {
                    onResult(Result.failure(Exception("Login failed: User ID is null")))
                }
            }
            .addOnFailureListener {
                onResult(Result.failure(it))
            }
    }

    fun register(
        name: String,
        email: String,
        mobile: String,
        password: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid.isNullOrBlank()) {
                    onResult(Result.failure(Exception("Registration failed: User ID is null")))
                    return@addOnSuccessListener
                }
                
                val user = User(
                    uid = uid,
                    name = name,
                    email = email.trim(),
                    mobile = mobile,
                    role = "CUSTOMER",
                    isApproved = false
                )

                db.collection("users")
                    .document(uid)
                    .set(user)
                    .addOnSuccessListener {
                        onResult(Result.success(Unit))
                    }
                    .addOnFailureListener {
                        onResult(Result.failure(it))
                    }
            }
            .addOnFailureListener {
                onResult(Result.failure(it))
            }
    }

    fun resetPassword(email: String, onResult: (Result<Unit>) -> Unit) {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty()) {
            onResult(Result.failure(Exception("Please enter your email address")))
            return
        }
        auth.sendPasswordResetEmail(cleanEmail)
            .addOnSuccessListener { 
                Log.d("AuthRepository", "Password reset email sent successfully to: $cleanEmail")
                onResult(Result.success(Unit)) 
            }
            .addOnFailureListener { 
                Log.e("AuthRepository", "Password reset failed for: $cleanEmail", it)
                onResult(Result.failure(it)) 
            }
    }

    fun getUserDetails(
        uid: String,
        onResult: (Result<User>) -> Unit
    ) {
        val cleanUid = uid.trim()
        if (cleanUid.isBlank()) {
            onResult(Result.failure(Exception("Invalid UID")))
            return
        }
        try {
            db.collection("users")
                .document(cleanUid)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val user = doc.toObject(User::class.java)
                        if (user != null) {
                            onResult(Result.success(user))
                        } else {
                            onResult(Result.failure(Exception("Error parsing user data")))
                        }
                    } else {
                        onResult(Result.failure(Exception("User profile not found")))
                    }
                }
                .addOnFailureListener {
                    onResult(Result.failure(it))
                }
        } catch (e: Exception) {
            onResult(Result.failure(e))
        }
    }

    fun observeUserDetails(
        uid: String,
        onResult: (Result<User>) -> Unit
    ): ListenerRegistration? {
        val cleanUid = uid.trim()
        if (cleanUid.isBlank()) {
            onResult(Result.failure(IllegalArgumentException("UID cannot be empty")))
            return null
        }
        return try {
            db.collection("users")
                .document(cleanUid)
                .addSnapshotListener { doc, error ->
                    if (error != null) {
                        onResult(Result.failure(error))
                        return@addSnapshotListener
                    }
                    if (doc != null && doc.exists()) {
                        val user = doc.toObject(User::class.java)
                        if (user != null) {
                            onResult(Result.success(user))
                        } else {
                            onResult(Result.failure(Exception("Error parsing user data")))
                        }
                    } else {
                        onResult(Result.failure(Exception("User profile not found")))
                    }
                }
        } catch (e: Exception) {
            onResult(Result.failure(e))
            null
        }
    }

    fun updateUserDetails(
        uid: String,
        name: String,
        mobile: String,
        address: String,
        onResult: (Boolean) -> Unit
    ) {
        val cleanUid = uid.trim()
        if (cleanUid.isBlank()) {
            onResult(false)
            return
        }
        val updates = mapOf(
            "name" to name,
            "mobile" to mobile,
            "address" to address
        )
        db.collection("users")
            .document(cleanUid)
            .update(updates)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun getUserRole(
        uid: String,
        onResult: (Result<String>) -> Unit
    ) {
        if (uid.isBlank()) {
            onResult(Result.failure(Exception("Invalid UID")))
            return
        }
        getUserDetails(uid) { result ->
            result.onSuccess { onResult(Result.success(it.role)) }
            result.onFailure { onResult(Result.failure(it)) }
        }
    }

    fun getPendingUsers(onResult: (List<User>) -> Unit) {
        db.collection("users")
            .whereEqualTo("role", "CUSTOMER")
            .whereEqualTo("isApproved", false)
            .get()
            .addOnSuccessListener { snapshot ->
                val users = snapshot.toObjects(User::class.java)
                onResult(users)
            }
            .addOnFailureListener {
                Log.e("AuthRepository", "Error getting pending users", it)
                onResult(emptyList())
            }
    }

    fun getAllCustomers(onResult: (List<User>) -> Unit) {
        db.collection("users")
            .whereEqualTo("role", "CUSTOMER")
            .get()
            .addOnSuccessListener { snapshot ->
                val users = snapshot.toObjects(User::class.java)
                onResult(users)
            }
            .addOnFailureListener {
                Log.e("AuthRepository", "Error getting all customers", it)
                onResult(emptyList())
            }
    }

    fun approveUser(uid: String, onResult: (Boolean) -> Unit) {
        val cleanUid = uid.trim()
        if (cleanUid.isBlank()) {
            onResult(false)
            return
        }
        db.collection("users").document(cleanUid).update("isApproved", true)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun rejectUser(uid: String, onResult: (Boolean) -> Unit) {
        val cleanUid = uid.trim()
        if (cleanUid.isBlank()) {
            onResult(false)
            return
        }
        db.collection("users").document(cleanUid).delete()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun logout() {
        auth.signOut()
    }
}
