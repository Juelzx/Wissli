package com.wissli.app.data.question

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// TODO: replace with a Firestore implementation (questions + categories).
class PlaceholderQuestionRepository : QuestionRepository {
    override fun observeDailyQuestion(): Flow<DailyQuestion?> =
        flowOf(
            DailyQuestion(
                questionId = "sky-blue",
                title = "Warum ist der Himmel blau?",
                categoryName = "Natur",
                ageFrom = 6,
                ageTo = 10,
            ),
        )
}
