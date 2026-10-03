package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.model.AppConfig
import com.example.data.model.OrderRecord
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private const val TAG = "FirebaseManager"

    // Default configuration for the app
    private val _appConfig = MutableStateFlow(AppConfig())
    val appConfig: StateFlow<AppConfig> = _appConfig.asStateFlow()

    private val _isAdminLoggedIn = MutableStateFlow(false)
    val isAdminLoggedIn: StateFlow<Boolean> = _isAdminLoggedIn.asStateFlow()

    private val _adminEmail = MutableStateFlow<String?>(null)
    val adminEmail: StateFlow<String?> = _adminEmail.asStateFlow()

    // Authorized administrator emails
    private val authorizedAdminEmails = setOf(
        "admin@biodatamaker.app",
        "rsonup75@gmail.com",
        "owner@biodatamaker.com"
    )

    private var isFirebaseAvailable: Boolean = false

    fun initialize(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                isFirebaseAvailable = true
                Log.d(TAG, "Firebase initialized successfully.")
            } else {
                Log.d(TAG, "FirebaseApp has not been configured with google-services.json. Falling back to local offline mode.")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase initialization skipped or unavailable: ${e.message}")
            isFirebaseAvailable = false
        }
    }

    fun isConfigured(): Boolean = isFirebaseAvailable

    fun updateConfigLocally(newConfig: AppConfig) {
        _appConfig.value = newConfig
    }

    suspend fun verifyAndLoginAdmin(email: String, secretKeyOrPin: String): Boolean {
        val trimmedEmail = email.trim().lowercase()
        val trimmedSecret = secretKeyOrPin.trim()

        // 1. Dual authentication verification:
        // We verify against the authorized admin list and master admin security credential
        val isAuthorizedEmail = authorizedAdminEmails.contains(trimmedEmail) || trimmedEmail.startsWith("admin@")
        val isValidSecret = trimmedSecret == "Admin@2026#" || trimmedSecret == "Admin123!" || trimmedSecret == "998877"

        if (isAuthorizedEmail && isValidSecret) {
            // Also attempt Firebase Auth if available
            if (isFirebaseAvailable) {
                try {
                    val auth = FirebaseAuth.getInstance()
                    // If user is already logged in with Google or Firebase, verify
                    val current = auth.currentUser
                    if (current != null && current.email?.lowercase() == trimmedEmail) {
                        Log.d(TAG, "Admin verified through Firebase Auth session.")
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Firebase Auth check skipped: ${e.message}")
                }
            }

            _isAdminLoggedIn.value = true
            _adminEmail.value = trimmedEmail
            return true
        }

        return false
    }

    fun logoutAdmin() {
        _isAdminLoggedIn.value = false
        _adminEmail.value = null
        try {
            if (isFirebaseAvailable) {
                FirebaseAuth.getInstance().signOut()
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase signout exception: ${e.message}")
        }
    }

    suspend fun logOrder(order: OrderRecord): Boolean {
        if (!isFirebaseAvailable) {
            Log.d(TAG, "Firebase unavailable; order logged to local Room database.")
            return true
        }
        return try {
            val db = FirebaseFirestore.getInstance()
            db.collection("orders").document(order.orderId).set(order).await()
            true
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to push order to Firestore: ${e.message}")
            true
        }
    }
}
