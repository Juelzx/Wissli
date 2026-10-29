package com.wissli.app.data.question

import android.util.Log
import com.wissli.app.core.model.Category
import com.wissli.app.core.model.Question
import com.wissli.app.data.local.ContentDao
import com.wissli.app.data.question.content.BundledContentSource
import com.wissli.app.data.question.content.QuestionContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * Questions come from a package stored in Room: playing costs no Firestore reads and works
 * offline. On first access the package bundled in the APK is stored if it is newer than what the
 * database holds.
 */
class RoomQuestionRepository(
    private val contentDao: ContentDao,
    private val bundledContentSource: BundledContentSource,
    private val currentEpochDay: () -> Long = { LocalDate.now().toEpochDay() },
) : QuestionRepository {
    private val contentMutex = Mutex()

    @Volatile
    private var contentChecked = false

    override fun observeCategories(): Flow<List<Category>> =
        flow {
            ensureContent()
            emitAll(contentDao.observeCategories().map { categories -> categories.map { it.toModel() } })
        }

    override suspend fun getLevelQuestions(
        categoryId: String,
        level: Int,
    ): List<Question> {
        ensureContent()
        return contentDao.getLevelQuestions(categoryId, level).map { it.toModel() }
    }

    // TODO: pick from questions outside the solo path once the catalog defines them.
    override fun observeDailyQuestion(): Flow<DailyQuestion?> =
        flow {
            ensureContent()
            val count = contentDao.countQuestions().toLong()
            val offset = if (count > 0) Math.floorMod(currentEpochDay(), count).toInt() else null
            val row = offset?.let { contentDao.getDailyQuestionAt(it) }
            emit(
                row?.let {
                    DailyQuestion(
                        questionId = it.id,
                        title = it.title,
                        categoryName = it.categoryName,
                        ageFrom = it.ageFrom,
                        ageTo = it.ageTo,
                    )
                },
            )
        }

    // TODO: also apply packages downloaded after a remote contentVersion check.
    private suspend fun ensureContent() {
        if (contentChecked) return
        contentMutex.withLock {
            if (contentChecked) return
            // A broken bundle must not block the app; whatever is stored stays usable.
            runCatching { applyIfNewer(bundledContentSource.load()) }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Log.e(TAG, "Could not apply the bundled question package", error)
                }
            contentChecked = true
        }
    }

    private suspend fun applyIfNewer(content: QuestionContent) {
        val storedVersion = contentDao.getContentVersion() ?: 0
        if (content.contentVersion <= storedVersion) return
        contentDao.replaceContent(
            categories = content.categories.map { it.toEntity() },
            questions = content.questions.map { it.toEntity() },
            contentVersion = content.contentVersion,
        )
    }

    private companion object {
        const val TAG = "RoomQuestionRepository"
    }
}
