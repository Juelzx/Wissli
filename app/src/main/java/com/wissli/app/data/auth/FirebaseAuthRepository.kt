package com.wissli.app.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.wissli.app.core.model.User
import com.wissli.app.core.model.UserRole
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Einzige Stelle im Code, die FirebaseAuth/FirebaseFirestore für Auth anfasst (siehe CLAUDE.md
 * "Firebase-/Room-Zugriffe ausschließlich hinter Repository-/DataSource-Interfaces").
 */
class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AuthRepository {

    override fun observeCurrentUser(): Flow<User?> = callbackFlow {
        // AuthStateListener ist ein plainer SAM-Callback, kein CoroutineScope selbst — deshalb den
        // ProducerScope von callbackFlow (this) explizit einfangen, um darin launch() zu nutzen.
        val producerScope = this
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            if (firebaseUser == null) {
                trySend(null)
            } else {
                producerScope.launch { trySend(fetchUserDocument(firebaseUser.uid)) }
            }
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithGoogle(googleIdToken: String): Result<User> = runCatching {
        val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val authResult = firebaseAuth.signInWithCredential(credential).await()
        val firebaseUser = requireNotNull(authResult.user) { "Firebase-Sign-in ohne User zurückgegeben" }
        getOrCreateUserDocument(uid = firebaseUser.uid, displayName = firebaseUser.displayName.orEmpty())
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    private suspend fun fetchUserDocument(uid: String): User? {
        val snapshot = firestore.collection(USERS_COLLECTION).document(uid).get().await()
        return snapshot.toUserOrNull()
    }

    /**
     * TODO: Sobald es einen Rollen-Onboarding-Screen gibt (Parent wählt bei Erstanmeldung
     * "Elternteil" oder legt ein Kinderprofil an), hier nicht mehr hart PARENT vergeben, sondern
     * den State (z. B. "braucht Onboarding") zurückgeben und die Rolle dort setzen lassen. Aktuell
     * meldet sich über Google ausschließlich der Elternteil an — Kinder bekommen laut Produktscope
     * eigene, datensparsame Profile innerhalb der Family statt eines eigenen Google-Accounts.
     */
    private suspend fun getOrCreateUserDocument(uid: String, displayName: String): User {
        val docRef = firestore.collection(USERS_COLLECTION).document(uid)
        val existing = docRef.get().await().toUserOrNull()
        if (existing != null) return existing

        val newUser = User(
            id = uid,
            displayName = displayName,
            role = UserRole.PARENT,
            familyId = null,
            createdAt = System.currentTimeMillis(),
        )
        docRef.set(newUser.toFirestoreMap()).await()
        return newUser
    }

    private fun DocumentSnapshot.toUserOrNull(): User? {
        if (!exists()) return null
        val role = getString("role")?.let { runCatching { UserRole.valueOf(it) }.getOrNull() } ?: return null
        return User(
            id = id,
            displayName = getString("displayName").orEmpty(),
            role = role,
            familyId = getString("familyId"),
            createdAt = getLong("createdAt") ?: 0L,
        )
    }

    private fun User.toFirestoreMap(): Map<String, Any?> = mapOf(
        "displayName" to displayName,
        "role" to role.name,
        "familyId" to familyId,
        "createdAt" to createdAt,
    )

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}
