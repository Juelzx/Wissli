package com.wissli.app.data.question

import com.wissli.app.data.local.CategoryEntity
import com.wissli.app.data.local.CategoryWithLevelCount
import com.wissli.app.data.local.ContentDao
import com.wissli.app.data.local.ContentInfoEntity
import com.wissli.app.data.local.DailyQuestionRow
import com.wissli.app.data.local.QuestionEntity
import com.wissli.app.data.question.content.BundledContentSource
import com.wissli.app.data.question.content.ContentCategory
import com.wissli.app.data.question.content.ContentQuestion
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
            dao.replaceContent(emptyList(), emptyList(), contentVersion = 2)

            repository(bundled = content(version = 2, "nature-0001")).getLevelQuestions("nature", 1)

            assertEquals(0, dao.countQuestions())
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
            val repository = RoomQuestionRepository(dao, source)

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

            val questions = RoomQuestionRepository(dao, broken).getLevelQuestions("nature", 1)

            assertEquals(listOf("nature-0001"), questions.map { it.id })
        }

    @Test
    fun `daily question rotates through all questions by day`() =
        runTest {
            dao.replaceContent(emptyList(), emptyList(), contentVersion = 1)
            val bundled = content(version = 2, "nature-0001", "nature-0002", "nature-0003")

            val day4 = repository(bundled, epochDay = 4).observeDailyQuestion().first()
            val day5 = repository(bundled, epochDay = 5).observeDailyQuestion().first()

            assertEquals("nature-0002", day4?.questionId)
            assertEquals("nature-0003", day5?.questionId)
            assertEquals("Natur", day5?.categoryName)
        }

    private fun repository(
        bundled: QuestionContent,
        epochDay: Long = 0,
    ) = RoomQuestionRepository(dao, CountingSource(bundled), currentEpochDay = { epochDay })

    private fun content(
        version: Int,
        vararg questionIds: String,
    ) = QuestionContent(
        contentVersion = version,
        categories = listOf(ContentCategory(id = "nature", name = "Natur", order = 0)),
        questions =
            questionIds.mapIndexed { index, id ->
                ContentQuestion(
                    id = id,
                    categoryId = "nature",
                    level = 1,
                    order = index + 1,
                    title = "Question $id",
                    answers = listOf("a", "b", "c", "d"),
                    correctAnswerIndex = 0,
                    explanation = "Because.",
                    ageFrom = 6,
                    ageTo = 12,
                    difficulty = null,
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

    private class FakeContentDao : ContentDao() {
        private val categories = MutableStateFlow(emptyList<CategoryEntity>())
        private val questions = MutableStateFlow(emptyList<QuestionEntity>())
        private var info: ContentInfoEntity? = null
        var replaceCount = 0

        override fun observeCategories(): Flow<List<CategoryWithLevelCount>> =
            categories.map { all ->
                all.sortedBy { it.sortOrder }.map { category ->
                    val levelCount =
                        questions.value.filter { it.categoryId == category.id }.maxOfOrNull { it.level } ?: 0
                    CategoryWithLevelCount(category.id, category.name, category.sortOrder, levelCount)
                }
            }

        override suspend fun getLevelQuestions(
            categoryId: String,
            level: Int,
        ) = questions.value.filter { it.categoryId == categoryId && it.level == level }.sortedBy { it.sortOrder }

        override suspend fun countQuestions() = questions.value.size

        override suspend fun getDailyQuestionAt(offset: Int) =
            questions.value.sortedBy { it.id }.getOrNull(offset)?.let { question ->
                val categoryName = categories.value.first { it.id == question.categoryId }.name
                DailyQuestionRow(question.id, question.title, categoryName, question.ageFrom, question.ageTo)
            }

        override suspend fun getContentVersion() = info?.contentVersion

        override suspend fun replaceContent(
            categories: List<CategoryEntity>,
            questions: List<QuestionEntity>,
            contentVersion: Int,
        ) {
            replaceCount++
            super.replaceContent(categories, questions, contentVersion)
        }

        override suspend fun deleteQuestions() {
            questions.value = emptyList()
        }

        override suspend fun deleteCategories() {
            categories.value = emptyList()
        }

        override suspend fun insertCategories(categories: List<CategoryEntity>) {
            this.categories.value += categories
        }

        override suspend fun insertQuestions(questions: List<QuestionEntity>) {
            this.questions.value += questions
        }

        override suspend fun upsertContentInfo(info: ContentInfoEntity) {
            this.info = info
        }
    }
}
