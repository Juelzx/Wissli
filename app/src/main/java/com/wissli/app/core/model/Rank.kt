package com.wissli.app.core.model

/**
 * Ranks depend on completed levels, not on a share of the category, so publishing new levels
 * never takes a rank away again.
 */
@Suppress("MagicNumber") // The thresholds are the definition of the ranks.
enum class Rank(
    val minCompletedLevels: Int,
) {
    NEWCOMER(0),
    EXPLORER(1),
    KNOWER(3),
    EXPERT(6),
    MASTER(10),
    ;

    val next: Rank?
        get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun forCompletedLevels(completedLevels: Int): Rank = entries.last { completedLevels >= it.minCompletedLevels }
    }
}
