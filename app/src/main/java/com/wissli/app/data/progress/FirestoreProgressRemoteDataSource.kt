package com.wissli.app.data.progress

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.wissli.app.core.model.CategoryProgress
import kotlinx.coroutines.tasks.await

// Field names must match the progress rules in firestore.rules.
class FirestoreProgressRemoteDataSource(
    private val firestore: FirebaseFirestore,
) : ProgressRemoteDataSource {
    override suspend fun fetchAll(userId: String): List<CategoryProgress> =
        progressCollection(userId)
            .get(Source.SERVER)
            .await()
            .documents
            .map { it.toProgress() }

    override suspend fun save(
        userId: String,
        progress: CategoryProgress,
    ) {
        progressCollection(userId)
            .document(progress.categoryId)
            .set(progress.toFirestoreMap())
            .await()
    }

    private fun progressCollection(userId: String): CollectionReference =
        firestore.collection(USERS_COLLECTION).document(userId).collection(PROGRESS_COLLECTION)

    private fun DocumentSnapshot.toProgress() =
        CategoryProgress(
            categoryId = id,
            completedLevels = getLong(FIELD_COMPLETED_LEVELS)?.toInt() ?: 0,
            solvedInCurrentLevel =
                (get(FIELD_SOLVED_IN_CURRENT_LEVEL) as? List<*>)?.filterIsInstance<String>()?.toSet().orEmpty(),
            updatedAt = getLong(FIELD_UPDATED_AT) ?: 0L,
        )

    private fun CategoryProgress.toFirestoreMap(): Map<String, Any> =
        mapOf(
            FIELD_COMPLETED_LEVELS to completedLevels,
            FIELD_SOLVED_IN_CURRENT_LEVEL to solvedInCurrentLevel.sorted(),
            FIELD_UPDATED_AT to updatedAt,
        )

    private companion object {
        const val USERS_COLLECTION = "users"
        const val PROGRESS_COLLECTION = "progress"
        const val FIELD_COMPLETED_LEVELS = "completedLevels"
        const val FIELD_SOLVED_IN_CURRENT_LEVEL = "solvedInCurrentLevel"
        const val FIELD_UPDATED_AT = "updatedAt"
    }
}
