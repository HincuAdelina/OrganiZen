package com.organizen.app.auth.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser get() = auth.currentUser

    suspend fun login(email: String, password: String): Result<Unit> = try {
        auth.signInWithEmailAndPassword(email, password).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun register(name: String, email: String, password: String): Result<Unit> = try {
        // Creează contul
        auth.createUserWithEmailAndPassword(email, password).await()

        // Setează displayName în profil
        val profile = userProfileChangeRequest { displayName = name }
        auth.currentUser?.updateProfile(profile)?.await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun logout() = auth.signOut()

    suspend fun updateName(name: String): Result<Unit> = try {
        val profile = userProfileChangeRequest { displayName = name }
        auth.currentUser?.updateProfile(profile)?.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updatePassword(password: String): Result<Unit> = try {
        auth.currentUser?.updatePassword(password)?.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
