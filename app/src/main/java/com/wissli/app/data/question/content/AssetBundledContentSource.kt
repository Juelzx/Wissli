package com.wissli.app.data.question.content

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AssetBundledContentSource(
    private val context: Context,
) : BundledContentSource {
    override suspend fun load(): QuestionContent =
        withContext(Dispatchers.IO) {
            val json =
                context.assets
                    .open(ASSET_PATH)
                    .bufferedReader()
                    .use { it.readText() }
            QuestionContentParser.parse(json)
        }

    private companion object {
        const val ASSET_PATH = "content/questions.json"
    }
}
