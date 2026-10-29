package com.wissli.app.core.navigation

import androidx.annotation.StringRes
import com.wissli.app.R

enum class MainTab(
    @param:StringRes val labelRes: Int,
) {
    HOME(R.string.main_tab_home),
    QUESTIONS(R.string.main_tab_questions),
    CHALLENGES(R.string.main_tab_challenges),
    GROUPS(R.string.main_tab_groups),
}
