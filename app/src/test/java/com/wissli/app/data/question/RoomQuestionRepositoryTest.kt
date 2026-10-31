package com.wissli.app.data.question

import com.wissli.app.core.model.AppLanguage
import com.wissli.app.data.local.CategoryEntity
import com.wissli.app.data.local.CategoryNameEntity
import com.wissli.app.data.local.CategoryWithLevelCount
import com.wissli.app.data.local.ContentDao
import com.wissli.app.data.local.ContentInfoEntity
import com.wissli.app.data.local.ContentRows
import com.wissli.app.data.local.LocalizedQuestion
import com.wissli.app.data.local.QuestionEntity
import com.wissli.app.data.local.QuestionTextEntity
import com.wissli.app.data.question.content.BundledContentSource
import com.wissli.app.data.question.content.ContentCategory
import com.wissli.app.data.question.content.ContentQuestion
import com.wissli.app.data.question.content.ContentQuestionText
import com.wissli.app.data.question.content.QuestionContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomQuestionRepositoryTest {
    private val dao = FakeContentDao()
    private val language = MutableStateFlow(AppLanguage.GERMAN)

    @Test
    fun `stores the bundled package on first access`() =
        runTest {
            val repository = repository(bundled = content(version = 1, "nature-0001", "nature-0002"))

            val questions = repository.getLevelQuestions("nature", 1)

            assertEquals(listOf("nature-0001", "nature-0002"), questions.map { it.id })
            assertEquals(listOf("a", "b", "c", "d"), questions.first().answers)
            assertEquals(1, dao.getContentVersion())
        }

    @Test
    fun `keeps stored content when the bundle is not newer`() =
        runTest {
            dao.replaceContent(ContentRows(emptyList(), emptyList(), emptyList(), emptyList()), contentVersion = 2)

            repository(bundled = content(version = 2, "nature-0001")).getLevelQuestions("nature", 1)

            assertEquals(emptyList<String>(), dao.getQuestionIds())
            assertEquals(1, dao.replaceCount)
        }

    @Test
    fun `replaces stored content when the bundle is newer`() =
        runTest {
            repository(bundled = content(version = 1, "nature-0001")).getLevelQuestions("nature", 1)

            val questions =
                repository(
                    bundled = content(version = 2, "nature-0001", "nature-0002"),
                ).getLevelQuestions("nature", 1)

            assertEquals(2, questions.size)
            assertEquals(2, dao.getContentVersion())
        }

    @Test
    fun `checks the bundle only once per app start`() =
        runTest {
            val source = CountingSource(content(version = 1, "nature-0001"))
            val repository = RoomQuestionRepository(dao, source, language)

            repository.getLevelQuestions("nature", 1)
            repository.observeCategories().first()

            assertEquals(1, source.loads)
        }

    @Test
    fun `a broken bundle leaves stored content usable`() =
        runTest {
            repository(bundled = content(version = 1, "nature-0001")).getLevelQuestions("nature", 1)
            val broken =
                object : BundledContentSource {
                    override suspend fun load(): QuestionContent = error("broken package")
                }

            val questions = RoomQuestionRepository(dao, broken, language).getLevelQuestions("nature", 1)

            assertEquals(listOf("nature-0001"), questions.map { it.id })
        }

    @Test
    fun `texts follow the app language`() =
        runTest {
            val repository = repository(bundled = content(version = 1, "nature-0001"))
            assertEquals("Natur", categoryName(repository))
            assertEquals("Frage nature-0001", repository.getLevelQuestions("nature", 1).single().title)

            language.value = AppLanguage.ENGLISH

            assertEquals("Nature", categoryName(repository))
            assertEquals("Question nature-0001", repository.observeQuestion("nature-0001").first()?.title)
        }

    private fun repository(bundled: QuestionContent) = RoomQuestionRepository(dao, CountingSource(bundled), language)

    private suspend fun categoryName(repository: RoomQuestionRepository): String =
        repository
            .observeCategories()
            .first()
            .single()
            .name

    private fun content(
        version: Int,
        vararg questionIds: String,
    ) = QuestionContent(
        contentVersion = version,
        languages = listOf("de", "en"),
        categories =
            listOf(ContentCategory(id = "nature", names = mapOf("de" to "Natur", "en" to "Nature"), order = 0)),
        questions =
            questionIds.mapIndexed { index, id ->
                ContentQuestion(
                    id = id,
                    categoryId = "nature",
                    level = 1,
                    order = index + 1,
                    correctAnswerIndex = 0,
                    ageFrom = 6,
                    ageTo = 12,
                    difficulty = null,
                    texts =
                        mapOf(
                            "de" to ContentQuestionText("Frage $id", listOf("a", "b", "c", "d"), "Weil."),
                            "en" to ContentQuestionText("Question $id", listOf("a", "b", "c", "d"), "Because."),
                        ),
                )
            },
    )

    private class CountingSource(
        private val content: QuestionContent,
    ) : BundledContentSource {
        var loads = 0

        override suspend fun load(): QuestionContent {
            loads++
            return content
        }
    }

    /** Mirrors the joins of the real queries on in-memory rows. */
    private class FakeContentDao : ContentDao() {
        private val rows = MutableStateFlow(ContentRows(emptyList(), emptyList(), emptyList(), emptyList()))
        private var info: ContentInfoEntity? = null
        var replaceCount = 0

        override fun observeCategories(language: String): Flow<List<CategoryWithLevelCount>> =
            rows.map { all ->
                all.categories.sortedBy { it.sortOrder }.mapNotNull { category ->
                    val name =
                        all.categoryNames.firstOrNull { it.categoryId == category.id && it.language == language }
                            ?: return@mapNotNull null
                    val levelCount = all.questions.filter { it.categoryId == category.id }.maxOfOrNull { it.level } ?: 0
                    CategoryWithLevelCount(category.id, name.name, category.sortOrder, levelCount)
                }
            }

        override suspend fun getLevelQuestions(
            categoryId: String,
            level: Int,
            language: String,
        ) = localized(language).filter { it.categoryId == categoryId && it.level == level }.sortedBy { it.sortOrder }

        override fun observeQuestion(
            questionId: String,
            language: String,
        ): Flow<LocalizedQuestion?> = rows.map { localized(language).firstOrNull { it.id == questionId } }

        override suspend fun getQuestionIds() =
            rows.value.questions
                .map { it.id }
                .sorted()

        override suspend fun getContentVersion() = info?.contentVersion

        override suspend fun replaceContent(
            rows: ContentRows,
            contentVersion: Int,
        ) {
            replaceCount++
            this.rows.value = rows
            info = ContentInfoEntity(contentVersion = contentVersion)
        }

        override suspend fun deleteQuestionTexts() = Unit

        override suspend fun deleteQuestions() = Unit

        override suspend fun deleteCategoryNames() = Unit

        override suspend fun deleteCategories() = Unit

        override suspend fun insertCategories(categories: List<CategoryEntity>) = Unit

        override suspend fun insertCategoryNames(names: List<CategoryNameEntity>) = Unit

        override suspend fun insertQuestions(questions: List<QuestionEntity>) = Unit

        override suspend fun insertQuestionTexts(texts: List<QuestionTextEntity>) = Unit

        override suspend fun upsertContentInfo(info: ContentInfoEntity) = Unit

        private fun localized(language: String): List<LocalizedQuestion> =
            rows.value.questions.mapNotNull { question ->
                val text =
                    rows.value.questionTexts.firstOrNull { it.questionId == question.id && it.language == language }
                        ?: return@mapNotNull null
                LocalizedQuestion(
                    id = question.id,
                    categoryId = question.categoryId,
                    level = question.level,
                    sortOrder = question.sortOrder,
                    correctAnswerIndex = question.correctAnswerIndex,
                    difficulty = question.difficulty,
                    title = text.title,
                    answersJson = text.answersJson,
                    explanation = text.explanation,
                )
            }
    }
}
