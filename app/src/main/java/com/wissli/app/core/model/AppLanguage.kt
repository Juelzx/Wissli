package com.wissli.app.core.model

/** Languages the app and its question package are available in; [tag] is the BCP 47 language tag. */
enum class AppLanguage(
    val tag: String,
) {
    GERMAN("de"),
    ENGLISH("en"),
    ;

    companion object {
        /** Everything that is not German falls back to English, the default resource language. */
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag == tag?.substringBefore('-')?.lowercase() } ?: ENGLISH
    }
}
