package com.wissli.app.data.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.wissli.app.core.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AuthRepository {
    override fun observeCurrentUser(): Flow<User?> =
        callbackFlow {
            val producerScope = this
            val listener =
                FirebaseAuth.AuthStateListener { auth ->
                    val firebaseUser = auth.currentUser
                    if (firebaseUser == null) {
                        Log.d(TAG, "observeCurrentUser: kein FirebaseUser -> ausgeloggt")
                        trySend(null)
                    } else {
                        Log.d(TAG, "observeCurrentUser: FirebaseAuth-Session vorhanden (uid=${firebaseUser.uid})")
                        producerScope.launch {
                            val uid = firebaseUser.uid
                            val user =
                                runCatching { fetchUserDocument(uid) }
                                    .onFailure { Log.d(TAG, "observeCurrentUser: users/$uid nicht ladbar", it) }
                                    .getOrNull()
                            val outcome = if (user != null) "geladen" else "null"
                            Log.d(TAG, "observeCurrentUser: users/$uid -> $outcome")
                            trySend(user)
                        }
                    }
                }
            firebaseAuth.addAuthStateListener(listener)
            awaitClose { firebaseAuth.removeAuthStateListener(listener) }
        }

    override suspend fun signInWithGoogle(googleIdToken: String): Result<User> =
        runCatching {
            val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = requireNotNull(authResult.user) { "Firebase-Sign-in ohne User zurückgegeben" }
            Log.d(TAG, "signInWithGoogle: Firebase-Auth-Login erfolgreich (uid=${firebaseUser.uid})")
            getOrCreateUserDocument(uid = firebaseUser.uid, displayName = firebaseUser.displayName.orEmpty())
        }.onFailure { throwable ->
            Log.d(TAG, "signInWithGoogle: fehlgeschlagen", throwable)
        }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    private suspend fun fetchUserDocument(uid: String): User? {
        val snapshot =
            firestore
                .collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .await()
        return snapshot.toUserOrNull()
    }

    private suspend fun getOrCreateUserDocument(
        uid: String,
        displayName: String,
    ): User {
        val docRef = firestore.collection(USERS_COLLECTION).document(uid)
        val existing = docRef.get().await().toUserOrNull()
        if (existing != null) return existing

        val newUser =
            User(
                id = uid,
                displayName = displayName,
                createdAt = System.currentTimeMillis(),
            )
        docRef.set(newUser.toFirestoreMap()).await()
        return newUser
    }

    private fun DocumentSnapshot.toUserOrNull(): User? {
        if (!exists()) return null
        return User(
            id = id,
            displayName = getString("displayName").orEmpty(),
            createdAt = getLong("createdAt") ?: 0L,
        )
    }

    private fun User.toFirestoreMap(): Map<String, Any?> =
        mapOf(
            "displayName" to displayName,
            "createdAt" to createdAt,
        )

    private companion object {
        const val TAG = "FirebaseAuthRepository"
        const val USERS_COLLECTION = "users"
    }
}
