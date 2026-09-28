package com.wissli.app.data.auth

import com.wissli.app.core.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeCurrentUser(): Flow<User?>

    suspend fun signInWithGoogle(googleIdToken: String): Result<User>

    suspend fun signOut()
}
