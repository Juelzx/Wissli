package com.wissli.app.data.auth

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

enum class GoogleAccountSelection {
    /** No picker if exactly one account is already authorized; falls back to [ANY_ACCOUNT] if none is. */
    PREFER_AUTHORIZED,

    ANY_ACCOUNT,
}

class GoogleSignInCancelledException(
    cause: Throwable,
) : Exception(cause)

class NoGoogleAccountException(
    cause: Throwable,
) : Exception(cause)

/**
 * Fetches a Google ID token via Credential Manager. Takes the activity context per call (needed for
 * the account picker) instead of holding it, to avoid leaking the activity.
 */
interface GoogleAuthDataSource {
    suspend fun requestGoogleIdToken(
        activityContext: Context,
        selection: GoogleAccountSelection,
    ): String
}

class CredentialManagerGoogleAuthDataSource(
    private val webClientId: String,
) : GoogleAuthDataSource {
    override suspend fun requestGoogleIdToken(
        activityContext: Context,
        selection: GoogleAccountSelection,
    ): String {
        val credentialManager = CredentialManager.create(activityContext)
        val credential =
            try {
                credentialManager.requestCredential(activityContext, selection)
            } catch (e: GetCredentialCancellationException) {
                throw GoogleSignInCancelledException(e)
            } catch (e: NoCredentialException) {
                throw NoGoogleAccountException(e)
            }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }

    private suspend fun CredentialManager.requestCredential(
        activityContext: Context,
        selection: GoogleAccountSelection,
    ): Credential =
        when (selection) {
            GoogleAccountSelection.PREFER_AUTHORIZED ->
                try {
                    getCredential(activityContext, authorizedAccountsRequest()).credential
                } catch (_: NoCredentialException) {
                    getCredential(activityContext, allAccountsRequest()).credential
                }

            GoogleAccountSelection.ANY_ACCOUNT ->
                getCredential(activityContext, allAccountsRequest()).credential
        }

    // Auto-select is fine: this request only runs after an explicit tap, never on app start.
    private fun authorizedAccountsRequest() = buildRequest(filterByAuthorizedAccounts = true, autoSelect = true)

    private fun allAccountsRequest() = buildRequest(filterByAuthorizedAccounts = false, autoSelect = false)

    private fun buildRequest(
        filterByAuthorizedAccounts: Boolean,
        autoSelect: Boolean,
    ): GetCredentialRequest {
        val option =
            GetGoogleIdOption
                .Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
                .setAutoSelectEnabled(autoSelect)
                .build()
        return GetCredentialRequest
            .Builder()
            .addCredentialOption(option)
            .build()
    }
}
