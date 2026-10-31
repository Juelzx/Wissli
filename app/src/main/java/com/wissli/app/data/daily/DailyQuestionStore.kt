package com.wissli.app.data.daily

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** The pick of [epochDay] (local date); [answeredIndex] is null until it is answered. */
data class DailyQuestionRecord(
    val epochDay: Long,
    val questionId: String,
    val answeredIndex: Int? = null,
)

/** Keeps only the latest pick: it is today's question and, from tomorrow on, the one not to repeat. */
interface DailyQuestionStore {
    suspend fun load(): DailyQuestionRecord?

    /** The latest saved record; null until [load] or [save] ran. */
    val record: StateFlow<DailyQuestionRecord?>

    suspend fun save(record: DailyQuestionRecord)
}

class SharedPreferencesDailyQuestionStore(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DailyQuestionStore {
    private val appContext = context.applicationContext
    private val prefs by lazy { appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    private val _record = MutableStateFlow<DailyQuestionRecord?>(null)
    override val record: StateFlow<DailyQuestionRecord?> = _record.asStateFlow()

    override suspend fun load(): DailyQuestionRecord? {
        _record.value?.let { return it }
        val stored =
            withContext(ioDispatcher) {
                val questionId = prefs.getString(KEY_QUESTION_ID, null) ?: return@withContext null
                DailyQuestionRecord(
                    epochDay = prefs.getLong(KEY_EPOCH_DAY, 0L),
                    questionId = questionId,
                    answeredIndex = prefs.getInt(KEY_ANSWERED_INDEX, NOT_ANSWERED).takeIf { it != NOT_ANSWERED },
                )
            }
        _record.compareAndSet(null, stored)
        return _record.value
    }

    override suspend fun save(record: DailyQuestionRecord) {
        _record.value = record
        withContext(ioDispatcher) {
            prefs.edit {
                putLong(KEY_EPOCH_DAY, record.epochDay)
                putString(KEY_QUESTION_ID, record.questionId)
                putInt(KEY_ANSWERED_INDEX, record.answeredIndex ?: NOT_ANSWERED)
            }
        }
    }

    private companion object {
        const val PREFS_NAME = "daily_question"
        const val KEY_EPOCH_DAY = "epoch_day"
        const val KEY_QUESTION_ID = "question_id"
        const val KEY_ANSWERED_INDEX = "answered_index"
        const val NOT_ANSWERED = -1
    }
}
