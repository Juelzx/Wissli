package com.wissli.app.data.auth

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Device-only, never written to Firestore and excluded from backups (see res/xml backup rules). */
data class LastSignedInAccount(
    val displayName: String,
    val email: String,
)

interface LastAccountLocalDataSource {
    suspend fun get(): LastSignedInAccount?

    suspend fun save(account: LastSignedInAccount)
}

class SharedPreferencesLastAccountDataSource(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LastAccountLocalDataSource {
    private val appContext = context.applicationContext
    private val prefs by lazy { appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    override suspend fun get(): LastSignedInAccount? =
        withContext(ioDispatcher) {
            val displayName = prefs.getString(KEY_DISPLAY_NAME, null)
            val email = prefs.getString(KEY_EMAIL, null)
            if (displayName == null && email == null) {
                null
            } else {
                LastSignedInAccount(displayName = displayName.orEmpty(), email = email.orEmpty())
            }
        }

    override suspend fun save(account: LastSignedInAccount) {
        withContext(ioDispatcher) {
            prefs.edit {
                putString(KEY_DISPLAY_NAME, account.displayName)
                putString(KEY_EMAIL, account.email)
            }
        }
    }

    private companion object {
        // Referenced by res/xml/backup_rules.xml and data_extraction_rules.xml.
        const val PREFS_NAME = "last_signed_in_account"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_EMAIL = "email"
    }
}
