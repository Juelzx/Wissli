package com.wissli.app.core.navigation

import androidx.annotation.StringRes
import com.wissli.app.R

// No challenges tab anymore: the planned group mode replaces 1:1 challenges and lives under Groups.
enum class MainTab(
    @param:StringRes val labelRes: Int,
) {
    HOME(R.string.main_tab_home),
    LEARN(R.string.main_tab_learn),
    GROUPS(R.string.main_tab_groups),
}
