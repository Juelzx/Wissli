package com.wissli.app.data.question

import android.util.Log
import com.wissli.app.core.model.AppLanguage
import com.wissli.app.core.model.Category
import com.wissli.app.core.model.Question
import com.wissli.app.data.local.ContentDao
import com.wissli.app.data.question.content.BundledContentSource
import com.wissli.app.data.question.content.QuestionContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Questions come from a package stored in Room: playing costs no Firestore reads and works
 * offline. On first access the package bundled in the APK is stored if it is newer than what the
 * database holds. Texts are read in [language], so switching the app language needs no reload.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RoomQuestionRepository(
    private val contentDao: ContentDao,
    private val bundledContentSource: BundledContentSource,
    private val language: Flow<AppLanguage>,
) : QuestionRepository {
    private val contentMutex = Mutex()

    @Volatile
    private var contentChecked = false

    override fun observeCategories(): Flow<List<Category>> =
        language
            .onStart { ensureContent() }
            .flatMapLatest { language -> contentDao.observeCategories(language.tag) }
            .map { categories -> categories.map { it.toModel() } }

    override suspend fun getLevelQuestions(
        categoryId: String,
        level: Int,
    ): List<Question> {
        ensureContent()
        return contentDao.getLevelQuestions(categoryId, level, language.first().tag).map { it.toModel() }
    }

    override suspend fun getQuestionIds(): List<String> {
        ensureContent()
        return contentDao.getQuestionIds()
    }

    override fun observeQuestion(questionId: String): Flow<Question?> =
        language
            .onStart { ensureContent() }
            .flatMapLatest { language -> contentDao.observeQuestion(questionId, language.tag) }
            .map { it?.toModel() }

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
        contentDao.replaceContent(content.toRows(), content.contentVersion)
    }

    private companion object {
        const val TAG = "RoomQuestionRepository"
    }
}
