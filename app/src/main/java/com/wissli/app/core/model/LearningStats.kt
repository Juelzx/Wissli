package com.wissli.app.core.model

/** How often questions of one category (or of one day) were answered, and how many of those right. */
data class AnswerCount(
    val answered: Int = 0,
    val correct: Int = 0,
) {
    /** Share of correct answers, null while nothing was answered. */
    val accuracy: Float?
        get() = if (answered == 0) null else correct.toFloat() / answered
}

/** Every recorded answer of the user, grouped by category id and by local epoch day. */
data class AnswerHistory(
    val byCategory: Map<String, AnswerCount> = emptyMap(),
    val answersByDay: Map<Long, Int> = emptyMap(),
)

data class TopicAccuracy(
    val category: Category,
    val count: AnswerCount,
)

data class DayActivity(
    val epochDay: Long,
    val answered: Int,
)

/**
 * The learning overview on the dashboard. [solvedQuestions] comes from the saved progress, so it
 * also covers answers given before answers were recorded; everything else only counts recorded ones.
 */
data class LearningStats(
    val solvedQuestions: Int = 0,
    val total: AnswerCount = AnswerCount(),
    val streakDays: Int = 0,
    val lastDays: List<DayActivity> = emptyList(),
    val topics: List<TopicAccuracy> = emptyList(),
)

const val ACTIVITY_DAYS = 7

fun learningStats(
    categories: List<Category>,
    progress: Map<String, CategoryProgress>,
    history: AnswerHistory,
    today: Long,
): LearningStats =
    LearningStats(
        solvedQuestions =
            progress.values.sumOf { it.completedLevels * QUESTIONS_PER_LEVEL + it.solvedInCurrentLevel.size },
        total =
            AnswerCount(
                answered = history.byCategory.values.sumOf { it.answered },
                correct = history.byCategory.values.sumOf { it.correct },
            ),
        streakDays = streakDays(history.answersByDay.filterValues { it > 0 }.keys, today),
        lastDays =
            (today - ACTIVITY_DAYS + 1..today).map { day -> DayActivity(day, history.answersByDay[day] ?: 0) },
        topics =
            categories
                .mapNotNull { category -> history.byCategory[category.id]?.let { TopicAccuracy(category, it) } }
                .filter { it.count.answered > 0 }
                .sortedWith(compareByDescending<TopicAccuracy> { it.count.answered }.thenBy { it.category.order }),
    )

/**
 * Days in a row with at least one answer. A streak that ended yesterday still counts, so it does
 * not drop to zero in the morning before the first question of the day.
 */
fun streakDays(
    activeDays: Set<Long>,
    today: Long,
): Int {
    val start = if (today in activeDays) today else today - 1
    return generateSequence(start) { it - 1 }.takeWhile { it in activeDays }.count()
}
