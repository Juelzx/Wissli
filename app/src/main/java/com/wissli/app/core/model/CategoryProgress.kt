package com.wissli.app.core.model

/**
 * A user's progress in one category. Levels are played in order, so the completed level count plus
 * the solved questions of the current level describe the whole path; that keeps it to one small
 * document per category instead of one per answered question.
 */
data class CategoryProgress(
    val categoryId: String,
    val completedLevels: Int = 0,
    val solvedInCurrentLevel: Set<String> = emptySet(),
    val updatedAt: Long = 0L,
) {
    val currentLevel: Int
        get() = completedLevels + 1

    val rank: Rank
        get() = Rank.forCompletedLevels(completedLevels)

    val hasStarted: Boolean
        get() = completedLevels > 0 || solvedInCurrentLevel.isNotEmpty()

    /** `null` once the highest rank is reached. */
    val levelsToNextRank: Int?
        get() = rank.next?.let { it.minCompletedLevels - completedLevels }

    /** Share of the way from the current rank to the next one, 1 at the highest rank. */
    val nextRankProgress: Float
        get() {
            val next = rank.next ?: return 1f
            val span = next.minCompletedLevels - rank.minCompletedLevels
            return (completedLevels - rank.minCompletedLevels).toFloat() / span
        }

    /** Counts only for the current level; replaying a completed level never changes progress. */
    fun withSolved(
        level: Int,
        questionId: String,
        levelQuestionCount: Int,
        now: Long,
    ): CategoryProgress {
        if (level != currentLevel || questionId in solvedInCurrentLevel) return this
        val solved = solvedInCurrentLevel + questionId
        return if (solved.size >= levelQuestionCount) {
            copy(completedLevels = completedLevels + 1, solvedInCurrentLevel = emptySet(), updatedAt = now)
        } else {
            copy(solvedInCurrentLevel = solved, updatedAt = now)
        }
    }
}

data class CategoryOverview(
    val category: Category,
    val progress: CategoryProgress,
) {
    val isFinished: Boolean
        get() = progress.completedLevels >= category.levelCount

    /** Levels below the current one are completed, the current one is open, everything above is locked. */
    fun levelStatus(level: Int): LevelStatus =
        when {
            level < progress.currentLevel -> LevelStatus.COMPLETED
            level == progress.currentLevel -> LevelStatus.CURRENT
            else -> LevelStatus.LOCKED
        }
}

enum class LevelStatus {
    COMPLETED,
    CURRENT,
    LOCKED,
}

fun List<Category>.withProgress(progress: Map<String, CategoryProgress>): List<CategoryOverview> =
    map { category -> CategoryOverview(category, progress[category.id] ?: CategoryProgress(category.id)) }

/** The category played most recently, or the first one with open levels for a fresh start. */
fun List<CategoryOverview>.continueCandidate(): CategoryOverview? {
    val open = filterNot { it.isFinished }
    return open.filter { it.progress.hasStarted }.maxByOrNull { it.progress.updatedAt } ?: open.firstOrNull()
}
