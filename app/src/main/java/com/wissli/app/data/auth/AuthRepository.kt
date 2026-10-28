package com.wissli.app.data.auth

import com.wissli.app.core.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeCurrentUser(): Flow<User?>

    /** Last account signed in on this device, kept after sign-out; `null` before the first sign-in. */
    suspend fun getLastSignedInAccount(): LastSignedInAccount?

    suspend fun signInWithGoogle(googleIdToken: String): Result<User>

    suspend fun signOut()
}
