package com.wissli.app.data.progress

import com.wissli.app.core.model.CategoryProgress
import com.wissli.app.core.model.User
import com.wissli.app.data.auth.AuthRepository
import com.wissli.app.data.auth.LastSignedInAccount
import com.wissli.app.data.local.ProgressDao
import com.wissli.app.data.local.ProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineFirstProgressRepositoryTest {
    private val auth = FakeAuthRepository()
    private val dao = FakeProgressDao()
    private val remote = FakeRemoteDataSource()
    private var now = 100L

    @Test
    fun `stores a solved question on the device and uploads it`() =
        runTest {
            val repository = repository()
            runCurrent()

            repository.markSolved("nature", level = 1, questionId = "nature-0001", levelQuestionCount = 20)
            runCurrent()

            val progress = repository.observeProgress().first().getValue("nature")
            assertEquals(setOf("nature-0001"), progress.solvedInCurrentLevel)
            assertEquals(setOf("nature-0001"), remote.stored(ALICE, "nature")?.solvedInCurrentLevel)
            assertFalse(dao.row(ALICE, "nature").pendingSync)
        }

    @Test
    fun `keeps progress offline and uploads it on the next sign-in`() =
        runTest {
            remote.online = false
            val repository = repository()
            runCurrent()

            val result = repository.markSolved("nature", level = 1, questionId = "nature-0001", levelQuestionCount = 20)
            runCurrent()

            assertTrue(result.isSuccess)
            assertTrue(dao.row(ALICE, "nature").pendingSync)

            remote.online = true
            auth.userId.value = null
            runCurrent()
            auth.userId.value = ALICE
            runCurrent()

            assertEquals(setOf("nature-0001"), remote.stored(ALICE, "nature")?.solvedInCurrentLevel)
            assertFalse(dao.row(ALICE, "nature").pendingSync)
        }

    @Test
    fun `merges progress from another device on sign-in`() =
        runTest {
            remote.seed(ALICE, CategoryProgress("space", completedLevels = 2, updatedAt = 50L))
            val local = CategoryProgress("space", completedLevels = 1, updatedAt = 80L)
            dao.upsert(local.toEntity(ALICE, pendingSync = false))

            val repository = repository()
            runCurrent()

            val merged = repository.observeProgress().first().getValue("space")
            assertEquals(2, merged.completedLevels)
            assertFalse(dao.row(ALICE, "space").pendingSync)
        }

    @Test
    fun `uploads several offline levels one level per write`() =
        runTest {
            val offline = CategoryProgress("body", completedLevels = 3, updatedAt = 80L)
            dao.upsert(offline.toEntity(ALICE, pendingSync = true))

            repository()
            runCurrent()

            val uploadedLevels = remote.saves.filter { it.categoryId == "body" }.map { it.completedLevels }
            assertEquals(listOf(1, 2, 3), uploadedLevels)
            assertFalse(dao.row(ALICE, "body").pendingSync)
        }

    @Test
    fun `progress belongs to the signed-in account`() =
        runTest {
            val repository = repository()
            runCurrent()
            repository.markSolved("nature", level = 1, questionId = "nature-0001", levelQuestionCount = 20)
            runCurrent()

            auth.userId.value = BOB
            runCurrent()

            assertEquals(emptyMap<String, CategoryProgress>(), repository.observeProgress().first())
        }

    @Test
    fun `fails without a signed-in account`() =
        runTest {
            auth.userId.value = null
            val repository = repository()

            val result = repository.markSolved("nature", level = 1, questionId = "nature-0001", levelQuestionCount = 20)

            assertTrue(result.isFailure)
        }

    private fun TestScope.repository() =
        OfflineFirstProgressRepository(
            authRepository = auth,
            progressDao = dao,
            remoteDataSource = remote,
            syncScope = backgroundScope,
            clock = { now++ },
        )

    private companion object {
        const val ALICE = "alice"
        const val BOB = "bob"
    }
}

private class FakeAuthRepository : AuthRepository {
    val userId = MutableStateFlow<String?>("alice")

    override fun observeCurrentUser(): Flow<User?> = userId.map { id -> id?.let { User(it, it, 0L) } }

    override fun observeUserId(): Flow<String?> = userId

    override suspend fun getLastSignedInAccount(): LastSignedInAccount? = null

    override suspend fun signInWithGoogle(googleIdToken: String): Result<User> =
        Result.failure(UnsupportedOperationException())

    override suspend fun signOut() {
        userId.value = null
    }
}

private class FakeProgressDao : ProgressDao {
    private val rows = MutableStateFlow(emptyMap<Pair<String, String>, ProgressEntity>())

    fun row(
        userId: String,
        categoryId: String,
    ): ProgressEntity = rows.value.getValue(userId to categoryId)

    override fun observeAll(userId: String): Flow<List<ProgressEntity>> =
        rows.map { all -> all.values.filter { it.userId == userId } }

    override suspend fun getAll(userId: String): List<ProgressEntity> = rows.value.values.filter { it.userId == userId }

    override suspend fun get(
        userId: String,
        categoryId: String,
    ): ProgressEntity? = rows.value[userId to categoryId]

    override suspend fun upsert(entity: ProgressEntity) {
        rows.value += (entity.userId to entity.categoryId) to entity
    }

    override suspend fun markSynced(
        userId: String,
        categoryId: String,
        updatedAt: Long,
    ) {
        val row = rows.value[userId to categoryId]?.takeIf { it.updatedAt == updatedAt } ?: return
        rows.value += (userId to categoryId) to row.copy(pendingSync = false)
    }
}

private class FakeRemoteDataSource : ProgressRemoteDataSource {
    private val documents = mutableMapOf<Pair<String, String>, CategoryProgress>()
    val saves = mutableListOf<CategoryProgress>()
    var online = true

    fun seed(
        userId: String,
        progress: CategoryProgress,
    ) {
        documents[userId to progress.categoryId] = progress
    }

    fun stored(
        userId: String,
        categoryId: String,
    ): CategoryProgress? = documents[userId to categoryId]

    override suspend fun fetchAll(userId: String): List<CategoryProgress> {
        check(online) { "offline" }
        return documents.filterKeys { it.first == userId }.values.toList()
    }

    override suspend fun save(
        userId: String,
        progress: CategoryProgress,
    ) {
        check(online) { "offline" }
        val stored = documents[userId to progress.categoryId]?.completedLevels ?: 0
        // Mirrors firestore.rules: at most one more completed level per write, never fewer.
        check(progress.completedLevels in stored..stored + 1) { "permission denied" }
        saves += progress
        documents[userId to progress.categoryId] = progress
    }
}
