package com.wissli.app.data.question.content

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AssetBundledContentSource(
    private val context: Context,
) : BundledContentSource {
    override suspend fun load(): QuestionContent =
        withContext(Dispatchers.IO) {
            QuestionContentParser.parse(
                readAsset(MANIFEST_PATH),
            ) { categoryId -> readAsset("$CATEGORIES_DIR/$categoryId.json") }
        }

    private fun readAsset(path: String): String =
        context.assets
            .open(path)
            .bufferedReader()
            .use { it.readText() }

    private companion object {
        const val MANIFEST_PATH = "content/manifest.json"
        const val CATEGORIES_DIR = "content/categories"
    }
}
