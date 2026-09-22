package com.wissli.app.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Platform-Datenquelle: holt über die Android Credential Manager API ein Google-ID-Token, das
 * [AuthRepository.signInWithGoogle] anschließend gegen eine Firebase-Session tauscht. Kein direkter
 * Firebase-Zugriff hier — reine Google/Android-Plattform-API, deshalb bewusst getrennt von
 * [AuthRepository] (Single Responsibility, leichter zu testen/mocken).
 *
 * Braucht einen Activity-Context (für den System-Bottomsheet/Account-Picker) — deshalb Context als
 * Methodenparameter statt als injiziertes Konstruktor-Feld. So hält die DataSource keine
 * Activity-Referenz über den Aufruf hinaus fest (kein Leak).
 */
interface GoogleAuthDataSource {
    suspend fun requestGoogleIdToken(activityContext: Context): String
}

class CredentialManagerGoogleAuthDataSource(
    private val webClientId: String,
) : GoogleAuthDataSource {
    override suspend fun requestGoogleIdToken(activityContext: Context): String {
        val credentialManager = CredentialManager.create(activityContext)
        val credential =
            try {
                // Erster Versuch: nur Accounts, die diese App schon mal für Sign-in autorisiert haben
                // (stilles, schnelleres Sign-in ohne vollen Account-Picker).
                credentialManager.getCredential(activityContext, authorizedAccountsRequest()).credential
            } catch (_: NoCredentialException) {
                // Kein vorher autorisierter Account gefunden (z. B. allererster Login) -> voller
                // Account-Picker mit allen Google-Accounts auf dem Gerät.
                credentialManager.getCredential(activityContext, allAccountsRequest()).credential
            }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }

    private fun authorizedAccountsRequest() = buildRequest(filterByAuthorizedAccounts = true)

    private fun allAccountsRequest() = buildRequest(filterByAuthorizedAccounts = false)

    private fun buildRequest(filterByAuthorizedAccounts: Boolean): GetCredentialRequest {
        val option =
            GetGoogleIdOption
                .Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
                .setAutoSelectEnabled(false)
                .build()
        return GetCredentialRequest
            .Builder()
            .addCredentialOption(option)
            .build()
    }
}

/*
 * TODO sobald Google Sign-In über die Basisfälle hinauswächst:
 *  - Andere GetCredentialException-Subtypen (z. B. GetCredentialCancellationException, wenn der
 *    Nutzer den Picker wegtippt) im Repository/ViewModel gezielt abfangen und in eine passende
 *    LoginState-Fehlermeldung übersetzen statt eine generische Fehlermeldung zu zeigen.
 *  - Optional (Hardening): setNonce(...) mit einem zufälligen, gehashten Wert setzen, falls eigene
 *    Replay-Schutz-Logik über den Firebase-Standardflow hinaus gebraucht wird.
 */
