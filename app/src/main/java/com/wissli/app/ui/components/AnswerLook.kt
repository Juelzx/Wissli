package com.wissli.app.ui.components

enum class AnswerLook {
    IDLE,
    CORRECT,
    WRONG,
    DIMMED,
}

// After answering, the right answer is always revealed; the explanation below gives it away anyway.
fun answerLook(
    index: Int,
    correctAnswerIndex: Int,
    selectedAnswerIndex: Int?,
): AnswerLook =
    when {
        selectedAnswerIndex == null -> AnswerLook.IDLE
        index == correctAnswerIndex -> AnswerLook.CORRECT
        index == selectedAnswerIndex -> AnswerLook.WRONG
        else -> AnswerLook.DIMMED
    }
