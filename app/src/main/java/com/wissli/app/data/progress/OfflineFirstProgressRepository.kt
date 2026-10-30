package com.wissli.app.data.progress

import android.util.Log
import com.wissli.app.core.model.CategoryProgress
import com.wissli.app.data.auth.AuthRepository
import com.wissli.app.data.local.ProgressDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Room is the source of truth, so playing works offline and the UI never waits for the network.
 * Every change is uploaded right away; whatever is still pending, or new on another device, is
 * merged when an account signs in or the app starts.
 */
class OfflineFirstProgressRepository(
    private val authRepository: AuthRepository,
    private val progressDao: ProgressDao,
    private val remoteDataSource: ProgressRemoteDataSource,
    private val syncScope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) : ProgressRepository {
    // Guards read-modify-write on the local rows.
    private val writeMutex = Mutex()

    // Keeps uploads in order, so an older state can never overwrite a newer one on the server.
    private val uploadMutex = Mutex()

    init {
        syncScope.launch {
            authRepository.observeUserId().collectLatest { userId -> if (userId != null) sync(userId) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeProgress(): Flow<Map<String, CategoryProgress>> =
        authRepository.observeUserId().flatMapLatest { userId ->
            if (userId == null) {
                flowOf(emptyMap())
            } else {
                progressDao.observeAll(userId).map { rows -> rows.associate { it.categoryId to it.toModel() } }
            }
        }

    override suspend fun markSolved(
        categoryId: String,
        level: Int,
        questionId: String,
        levelQuestionCount: Int,
    ): Result<CategoryProgress> =
        runCatching {
            val userId = checkNotNull(authRepository.observeUserId().first()) { "No signed-in user" }
            val (current, updated) =
                writeMutex.withLock {
                    val current = progressDao.get(userId, categoryId)?.toModel() ?: CategoryProgress(categoryId)
                    // Strictly increasing, so markSynced can tell two saves apart.
                    val now = maxOf(clock(), current.updatedAt + 1)
                    val updated = current.withSolved(level, questionId, levelQuestionCount, now)
                    if (updated != current) progressDao.upsert(updated.toEntity(userId, pendingSync = true))
                    current to updated
                }
            if (updated != current) syncScope.launch { uploadLatest(userId, categoryId) }
            updated
        }

    /** Uploads the newest local row instead of a captured copy, so the order of launches does not matter. */
    private suspend fun uploadLatest(
        userId: String,
        categoryId: String,
    ) = uploadMutex.withLock {
        val row = progressDao.get(userId, categoryId)?.takeIf { it.pendingSync } ?: return@withLock
        runCatching { remoteDataSource.save(userId, row.toModel()) }
            .onSuccess { progressDao.markSynced(userId, categoryId, row.updatedAt) }
            .onFailure { Log.w(TAG, "Upload of $categoryId failed, retrying on next sync", it) }
    }

    private suspend fun sync(userId: String) =
        uploadMutex.withLock {
            val remote =
                runCatching { remoteDataSource.fetchAll(userId) }
                    .onFailure { Log.w(TAG, "Progress sync skipped, server not reachable", it) }
                    .getOrNull()
                    ?.associateBy { it.categoryId }
                    ?: return@withLock
            val categoryIds = remote.keys + progressDao.getAll(userId).map { it.categoryId }
            categoryIds.forEach { categoryId -> syncCategory(userId, categoryId, remote[categoryId]) }
        }

    private suspend fun syncCategory(
        userId: String,
        categoryId: String,
        remote: CategoryProgress?,
    ) {
        val merged =
            writeMutex.withLock {
                val local = progressDao.get(userId, categoryId)?.toModel()
                val merged = listOfNotNull(local, remote).reduce(CategoryProgress::mergedWith)
                if (merged != local) progressDao.upsert(merged.toEntity(userId, pendingSync = merged != remote))
                merged
            }
        val uploaded =
            merged == remote ||
                runCatching { uploadInSteps(userId, remote?.completedLevels ?: 0, merged) }
                    .onFailure { Log.w(TAG, "Upload of $categoryId failed, retrying on next sync", it) }
                    .isSuccess
        if (uploaded) progressDao.markSynced(userId, categoryId, merged.updatedAt)
    }

    // The rules accept at most one more completed level per write, e.g. after several levels offline.
    private suspend fun uploadInSteps(
        userId: String,
        remoteCompletedLevels: Int,
        progress: CategoryProgress,
    ) {
        for (levels in remoteCompletedLevels + 1 until progress.completedLevels) {
            remoteDataSource.save(userId, progress.copy(completedLevels = levels, solvedInCurrentLevel = emptySet()))
        }
        remoteDataSource.save(userId, progress)
    }

    private companion object {
        const val TAG = "ProgressRepository"
    }
}
