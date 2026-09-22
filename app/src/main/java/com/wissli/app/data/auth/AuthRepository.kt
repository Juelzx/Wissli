package com.wissli.app.data.auth

import com.wissli.app.core.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Abstraktion über die Firebase-Auth-/Firestore-Zugriffe für den eingeloggten [User]. ViewModels
 * dürfen ausschließlich gegen dieses Interface programmieren (siehe CLAUDE.md "Architektur") —
 * niemals direkt gegen FirebaseAuth/FirebaseFirestore.
 */
interface AuthRepository {

    /** Emittiert den aktuell eingeloggten [User] (`null` = ausgeloggt). Ersetzt Polling. */
    fun observeCurrentUser(): Flow<User?>

    /**
     * Tauscht ein Google-ID-Token (aus [GoogleAuthDataSource]) gegen eine Firebase-Session und legt
     * bei Erstanmeldung das zugehörige `users/{uid}`-Dokument in Firestore an.
     */
    suspend fun signInWithGoogle(googleIdToken: String): Result<User>

    suspend fun signOut()
}
