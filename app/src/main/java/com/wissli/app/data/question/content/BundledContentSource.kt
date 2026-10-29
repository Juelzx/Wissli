package com.wissli.app.data.question.content

/** The question package shipped inside the APK; the starting point before any remote update. */
interface BundledContentSource {
    suspend fun load(): QuestionContent
}
